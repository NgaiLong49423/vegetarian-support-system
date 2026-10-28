package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientNutritionRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.NutritionSupportUpdateRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.request.VolumeConversionSaveRequest;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientNutritionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientGroupResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientDetailResponse;
import tech.mamxanh.nutrition.dto.response.PublicIngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.dto.response.IngredientUnitOptionResponse;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.nutrition.config.NutritionCatalogProperties;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;

@Service
public class IngredientCatalogService implements IngredientCatalogLookupService, IngredientConversionLookupService {
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final IngredientUnitConversionRepository conversionRepository;
    private final NutritionCatalogProperties properties;

    public IngredientCatalogService(IngredientRepository ingredientRepository, UnitRepository unitRepository,
                                    IngredientUnitConversionRepository conversionRepository,
                                    NutritionCatalogProperties properties) {
        this.ingredientRepository = ingredientRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> listIngredients(String query) {
        List<IngredientEntity> ingredients = (query == null || query.isBlank())
                ? ingredientRepository.findAll()
                : ingredientRepository.findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase(query.trim(), query.trim());
        return ingredients.stream()
                .sorted(Comparator.comparing(IngredientEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<IngredientResponse> searchIngredients(String query, String ingredientGroup,
            Boolean active, Boolean nutritionSupported, int page, int size) {
        CatalogStatus status = active == null ? null : active ? CatalogStatus.ACTIVE : CatalogStatus.INACTIVE;
        String normalizedQuery = normalizeFilter(query);
        // Bind the entire pattern: SQL Server CONCAT casts can lose Vietnamese characters.
        String pattern = normalizedQuery == null ? null : "%" + normalizedQuery + "%";
        var results = ingredientRepository.search(pattern, normalizeFilter(ingredientGroup),
                status, nutritionSupported,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
        return PageResponse.of(results.getContent().stream().map(this::toResponse).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PublicIngredientGroupResponse> listPublicGroups() {
        return ingredientRepository.findActiveIngredientGroups().stream()
                .map(group -> new PublicIngredientGroupResponse(group.getGroupName(), group.getIngredientCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicIngredientResponse> searchPublicIngredients(String query, String ingredientGroup,
            int page, int size) {
        PageResponse<IngredientResponse> results = searchIngredients(query, ingredientGroup, true, null, page, size);
        List<PublicIngredientResponse> items = results.content().stream()
                .map(ingredient -> new PublicIngredientResponse(ingredient.id(), ingredient.name(),
                        ingredient.ingredientGroup(), ingredient.imageUrl()))
                .toList();
        return PageResponse.of(items, results.page(), results.size(), results.totalElements());
    }

    @Transactional(readOnly = true)
    public PublicIngredientDetailResponse getPublicIngredient(long id) {
        IngredientEntity ingredient = ingredientRepository.findByIdAndStatus(id, CatalogStatus.ACTIVE)
                .orElseThrow(() -> notFound("INGREDIENT_NOT_FOUND", "Không tìm thấy nguyên liệu."));
        IngredientNutritionResponse nutrition = new IngredientNutritionResponse(ingredient.getEnergyKcalPer100g(),
                ingredient.getProteinGramsPer100g(), ingredient.getCarbohydrateGramsPer100g(),
                ingredient.getTotalFatGramsPer100g(), ingredient.getFiberGramsPer100g(),
                ingredient.getCalciumMgPer100g(), ingredient.getIronMgPer100g(),
                ingredient.getVitaminB12MicrogramsPer100g(), ingredient.getZincMgPer100g());
        return new PublicIngredientDetailResponse(ingredient.getId(), ingredient.getName(),
                ingredient.getIngredientGroup(), ingredient.getImageUrl(), nutrition);
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredient(long id) {
        return toResponse(requireIngredient(id));
    }

    public List<String> getAllowedSources() { return properties.allowedSources(); }

    public List<String> getAllowedGroups() {
        java.util.TreeSet<String> groups = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        groups.addAll(properties.allowedGroups());
        List<String> existingGroups = ingredientRepository.findDistinctIngredientGroups();
        if (existingGroups != null) groups.addAll(existingGroups);
        return List.copyOf(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientResponse> findActiveIngredients(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return ingredientRepository.findByStatus(CatalogStatus.ACTIVE).stream()
                .filter(ingredient -> normalizedQuery.isEmpty()
                        || ingredient.getName().toLowerCase(java.util.Locale.ROOT).contains(normalizedQuery)
                        || ingredient.getIngredientGroup().toLowerCase(java.util.Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(IngredientEntity::getName))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientResponse> findActiveIngredientsByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ingredientRepository.findByIdInAndStatus(ids, CatalogStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(IngredientEntity::getName))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientResponse> findIngredientReferences(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ingredientRepository.findAllById(ids).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> findUnitReferences(java.util.Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return unitRepository.findAllById(ids).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversionResponse> findActiveConversions(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        var activeIds = ingredientRepository.findByIdInAndStatus(ids, CatalogStatus.ACTIVE).stream()
                .map(IngredientEntity::getId).toList();
        if (activeIds.isEmpty()) return List.of();
        var activeUnits = unitRepository.findByActiveTrue().stream().map(UnitEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        return conversionRepository.findByIdIngredientIdInAndActiveTrue(activeIds).stream()
                .filter(conversion -> activeUnits.contains(conversion.getId().getUnitId()))
                .map(this::toResponse).toList();
    }

    @Transactional
    public IngredientResponse createIngredient(IngredientSaveRequest request) {
        String name = request.name().trim();
        if (ingredientRepository.existsByNameIgnoreCaseAndStatus(name, CatalogStatus.ACTIVE)) {
            throw conflict("INGREDIENT_NAME_EXISTS", "Tên nguyên liệu đã tồn tại trong danh mục đang hoạt động.");
        }
        validateGroup(request.ingredientGroup());
        validateSource(request.sourceName());
        IngredientEntity ingredient = new IngredientEntity(name, request.ingredientGroup().trim(),
                request.sourceName().trim(), normalizeFilter(request.sourceUrl()), request.referenceDate());
        if (request.nutrition() != null) applyNutrition(ingredient, request.nutrition());
        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse updateIngredient(long id, IngredientSaveRequest request) {
        IngredientEntity ingredient = requireIngredient(id);
        String name = request.name().trim();
        if (ingredient.getStatus() == CatalogStatus.ACTIVE
                && !ingredient.getName().equalsIgnoreCase(name)
                && ingredientRepository.existsByNameIgnoreCaseAndStatusAndIdNot(name, CatalogStatus.ACTIVE, id)) {
            throw conflict("INGREDIENT_NAME_EXISTS", "Tên nguyên liệu đã tồn tại trong danh mục đang hoạt động.");
        }
        validateGroup(request.ingredientGroup());
        validateSource(request.sourceName());
        if (ingredient.isNutritionSupported() && request.nutrition() != null
                && !hasAnyNutritionValue(request.nutrition())) {
            throw noNutritionValuesAllowed();
        }
        ingredient.update(name, request.ingredientGroup().trim(), request.sourceName().trim(),
                normalizeFilter(request.sourceUrl()), request.referenceDate());
        if (request.nutrition() != null) applyNutrition(ingredient, request.nutrition());
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse updateNutrition(long id, IngredientNutritionRequest request) {
        IngredientEntity ingredient = requireIngredient(id);
        if (ingredient.isNutritionSupported() && !hasAnyNutritionValue(request)) {
            throw noNutritionValuesAllowed();
        }
        applyNutrition(ingredient, request);
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse setNutritionSupport(long id, NutritionSupportUpdateRequest request) {
        IngredientEntity ingredient = requireIngredient(id);
        if (request.enabled()) {
            if (missingNutritionFields(ingredient).size() == 9) {
                throw new ApiException(HttpStatus.CONFLICT, "NUTRITION_VALUES_EMPTY",
                        "Cần nhập ít nhất một chỉ tiêu dinh dưỡng trước khi bật hỗ trợ tính.");
            }
        }
        ingredient.setNutritionSupported(request.enabled());
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse setIngredientStatus(long id, CatalogStatusUpdateRequest request) {
        IngredientEntity ingredient = requireIngredient(id);
        if (request.active() && ingredient.getStatus() != CatalogStatus.ACTIVE
                && ingredientRepository.existsByNameIgnoreCaseAndStatusAndIdNot(
                        ingredient.getName(), CatalogStatus.ACTIVE, id)) {
            throw conflict("INGREDIENT_NAME_EXISTS", "Tên nguyên liệu đã tồn tại trong danh mục đang hoạt động.");
        }
        ingredient.setActive(request.active());
        return toResponse(ingredient);
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> listUnits() { return unitRepository.findAll().stream().sorted(Comparator.comparing(UnitEntity::getCode)).map(this::toResponse).toList(); }

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponse> findActiveUnits() {
        return unitRepository.findByActiveTrue().stream().sorted(Comparator.comparing(UnitEntity::getCode)).map(this::toResponse).toList();
    }

    @Transactional
    public UnitResponse createUnit(UnitSaveRequest request) {
        String code = request.code().trim();
        if (unitRepository.existsByCodeIgnoreCase(code)) throw conflict("UNIT_CODE_EXISTS", "Ký hiệu đơn vị đã tồn tại.");
        return toResponse(unitRepository.save(new UnitEntity(code, request.name().trim(), request.dimension(), request.baseFactor())));
    }

    @Transactional
    public UnitResponse updateUnit(int id, UnitSaveRequest request) {
        UnitEntity unit = requireUnit(id); String code = request.code().trim();
        if (isRecipeStandardCode(unit.getCode()) && (!unit.getCode().equals(code)
                || unit.getDimension() != request.dimension() || unit.getBaseFactor().compareTo(request.baseFactor()) != 0)) {
            throw conflict("STANDARD_UNIT_FIXED", "Không thể đổi mã, thứ nguyên hoặc hệ số của đơn vị công thức chuẩn.");
        }
        if (!unit.getCode().equalsIgnoreCase(code) && unitRepository.existsByCodeIgnoreCase(code)) throw conflict("UNIT_CODE_EXISTS", "Ký hiệu đơn vị đã tồn tại.");
        if (unit.getDimension() != request.dimension() && conversionRepository.existsByIdUnitId(id)) {
            throw conflict("UNIT_HAS_INGREDIENT_CONVERSIONS", "Không thể đổi thứ nguyên khi đơn vị đã có tỷ lệ quy đổi theo nguyên liệu.");
        }
        unit.update(code, request.name().trim(), request.dimension(), request.baseFactor()); return toResponse(unit);
    }

    @Transactional
    public void deleteUnit(int id) {
        requireUnit(id);
        rejectHardDelete("đơn vị");
    }

    @Transactional
    public UnitResponse setUnitStatus(int id, CatalogStatusUpdateRequest request) {
        UnitEntity unit = requireUnit(id);
        if (isRecipeStandardCode(unit.getCode()) && !request.active()) {
            throw conflict("STANDARD_UNIT_REQUIRED", "Không thể ngừng đơn vị thuộc bộ chuẩn công thức.");
        }
        unit.setActive(request.active()); return toResponse(unit);
    }

    @Transactional(readOnly = true)
    public List<ConversionResponse> listConversions(Long ingredientId) {
        List<IngredientUnitConversionEntity> conversions = ingredientId == null ? conversionRepository.findAll() : conversionRepository.findByIdIngredientId(ingredientId);
        return conversions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<IngredientUnitOptionResponse> listRecipeUnits(long ingredientId) {
        IngredientEntity ingredient = ingredientRepository.findByIdAndStatus(ingredientId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> notFound("INGREDIENT_NOT_FOUND", "Không tìm thấy nguyên liệu."));
        var conversions = conversionRepository.findByIdIngredientId(ingredient.getId()).stream().filter(IngredientUnitConversionEntity::isActive)
                .collect(java.util.stream.Collectors.toMap(c -> c.getId().getUnitId(), java.util.function.Function.identity()));
        return unitRepository.findByActiveTrue().stream()
                .filter(unit -> java.util.Set.of("mg", "g", "kg", "ml", "L", "tsp", "tbsp", "cup", "quả", "củ", "miếng").contains(unit.getCode()))
                .filter(unit -> unit.getDimension() == MeasurementDimension.MASS || conversions.containsKey(unit.getId()))
                .sorted(Comparator.comparingInt(unit -> recipeUnitOrder(unit.getCode())))
                .map(unit -> {
                    IngredientUnitConversionEntity conversion = conversions.get(unit.getId());
                    BigDecimal grams = unit.getDimension() == MeasurementDimension.MASS ? unit.getBaseFactor() : conversion.getGramsPerUnit();
                    return new IngredientUnitOptionResponse(unit.getId(), unit.getCode(), unit.getName(), unit.getDimension(), grams,
                            conversion != null && conversion.isApproximate());
                }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientUnitOptionResponse> findActiveIngredientUnits(long ingredientId) {
        return listRecipeUnits(ingredientId);
    }

    private int recipeUnitOrder(String code) {
        return List.of("mg", "g", "kg", "ml", "L", "tsp", "tbsp", "cup", "quả", "củ", "miếng").indexOf(code);
    }

    private boolean isRecipeStandardCode(String code) {
        return List.of("mg", "g", "kg", "ml", "L", "tsp", "tbsp", "cup", "quả", "củ", "miếng").contains(code);
    }

    @Transactional
    public ConversionResponse createConversion(long ingredientId, int unitId, ConversionSaveRequest request) {
        requireActiveIngredient(ingredientId);
        UnitEntity unit = requireActiveUnit(unitId);
        requireConvertibleUnit(unit);
        IngredientUnitConversionId id = new IngredientUnitConversionId(ingredientId, unitId);
        if (conversionRepository.existsById(id)) throw conflict("CONVERSION_EXISTS", "Cặp nguyên liệu và đơn vị đã có tỷ lệ quy đổi.");
        try {
            conversionRepository.insertNew(ingredientId, unitId, request.gramsPerUnit(), request.approximate());
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateKeyViolation(exception)) {
                throw conflict("CONVERSION_EXISTS", "Cặp nguyên liệu và đơn vị đã có tỷ lệ quy đổi.");
            }
            throw exception;
        }
        IngredientUnitConversionEntity created = new IngredientUnitConversionEntity(id, request.gramsPerUnit(), request.approximate());
        return toResponse(created);
    }

    @Transactional
    public ConversionResponse updateConversion(long ingredientId, int unitId, ConversionSaveRequest request) {
        IngredientUnitConversionEntity conversion = requireConversion(ingredientId, unitId);
        requireConvertibleUnit(requireUnit(unitId));
        conversion.update(request.gramsPerUnit(), request.approximate());
        return toResponse(conversion);
    }

    @Transactional
    public ConversionResponse setConversionStatus(long ingredientId, int unitId, CatalogStatusUpdateRequest request) {
        IngredientUnitConversionEntity conversion = requireConversion(ingredientId, unitId);
        requireConvertibleUnit(requireUnit(unitId));
        conversion.setActive(request.active());
        return toResponse(conversion);
    }

    @Transactional
    public List<ConversionResponse> saveVolumeConversions(long ingredientId, VolumeConversionSaveRequest request) {
        requireActiveIngredient(ingredientId);
        var units = unitRepository.findByActiveTrue().stream().filter(u -> List.of("ml", "L", "tsp", "tbsp", "cup").contains(u.getCode())).toList();
        if (units.size() != 5) throw conflict("STANDARD_VOLUME_UNITS_INCOMPLETE", "Thiếu một hoặc nhiều đơn vị thể tích chuẩn.");
        List<ConversionResponse> results = new ArrayList<>();
        for (UnitEntity unit : units) {
            BigDecimal grams = request.gramsPerMl().multiply(unit.getBaseFactor());
            var id = new IngredientUnitConversionId(ingredientId, unit.getId());
            var existing = conversionRepository.findById(id);
            if (existing.isPresent()) {
                var conversion = existing.get();
                conversion.update(grams, request.approximate());
                conversion.setActive(true);
                results.add(toResponse(conversion));
            } else {
                conversionRepository.insertNew(ingredientId, unit.getId(), grams, request.approximate());
                var created = new IngredientUnitConversionEntity(id, grams, request.approximate());
                results.add(toResponse(created));
            }
        }
        return results;
    }

    @Transactional
    public List<ConversionResponse> setVolumeConversionStatus(long ingredientId, CatalogStatusUpdateRequest request) {
        requireActiveIngredient(ingredientId);
        var ids = unitRepository.findByActiveTrue().stream().filter(u -> List.of("ml", "L", "tsp", "tbsp", "cup").contains(u.getCode()))
                .map(UnitEntity::getId).toList();
        var existing = conversionRepository.findByIdIngredientId(ingredientId).stream()
                .collect(java.util.stream.Collectors.toMap(c -> c.getId().getUnitId(), java.util.function.Function.identity()));
        List<ConversionResponse> result = new ArrayList<>();
        for (Integer id : ids) {
            if (existing.containsKey(id)) {
                var conversion = existing.get(id);
                conversion.setActive(request.active());
                result.add(toResponse(conversion));
            }
        }
        return result;
    }

    public void rejectHardDelete(String resource) { throw conflict("HARD_DELETE_NOT_SUPPORTED", "Không hỗ trợ xóa vĩnh viễn " + resource + ". Hãy chuyển sang trạng thái ngừng sử dụng."); }

    @Override @Transactional(readOnly = true)
    public Optional<BigDecimal> findActiveGramsPerUnit(long ingredientId, int unitId) {
        Optional<IngredientUnitConversionEntity> conversion = conversionRepository.findByIdAndActiveTrue(
                new IngredientUnitConversionId(ingredientId, unitId));
        if (conversion.isEmpty()) return Optional.empty();

        boolean ingredientActive = ingredientRepository.findById(ingredientId)
                .filter(ingredient -> ingredient.getStatus() == CatalogStatus.ACTIVE)
                .isPresent();
        boolean unitActive = unitRepository.findById(unitId)
                .filter(UnitEntity::isActive)
                .isPresent();
        if (!ingredientActive || !unitActive) return Optional.empty();

        return conversion.map(IngredientUnitConversionEntity::getGramsPerUnit);
    }

    private IngredientEntity requireIngredient(long id) { return ingredientRepository.findById(id).orElseThrow(() -> notFound("INGREDIENT_NOT_FOUND", "Không tìm thấy nguyên liệu.")); }
    private UnitEntity requireUnit(int id) { return unitRepository.findById(id).orElseThrow(() -> notFound("UNIT_NOT_FOUND", "Không tìm thấy đơn vị.")); }
    private void requireActiveIngredient(long id) { if (requireIngredient(id).getStatus() == CatalogStatus.INACTIVE) throw conflict("INGREDIENT_INACTIVE", "Nguyên liệu đã ngừng sử dụng."); }
    private UnitEntity requireActiveUnit(int id) {
        UnitEntity unit = requireUnit(id);
        if (!unit.isActive()) throw conflict("UNIT_INACTIVE", "Đơn vị đã ngừng sử dụng.");
        return unit;
    }
    private void requireConvertibleUnit(UnitEntity unit) {
        if (unit.getDimension() == MeasurementDimension.MASS) {
            throw conflict("MASS_CONVERSION_NOT_ALLOWED", "Không thể cấu hình quy đổi riêng cho đơn vị khối lượng.");
        }
        if (unit.getDimension() == MeasurementDimension.VOLUME
                && List.of("ml", "L", "tsp", "tbsp", "cup").contains(unit.getCode())) {
            throw conflict("VOLUME_GROUP_REQUIRED", "Cấu hình đơn vị thể tích qua hệ số gram trên 1 ml để đồng bộ cả nhóm.");
        }
    }
    private IngredientUnitConversionEntity requireConversion(long ingredientId, int unitId) { return conversionRepository.findById(new IngredientUnitConversionId(ingredientId, unitId)).orElseThrow(() -> notFound("CONVERSION_NOT_FOUND", "Không tìm thấy tỷ lệ quy đổi.")); }
    private IngredientResponse toResponse(IngredientEntity entity) {
        IngredientNutritionResponse nutrition = new IngredientNutritionResponse(entity.getEnergyKcalPer100g(),
                entity.getProteinGramsPer100g(), entity.getCarbohydrateGramsPer100g(),
                entity.getTotalFatGramsPer100g(), entity.getFiberGramsPer100g(), entity.getCalciumMgPer100g(),
                entity.getIronMgPer100g(), entity.getVitaminB12MicrogramsPer100g(), entity.getZincMgPer100g());
        return new IngredientResponse(entity.getId(), entity.getName(), entity.getIngredientGroup(),
                entity.getSourceName(), entity.getImageUrl(), entity.getSourceUrl(), entity.getReferenceDate(),
                entity.isNutritionSupported(), entity.getStatus() == CatalogStatus.ACTIVE, nutrition);
    }

    private void applyNutrition(IngredientEntity ingredient, IngredientNutritionRequest nutrition) {
        ingredient.updateNutrition(nutrition.energyKcalPer100g(), nutrition.proteinGramsPer100g(),
                nutrition.carbohydrateGramsPer100g(), nutrition.totalFatGramsPer100g(),
                nutrition.fiberGramsPer100g(), nutrition.calciumMgPer100g(), nutrition.ironMgPer100g(),
                nutrition.vitaminB12MicrogramsPer100g(), nutrition.zincMgPer100g());
    }

    private List<String> missingNutritionFields(IngredientEntity ingredient) {
        List<String> missing = new ArrayList<>();
        if (ingredient.getEnergyKcalPer100g() == null) missing.add("energyKcalPer100g");
        if (ingredient.getProteinGramsPer100g() == null) missing.add("proteinGramsPer100g");
        if (ingredient.getCarbohydrateGramsPer100g() == null) missing.add("carbohydrateGramsPer100g");
        if (ingredient.getTotalFatGramsPer100g() == null) missing.add("totalFatGramsPer100g");
        if (ingredient.getFiberGramsPer100g() == null) missing.add("fiberGramsPer100g");
        if (ingredient.getCalciumMgPer100g() == null) missing.add("calciumMgPer100g");
        if (ingredient.getIronMgPer100g() == null) missing.add("ironMgPer100g");
        if (ingredient.getVitaminB12MicrogramsPer100g() == null) missing.add("vitaminB12MicrogramsPer100g");
        if (ingredient.getZincMgPer100g() == null) missing.add("zincMgPer100g");
        return missing;
    }

    private void validateSource(String sourceName) {
        String candidate = sourceName == null ? "" : sourceName.trim();
        if (candidate.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NUTRITION_SOURCE_REQUIRED",
                    "Cần nhập tên nguồn hoặc ghi rõ dữ liệu do quản trị viên cung cấp.");
        }
    }

    private boolean hasAnyNutritionValue(IngredientNutritionRequest nutrition) {
        return nutrition.energyKcalPer100g() != null || nutrition.proteinGramsPer100g() != null
                || nutrition.carbohydrateGramsPer100g() != null || nutrition.totalFatGramsPer100g() != null
                || nutrition.fiberGramsPer100g() != null || nutrition.calciumMgPer100g() != null
                || nutrition.ironMgPer100g() != null || nutrition.vitaminB12MicrogramsPer100g() != null
                || nutrition.zincMgPer100g() != null;
    }

    private ApiException noNutritionValuesAllowed() {
        return new ApiException(HttpStatus.CONFLICT, "NUTRITION_VALUES_EMPTY",
                "Cần giữ lại ít nhất một chỉ tiêu có dữ liệu khi bật hỗ trợ tính dinh dưỡng.");
    }

    private void validateGroup(String ingredientGroup) {
        if (properties.allowedGroups().isEmpty()) return;
        String candidate = ingredientGroup == null ? "" : ingredientGroup.trim();
        boolean allowed = getAllowedGroups().stream().filter(java.util.Objects::nonNull)
                .map(String::trim).anyMatch(group -> group.equalsIgnoreCase(candidate));
        if (!allowed) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_INGREDIENT_GROUP",
                    "Nhóm nguyên liệu phải thuộc danh sách nhóm được hỗ trợ.");
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private UnitResponse toResponse(UnitEntity entity) { return new UnitResponse(entity.getId(), entity.getCode(), entity.getName(), entity.getDimension(), entity.getBaseFactor(), entity.isActive()); }
    private ConversionResponse toResponse(IngredientUnitConversionEntity entity) { return new ConversionResponse(entity.getId().getIngredientId(), entity.getId().getUnitId(), entity.getGramsPerUnit(), entity.isApproximate(), entity.isActive()); }
    private boolean isDuplicateKeyViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 2601 || sqlException.getErrorCode() == 2627)) {
                return true;
            }
        }
        return false;
    }

    private ApiException conflict(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }
    private ApiException notFound(String code, String message) { return new ApiException(HttpStatus.NOT_FOUND, code, message); }
}
