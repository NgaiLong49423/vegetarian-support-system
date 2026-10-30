package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.subscription.dto.response.AdminTransactionResponse;
import tech.mamxanh.subscription.repository.TransactionAuditRepository;

class TransactionAuditServiceTest {
    private final TransactionAuditRepository repository = mock(TransactionAuditRepository.class);
    private final TransactionAuditService service = new TransactionAuditService(repository,
            Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"),ZoneOffset.UTC));

    @Test void exportRefusesOversizeInsteadOfSilentlyTruncating() {
        when(repository.page(any(),eq(0L),eq(10001))).thenReturn(Collections.nCopies(10001,null));
        assertThatThrownBy(() -> service.export(null,null,null,null,null)).isInstanceOf(AppException.class).hasMessageContaining("10.000");
    }
    @Test void exportAllowsExactlyTenThousandAndNeutralizesSpreadsheetPayloads() {
        var row = new AdminTransactionResponse(1,"+123",7,"\t=1"," @SUM(1)",null,49000,"PENDING",Instant.EPOCH,null,null,null,null,null);
        assertThat(row.toString()).isEqualTo("AdminTransactionResponse[redacted]");
        when(repository.page(any(),eq(0L),eq(10001))).thenReturn(Collections.nCopies(10000,row));
        String csv=service.export("   ","",null,null,null);
        assertThat(csv).contains("\"'+123\"","\"'\t=1\"","\"' @SUM(1)\"");
        assertThat(csv.lines().count()).isEqualTo(10001);
    }
}
