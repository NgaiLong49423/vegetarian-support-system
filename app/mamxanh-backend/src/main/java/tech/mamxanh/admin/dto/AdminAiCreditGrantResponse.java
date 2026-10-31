package tech.mamxanh.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminAiCreditGrantResponse(long userId, BigDecimal credits,
        BigDecimal balance, LocalDateTime grantedAt) { }
