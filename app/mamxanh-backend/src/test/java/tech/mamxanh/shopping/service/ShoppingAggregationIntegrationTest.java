package tech.mamxanh.shopping.service;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.shopping.dto.request.ShoppingAggregationLine;
import tech.mamxanh.shopping.dto.response.ShoppingAggregationResult.Item;
import tech.mamxanh.nutrition.service.IngredientConversionLookupService;

/** Real catalog and conversion table; rollback fixtures. No FR-53 persistence or UI is claimed. */
@Transactional
class ShoppingAggregationIntegrationTest extends AbstractIntegrationTest {
    @Autowired ShoppingAggregationService service;
    @Autowired IngredientConversionLookupService conversions;
    @Autowired JdbcTemplate sql;
    long tofu,soy,avocado;
    int g,kg,ml,l,piece,block;

    @BeforeEach void fixtures() {
        tofu=ingredient("Đậu phụ","Đậu");soy=ingredient("Nước tương","Gia vị");avocado=ingredient("Bơ","Rau củ");
        g=unit("g");kg=unit("kg");ml=unit("ml");l=unit("L");piece=unit("quả");block=unit("bìa");
    }

    @Test void ac541And542UseRealStandardUnitCatalog() {
        var output=items(List.of(line(tofu,g,"600"),line(tofu,kg,"0.8"),line(soy,ml,"500"),line(soy,l,"0.75")));
        assertThat(output).hasSize(2);
        assertThat(output).filteredOn(i -> i.ingredientId()==tofu).singleElement()
                .satisfies(i -> {assertThat(i.quantity()).isEqualByComparingTo("1400");assertThat(i.unitCode()).isEqualTo("g");});
        assertThat(output).filteredOn(i -> i.ingredientId()==soy).singleElement()
                .satisfies(i -> {assertThat(i.quantity()).isEqualByComparingTo("1250");assertThat(i.unitCode()).isEqualTo("ml");});
    }

    @Test void ac543And545NeverFabricateMissingSqlConversions() {
        var output=items(List.of(line(soy,g,"300"),line(soy,ml,"200"),line(avocado,piece,"2"),line(avocado,g,"300")));
        assertThat(output).hasSize(4).allSatisfy(i -> assertThat(i.incompatibleUnits()).isTrue());
    }

    @Test void ac544UsesIngredientSpecificDatabaseRuleAndRejectsInactiveRules() {
        conversion(tofu,block,"150");
        assertThat(items(List.of(line(tofu,block,"2"),line(tofu,g,"300")))).singleElement()
                .satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("600"));
        sql.update("UPDATE [INGREDIENT_UNIT_CONVERSION] SET is_active=0 WHERE ingredient_id=? AND unit_id=?",tofu,block);
        assertThat(items(List.of(line(tofu,block,"2"),line(tofu,g,"300")))).hasSize(2);
    }

    @Test void neverUsesAnotherIngredientsConversionOrInactiveIngredientsRates() {
        conversion(tofu,block,"150");
        assertThat(items(List.of(line(avocado,block,"2"),line(avocado,g,"300")))).hasSize(2);
        sql.update("UPDATE [INGREDIENT] SET status='INACTIVE' WHERE ingredient_id=?",tofu);
        assertThat(conversions.findActiveConversions(List.of(tofu))).isEmpty();
        assertThat(items(List.of(line(tofu,block,"2"),line(tofu,g,"300")))).hasSize(2);
    }

    @Test void inactiveUnitRulesAreNotEligibleAndHistoricalNamesRemainAvailable() {
        int historic=sql.queryForObject("INSERT INTO [UNIT] (code,name,dimension,base_factor) OUTPUT INSERTED.unit_id VALUES (?,N'Đơn vị lịch sử','COUNT',1)",Integer.class,
                "h"+UUID.randomUUID().toString().substring(0,12));
        conversion(tofu,historic,"150");
        sql.update("UPDATE [UNIT] SET is_active=0 WHERE unit_id=?",historic);
        assertThat(conversions.findActiveConversions(List.of(tofu))).isEmpty();
        var result=items(List.of(line(tofu,historic,"2"),line(tofu,g,"300")));
        assertThat(result).hasSize(2).extracting(Item::name).allMatch(name -> name.startsWith("Đậu phụ"));
    }

    @Test void officialVolumeDensityAndManualNameGroupingWorkWithoutExternalProviders() {
        conversion(soy,ml,"1.25");
        assertThat(items(List.of(line(soy,g,"100"),line(soy,l,"1")))).singleElement()
                .satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("1350"));
        var manual=items(List.of(new ShoppingAggregationLine(null," Khăn giấy ",new BigDecimal("2"),piece),
                new ShoppingAggregationLine(null,"khăn giấy",new BigDecimal("3"),piece)));
        assertThat(manual).singleElement().satisfies(i -> {assertThat(i.ingredientId()).isNull();assertThat(i.quantity()).isEqualByComparingTo("5");});
    }

    long ingredient(String name,String group) {
        return sql.queryForObject("INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date) OUTPUT INSERTED.ingredient_id VALUES (?,?,N'Nguồn kiểm thử','2026-10-17')",Long.class,
                name+" "+UUID.randomUUID(),group);
    }
    int unit(String code) {return sql.queryForObject("SELECT unit_id FROM [UNIT] WHERE code=?",Integer.class,code);}
    void conversion(long ingredient,int unit,String grams) {
        sql.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id,unit_id,grams_per_unit) VALUES (?,?,?)",ingredient,unit,new BigDecimal(grams));
    }
    ShoppingAggregationLine line(long id,int unit,String quantity) {return new ShoppingAggregationLine(id,null,new BigDecimal(quantity),unit);}
    List<Item> items(List<ShoppingAggregationLine> lines) {return service.aggregate(lines).groups().stream().flatMap(group -> group.items().stream()).toList();}
}
