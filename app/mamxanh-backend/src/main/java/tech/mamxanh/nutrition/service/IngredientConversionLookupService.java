package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;

public interface IngredientConversionLookupService {
    Optional<BigDecimal> findActiveGramsPerUnit(long ingredientId, int unitId);

    /** Batch equivalent of the single lookup: conversion, ingredient and unit must all be active. */
    List<ConversionResponse> findActiveConversions(Collection<Long> ingredientIds);
}
