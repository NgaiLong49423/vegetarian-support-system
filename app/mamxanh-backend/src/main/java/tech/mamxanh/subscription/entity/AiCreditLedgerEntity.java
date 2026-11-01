package tech.mamxanh.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AI_CREDIT_LEDGER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCreditLedgerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id") private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "transaction_type", nullable = false) private String type;
    @Column(name = "reference_id", nullable = false) private String referenceId;
    @Column(name = "credit_delta", nullable = false, precision = 38, scale = 9) private BigDecimal delta;
    @Column(name = "balance_after", nullable = false, precision = 38, scale = 9) private BigDecimal balanceAfter;
    @Column(name = "overage_tokens") private Long overageTokens;
    @Column(name = "credits_per_1000_tokens") private Long rate;
    @Column(name = "system_absorbed_tokens") private Long absorbedTokens;
    @Column(name = "system_absorbed_credits", precision = 38, scale = 9) private BigDecimal absorbedCredits;
    @Column(name = "granted_by_user_id") private Long grantedBy;
    @Column(name = "reason", length = 500) private String reason;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
