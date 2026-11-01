package tech.mamxanh.nutrition.service;

import tech.mamxanh.nutrition.dto.response.ConversionResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IngredientConversionLookupService {
    Optional<BigDecimal> findActiveGramsPerUnit(long ingredientId, int unitId);

    /** Batch equivalent of the single lookup: conversion, ingredient and unit must all be active. */
    List<ConversionResponse> findActiveConversions(Collection<Long> ingredientIds);
}
