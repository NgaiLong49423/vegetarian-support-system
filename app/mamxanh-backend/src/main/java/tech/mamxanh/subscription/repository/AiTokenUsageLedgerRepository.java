package tech.mamxanh.subscription.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.mamxanh.subscription.entity.AiTokenUsageLedgerEntity;

public interface AiTokenUsageLedgerRepository extends JpaRepository<AiTokenUsageLedgerEntity, String> {
    @Query("select coalesce(sum(row.tokenCount), 0) from AiTokenUsageLedgerEntity row "
            + "where row.userId = :userId and row.periodStart = :periodStart and row.periodEnd = :periodEnd")
    long sumMemberTokens(@Param("userId") long userId, @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd);

    @Query("select coalesce(sum(row.tokenCount), 0) from AiTokenUsageLedgerEntity row "
            + "where row.anonymousSessionId = :sessionId")
    long sumGuestTokens(@Param("sessionId") String sessionId);

    @Query("select coalesce(sum(row.tokenCount), 0) from AiTokenUsageLedgerEntity row "
            + "where row.userId = :userId and row.periodStart = :periodStart and row.periodEnd = :periodEnd "
            + "and row.tokenCount is not null")
    long sumMemberTokensIncludingExpired(@Param("userId") long userId,
            @Param("periodStart") LocalDateTime periodStart, @Param("periodEnd") LocalDateTime periodEnd);

    @Modifying
    @Query("delete from AiTokenUsageLedgerEntity row where row.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
