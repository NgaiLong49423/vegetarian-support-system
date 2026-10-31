package tech.mamxanh.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import tech.mamxanh.admin.dto.AdminUserAuditResponse;
import tech.mamxanh.admin.dto.AdminUserResponse;
import tech.mamxanh.admin.dto.AdminUserSummaryResponse;

@ExtendWith(MockitoExtension.class)
class AdminUserQueryRepositoryTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 20, 8, 0);
    @Mock private JdbcTemplate jdbc;
    @Mock private ResultSet rs;
    private AdminUserQueryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AdminUserQueryRepository(jdbc);
    }

    @Test
    void buildsBoundedPageWithEscapedSearchAndEveryFilter() throws Exception {
        stubUserRow();
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<AdminUserResponse>>any(), any(Object[].class)))
                .thenAnswer(call -> List.of(call.<RowMapper<AdminUserResponse>>getArgument(1).mapRow(rs, 0)));

        List<AdminUserResponse> result = repository.page("  an%_[  ", "CUSTOMER", "ACTIVE", 2, 20);

        assertThat(result).containsExactly(new AdminUserResponse(42L, "Nguyễn An", "an@example.test",
                "CUSTOMER", "ACTIVE", CREATED, 3));
        verify(jdbc).query(anyString(), ArgumentMatchers.<RowMapper<AdminUserResponse>>any(),
                eq(new Object[] { "CUSTOMER", "ACTIVE", "%an[%][_][[]%", "%an[%][_][[]%", "%an[%][_][[]%", 40L, 20 }));
    }

    @Test
    void countsWithoutOptionalFiltersAndReturnsGlobalSummary() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(8L);
        when(jdbc.queryForObject(anyString(), ArgumentMatchers.<RowMapper<AdminUserSummaryResponse>>any()))
                .thenAnswer(call -> call.<RowMapper<AdminUserSummaryResponse>>getArgument(1).mapRow(summaryResultSet(), 0));

        assertThat(repository.count(null, null, null)).isEqualTo(8L);
        assertThat(repository.summary()).isEqualTo(new AdminUserSummaryResponse(8, 6, 2, 1));
    }

    @Test
    void findsUserAndReturnsEmptyWhenNoRowExists() throws Exception {
        stubUserRow();
        when(jdbc.query(anyString(), ArgumentMatchers.<ResultSetExtractor<Optional<AdminUserResponse>>>any(), eq(42L)))
                .thenAnswer(call -> call.<ResultSetExtractor<Optional<AdminUserResponse>>>getArgument(1).extractData(rs));
        when(rs.next()).thenReturn(true, false);

        assertThat(repository.findById(42L)).contains(new AdminUserResponse(42L, "Nguyễn An", "an@example.test",
                "CUSTOMER", "ACTIVE", CREATED, 3));
        assertThat(repository.findById(42L)).isEmpty();
    }

    @Test
    void mapsPagedHistoryAndCountsCurrentAndReportedLocks() throws Exception {
        when(rs.getString("event_id")).thenReturn("A-2");
        when(rs.getString("action")).thenReturn("ROLE_CHANGED");
        when(rs.getString("old_value")).thenReturn("CUSTOMER");
        when(rs.getString("new_value")).thenReturn("EXPERT");
        when(rs.getString("reason")).thenReturn("Đủ điều kiện chia sẻ công thức");
        when(rs.getObject("administrator_id", Long.class)).thenReturn(7L);
        when(rs.getString("administrator_name")).thenReturn("Admin");
        when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.valueOf(CREATED));
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<AdminUserAuditResponse>>any(), any(Object[].class)))
                .thenAnswer(call -> List.of(call.<RowMapper<AdminUserAuditResponse>>getArgument(1).mapRow(rs, 0)));
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(4L);

        assertThat(repository.history(42L, 1, 20)).containsExactly(new AdminUserAuditResponse("A-2", "ROLE_CHANGED",
                "CUSTOMER", "EXPERT", "Đủ điều kiện chia sẻ công thức", 7L, "Admin", CREATED, null));
        assertThat(repository.historyCount(42L)).isEqualTo(4L);
    }

    private ResultSet summaryResultSet() throws Exception {
        ResultSet summary = org.mockito.Mockito.mock(ResultSet.class);
        when(summary.getLong("total_users")).thenReturn(8L);
        when(summary.getLong("active_users")).thenReturn(6L);
        when(summary.getLong("expert_users")).thenReturn(2L);
        when(summary.getLong("locked_users")).thenReturn(1L);
        return summary;
    }

    private void stubUserRow() throws Exception {
        when(rs.getLong("user_id")).thenReturn(42L);
        when(rs.getLong("recipe_count")).thenReturn(3L);
        when(rs.getString("display_name")).thenReturn("Nguyễn An");
        when(rs.getString("email")).thenReturn("an@example.test");
        when(rs.getString("role")).thenReturn("CUSTOMER");
        when(rs.getString("account_status")).thenReturn("ACTIVE");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(CREATED));
    }
}
