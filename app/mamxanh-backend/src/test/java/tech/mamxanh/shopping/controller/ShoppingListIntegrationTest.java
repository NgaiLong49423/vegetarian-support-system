package tech.mamxanh.shopping.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;

/** AC-53.2–53.5: real Flyway/SQL Server and full Spring security; list creation is SQL fixture only. */
class ShoppingListIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate sql;
    long owner, other, expert, admin, locked, list, foreignList;
    int roll;

    @BeforeEach void fixtures() {
        cleanup();
        owner = member("owner", "CUSTOMER", "ACTIVE");
        other = member("other", "CUSTOMER", "ACTIVE");
        expert = member("expert", "EXPERT", "ACTIVE");
        admin = member("admin", "ADMIN", "ACTIVE");
        locked = member("locked", "CUSTOMER", "LOCKED");
        list = list(owner, "Danh sách của tôi"); foreignList = list(other, "Danh sách riêng");
        list(expert, "Danh sách chuyên gia");
        roll = sql.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'cuộn'", Integer.class);
    }

    @AfterEach void cleanup() {
        sql.update("DELETE FROM [SHOPPING_LIST] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'fr53-%@test.local')");
        sql.update("DELETE FROM [USER] WHERE email LIKE 'fr53-%@test.local'");
    }

    @Test void manualItemEditTickReloadAndDeletePersistInSql() throws Exception {
        long id = add(list, owner, "Khăn giấy lau bếp");
        mockMvc.perform(get(path(list)).with(as(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Khăn giấy lau bếp"))
                .andExpect(jsonPath("$.items[0].quantity").value(2)).andExpect(jsonPath("$.items[0].bought").value(false));
        mockMvc.perform(patch(path(list)+"/items/"+id+"/bought").with(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bought\":true}"))
                .andExpect(status().isOk());
        var timestamp = sql.queryForObject("SELECT updated_at FROM [SHOPPING_LIST] WHERE shopping_list_id=?", java.sql.Timestamp.class, list);
        clock.advance(java.time.Duration.ofMinutes(1));
        mockMvc.perform(patch(path(list)+"/items/"+id+"/bought").with(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bought\":true}"))
                .andExpect(status().isOk());
        assertThat(sql.queryForObject("SELECT updated_at FROM [SHOPPING_LIST] WHERE shopping_list_id=?", java.sql.Timestamp.class, list)).isEqualTo(timestamp);
        mockMvc.perform(put(path(list)+"/items/"+id).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3.5,\"unitId\":"+roll+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bought").value(true));
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?", BigDecimal.class, id)).isEqualByComparingTo("3.5");
        mockMvc.perform(get(path(list)).with(as(owner))).andExpect(jsonPath("$.items[0].bought").value(true));
        mockMvc.perform(patch(path(list)+"/items/"+id+"/bought").with(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bought\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bought").value(false));
        mockMvc.perform(delete(path(list)+"/items/"+id).with(as(owner))).andExpect(status().isNoContent());
        assertThat(count(list)).isZero();
    }

    @Test void allForeignAccessAndItemListMismatchAreBlocked() throws Exception {
        long id = add(list, owner, "Mục riêng");
        long otherItem = add(foreignList, other, "Mục khác");
        mockMvc.perform(get(path(list)).with(as(other))).andExpect(status().isNotFound());
        mockMvc.perform(post(path(list)+"/items").with(as(other)).contentType(MediaType.APPLICATION_JSON).content(payload("X")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(path(list)+"/items/"+id).with(as(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3,\"unitId\":"+roll+"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(path(list)+"/items/"+id+"/bought").with(as(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bought\":true}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(path(list)+"/items/"+id).with(as(other))).andExpect(status().isNotFound());
        mockMvc.perform(delete(path(list)+"/items?bought=true").with(as(other))).andExpect(status().isNotFound());
        mockMvc.perform(delete(path(list)+"/items/"+otherItem).with(as(owner))).andExpect(status().isNotFound());
        assertThat(count(list)).isEqualTo(1); assertThat(count(foreignList)).isEqualTo(1);
    }

    @Test void cleanupPreservesUnboughtAndOtherLists() throws Exception {
        long bought = add(list, owner, "Đã mua"); add(list, owner, "Cần mua");
        add(foreignList, other, "Riêng");
        mockMvc.perform(patch(path(list)+"/items/"+bought+"/bought").with(as(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"bought\":true}"))
                .andExpect(status().isOk());
        for (int i=0; i<2; i++) mockMvc.perform(delete(path(list)+"/items?bought=true").with(as(owner))).andExpect(status().isNoContent());
        assertThat(count(list)).isEqualTo(1); assertThat(count(foreignList)).isEqualTo(1);
        mockMvc.perform(get(path(list)).with(as(owner))).andExpect(jsonPath("$.items[0].name").value("Cần mua"));
    }

    @Test void activeMembersOnlyAndOwnerScopedPaging() throws Exception {
        mockMvc.perform(get("/api/v1/shopping-lists")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/shopping-lists").with(as(locked))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/shopping-lists").with(as(admin))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/shopping-lists").with(as(expert)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/shopping-lists").with(as(owner)).param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(list))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/shopping-lists").with(as(owner)).param("size","51"))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectsUnsafeAndInvalidInputWithoutWriting() throws Exception {
        for (String body : new String[]{payload("<script>alert(1)</script>"), payload("   "),
                "{\"name\":\"X\",\"quantity\":0,\"unitId\":"+roll+"}",
                "{\"name\":\"X\",\"quantity\":2,\"unitId\":2147483647}"}) {
            mockMvc.perform(post(path(list)+"/items").with(as(owner)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(count(list)).isZero();
        mockMvc.perform(get(path(0)).with(as(owner))).andExpect(status().isBadRequest());
    }

    @Test void historicalInactiveUnitsRemainReadableButCannotBeSelectedForNewItems() throws Exception {
        // A test-owned unit is isolated from the shared catalog used by other integration tests.
        int unit = sql.queryForObject("INSERT INTO [UNIT] (code,name,dimension,base_factor) OUTPUT INSERTED.unit_id VALUES (N'fr53-test',N'Kiểm thử','COUNT',1)",Integer.class);
        try {
            long id = sql.queryForObject("INSERT INTO [SHOPPING_LIST_ITEM] (shopping_list_id,custom_ingredient_name,quantity,unit_id) OUTPUT INSERTED.shopping_list_item_id VALUES (?,N'Mục cũ',2,?)",Long.class,list,unit);
            sql.update("UPDATE [UNIT] SET is_active=0 WHERE unit_id=?",unit);
            mockMvc.perform(get(path(list)).with(as(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitCode").value("fr53-test"));
            mockMvc.perform(put(path(list)+"/items/"+id).with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":3,\"unitId\":"+unit+"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(post(path(list)+"/items").with(as(owner)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Mới\",\"quantity\":2,\"unitId\":"+unit+"}"))
                    .andExpect(status().isBadRequest());
        } finally { sql.update("DELETE FROM [SHOPPING_LIST_ITEM] WHERE unit_id=?",unit); sql.update("DELETE FROM [UNIT] WHERE unit_id=?",unit); }
    }

    long member(String name,String role,String status) {
        return sql.queryForObject("INSERT INTO [USER] (email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,?,?,?,1)",Long.class,
                "fr53-"+name+"@test.local","FR53 "+name,role,status);
    }
    long list(long userId,String name) { return sql.queryForObject("INSERT INTO [SHOPPING_LIST] (user_id,name) OUTPUT INSERTED.shopping_list_id VALUES (?,?)",Long.class,userId,name); }
    String path(long id) { return "/api/v1/shopping-lists/"+id; }
    RequestPostProcessor as(long id) { return user(Long.toString(id)).roles("CUSTOMER"); }
    String payload(String name) { return "{\"name\":\""+name+"\",\"quantity\":2,\"unitId\":"+roll+"}"; }
    long add(long listId,long userId,String name) throws Exception {
        mockMvc.perform(post(path(listId)+"/items").with(as(userId)).contentType(MediaType.APPLICATION_JSON).content(payload(name)))
                .andExpect(status().isCreated());
        return sql.queryForObject("SELECT MAX(shopping_list_item_id) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",Long.class,listId);
    }
    int count(long listId) { return sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",Integer.class,listId); }
}
