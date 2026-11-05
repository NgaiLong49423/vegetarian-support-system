package tech.mamxanh.subscription.dto.response;

import java.math.BigDecimal;

public record MyAiCreditResponse(BigDecimal balance, BigDecimal creditsPer1000Vnd,
        int minimumTopUpVnd, int maximumTopUpVnd) { }
