package tech.mamxanh.shopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.shopping.dto.request.*;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse;
import tech.mamxanh.shopping.dto.response.ShoppingListCreationResponse.Warning;
import tech.mamxanh.shopping.entity.*;
import tech.mamxanh.shopping.repository.*;

/** AC-53.6, following the approved #90 personal-list/source semantics. */
@Service
public class RecipeShoppingListService {
    private final CurrentUserService users;
    private final RecipeService recipes;
    private final ShoppingAggregationService aggregation;
    private final ShoppingListRepository lists;
    private final ShoppingListItemRepository items;
    private final ShoppingRecipeContributionRepository contributions;
    private final ShoppingListService checklists;
    private final EntityManager em;
    private final Clock clock;

    public RecipeShoppingListService(CurrentUserService users, RecipeService recipes, ShoppingAggregationService aggregation,
            ShoppingListRepository lists, ShoppingListItemRepository items, ShoppingRecipeContributionRepository contributions,
            ShoppingListService checklists, EntityManager em, Clock clock) {
        this.users=users; this.recipes=recipes; this.aggregation=aggregation; this.lists=lists; this.items=items;
        this.contributions=contributions; this.checklists=checklists; this.em=em; this.clock=clock;
    }

    @Transactional
    public ShoppingListCreationResponse addRecipes(AddRecipeShoppingRequest request) {
        long owner=users.lockActiveMember().id(); // serialize first creation and repeated concurrent additions
        if (request==null || request.recipeIds()==null || request.recipeIds().isEmpty() || request.recipeIds().size()>50
                || request.recipeIds().stream().anyMatch(id -> id==null || id<1)) throw invalid("Chọn từ 1 đến 50 công thức công khai.");
        var ids=request.recipeIds().stream().distinct().toList();
        var sources=recipes.findShoppingSources(ids);
        if (!sources.keySet().containsAll(ids)) throw new AppException(ErrorCode.NOT_FOUND, "Một công thức đã chọn không còn công khai. Hãy tải lại danh sách.");
        var list=lists.findByUserIdAndKind(owner,"PERSONAL").orElse(null);
        if (list!=null) list=lists.lockOwned(list.getId(),owner).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var saved=list==null ? new ArrayList<ShoppingRecipeContributionEntity>()
                : new ArrayList<>(contributions.findByShoppingListIdOrderByIdAsc(list.getId()));
        var existingRecipes=new HashSet<Long>(); saved.forEach(c -> existingRecipes.add(c.getRecipeId()));
        var added=new ArrayList<ShoppingRecipeContributionEntity>();
        for (long id:ids) {
            if (existingRecipes.contains(id)) continue;
            var source=sources.get(id);
            if (source.ingredients().isEmpty()) throw invalid("Công thức không có nguyên liệu để thêm vào danh sách.");
            for (var line:source.ingredients()) {
                if (line.ingredientId()==null || line.unitId()==null || line.quantity()==null || line.quantity().signum()<=0)
                    throw invalid("Công thức có nguyên liệu hoặc định lượng không hợp lệ.");
                var row=new ShoppingRecipeContributionEntity();
                row.setRecipeId(id); row.setIngredientId(line.ingredientId()); row.setUnitId(line.unitId()); row.setQuantity(line.quantity());
                added.add(row);
            }
        }
        if (added.isEmpty()) return new ShoppingListCreationResponse(checklists.get(list.getId()),List.of(),List.of());
        var ingredientIds=new HashSet<Long>(); added.forEach(c -> ingredientIds.add(c.getIngredientId()));
        // Use saved, user-editable totals as the base; never rebuild them from changed recipes or old snapshots.
        var previous=(list==null ? List.<ShoppingListItemEntity>of() : items.findByShoppingListIdOrderByIdAsc(list.getId())).stream()
                .filter(row -> row.getIngredientId()!=null && ingredientIds.contains(row.getIngredientId())).toList();
        var lines=new ArrayList<ShoppingAggregationLine>();
        previous.forEach(row -> lines.add(new ShoppingAggregationLine(row.getIngredientId(),null,row.getQuantity(),row.getUnitId())));
        added.forEach(row -> lines.add(new ShoppingAggregationLine(row.getIngredientId(),null,row.getQuantity(),row.getUnitId())));
        var result=aggregation.aggregate(lines);
        if (result.warnings().stream().anyMatch(w -> !w.code().equals("CONFLICTING_CONVERSION")))
            throw invalid("Không thể tổng hợp an toàn toàn bộ nguyên liệu đã chọn.");
        var outputs=result.groups().stream().flatMap(g -> g.items().stream()).toList();
        outputs.forEach(output -> storedQuantity(output.quantity())); // validate the entire plan before any insert/update
        if (list==null) {
            list=new ShoppingListEntity(); list.setUserId(owner); list.setKind("PERSONAL"); list.setName("Danh sách cá nhân từ công thức");
            lists.saveAndFlush(list); em.refresh(list);
        }
        var warnings=new ArrayList<Warning>();
        result.warnings().forEach(w -> warnings.add(new Warning(w.code(),w.message(),null,null)));
        var retained=new HashSet<Long>();
        var incompatible=new ArrayList<Long>();
        for (var output:outputs) {
            var quantity=storedQuantity(output.quantity());
            if (quantity.compareTo(output.quantity())!=0) warnings.add(new Warning("QUANTITY_ROUNDED","Lượng được làm tròn đến hai chữ số thập phân khi lưu.",null,null));
            var old=output.sourceIndices().stream().filter(i -> i<previous.size()).map(previous::get).toList();
            boolean hasNew=output.sourceIndices().stream().anyMatch(i -> i>=previous.size());
            var target=old.isEmpty() ? new ShoppingListItemEntity() : old.getFirst();
            target.setShoppingListId(list.getId()); target.setIngredientId(output.ingredientId()); target.setUnitId(output.unitId()); target.setQuantity(quantity);
            target.setBought(!hasNew && old.stream().allMatch(ShoppingListItemEntity::isBought));
            items.save(target); retained.add(target.getId()); // IDENTITY supplies new IDs; flush once with contributions
            if (output.incompatibleUnits()) incompatible.add(target.getId());
            var oldIds=new HashSet<Long>(); old.forEach(row -> oldIds.add(row.getId()));
            saved.stream().filter(c -> oldIds.contains(c.getShoppingListItemId())).forEach(c -> c.setShoppingListItemId(target.getId()));
            for (int index:output.sourceIndices()) if (index>=previous.size()) added.get(index-previous.size()).setShoppingListItemId(target.getId());
        }
        long listId=list.getId(); added.forEach(row -> row.setShoppingListId(listId));
        contributions.saveAllAndFlush(saved); contributions.saveAllAndFlush(added);
        items.deleteAll(previous.stream().filter(row -> !retained.contains(row.getId())).toList());
        list.setUpdatedAt(LocalDateTime.now(clock)); em.flush();
        return new ShoppingListCreationResponse(checklists.get(listId),warnings,incompatible);
    }

    private BigDecimal storedQuantity(BigDecimal value) {
        var quantity=value.setScale(2,RoundingMode.HALF_UP);
        if (quantity.signum()<=0 || quantity.compareTo(new BigDecimal("99999999.99"))>0)
            throw invalid("Định lượng sau tổng hợp vượt giới hạn lưu trữ hoặc quá nhỏ.");
        return quantity;
    }

    private AppException invalid(String message) { return new AppException(ErrorCode.VALIDATION_FAILED,message); }
}
