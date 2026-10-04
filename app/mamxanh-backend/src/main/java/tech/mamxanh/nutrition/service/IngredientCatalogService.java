package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.nutrition.dto.request.CatalogStatusUpdateRequest;
import tech.mamxanh.nutrition.dto.request.ConversionSaveRequest;
import tech.mamxanh.nutrition.dto.request.IngredientSaveRequest;
import tech.mamxanh.nutrition.dto.request.UnitSaveRequest;
import tech.mamxanh.nutrition.dto.response.ConversionResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.UnitResponse;
import tech.mamxanh.nutrition.entity.IngredientEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionEntity;
import tech.mamxanh.nutrition.entity.IngredientUnitConversionId;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.entity.CatalogStatus;
import tech.mamxanh.nutrition.entity.UnitEntity;
import tech.mamxanh.nutrition.repository.IngredientRepository;
import tech.mamxanh.nutrition.repository.IngredientUnitConversionRepository;
import tech.mamxanh.nutrition.repository.UnitRepository;

@Service
public class IngredientCatalogService implements IngredientCatalogLookupService, IngredientConversionLookupService {
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final IngredientUnitConversionRepository conversionRepository;

    public IngredientCatalogService(IngredientRepository ingredientRepository, UnitRepository unitRepository,
                                    IngredientUnitConversionRepository conversionRepository) {
        this.ingredientRepository = ingredientRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> listIngredients(String query) {
        List<IngredientEntity> ingredients = (query == null || query.isBlank())
                ? ingredientRepository.findAll()
                : ingredientRepository.findByNameContainingIgnoreCaseOrIngredientGroupContainingIgnoreCase(query.trim(), query.trim());
        return ingredients.stream().sorted(Comparator.comparing(IngredientEntity::getName)).map(this::toResponse).toList();
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

    @Transactional
    public IngredientResponse createIngredient(IngredientSaveRequest request) {
        String name = request.name().trim();
        if (ingredientRepository.existsByNameIgnoreCase(name)) throw conflict("INGREDIENT_NAME_EXISTS", "Tên nguyên liệu đã tồn tại.");
        return toResponse(ingredientRepository.save(new IngredientEntity(name, request.ingredientGroup().trim(), request.sourceName().trim(), request.sourceUrl(), request.referenceDate())));
    }

    @Transactional
    public IngredientResponse updateIngredient(long id, IngredientSaveRequest request) {
        IngredientEntity ingredient = requireIngredient(id);
        String name = request.name().trim();
        if (!ingredient.getName().equalsIgnoreCase(name) && ingredientRepository.existsByNameIgnoreCase(name)) throw conflict("INGREDIENT_NAME_EXISTS", "Tên nguyên liệu đã tồn tại.");
        if (ingredient.isNutritionSupported() && (request.sourceUrl() == null || request.sourceUrl().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SOURCE_REQUIRED_WHEN_NUTRITION_SUPPORTED",
                    "Cần cung cấp nguồn tham khảo cho nguyên liệu đang hỗ trợ dinh dưỡng.");
        }
        ingredient.update(name, request.ingredientGroup().trim(), request.sourceName().trim(), request.sourceUrl(), request.referenceDate());
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse setIngredientStatus(long id, CatalogStatusUpdateRequest request) {
        IngredientEntity ingredient = requireIngredient(id); ingredient.setActive(request.active()); return toResponse(ingredient);
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
        if (!unit.getCode().equalsIgnoreCase(code) && unitRepository.existsByCodeIgnoreCase(code)) throw conflict("UNIT_CODE_EXISTS", "Ký hiệu đơn vị đã tồn tại.");
        if (unit.getDimension() != request.dimension() && conversionRepository.existsByIdUnitId(id)) {
            throw conflict("UNIT_HAS_INGREDIENT_CONVERSIONS", "Không thể đổi thứ nguyên khi đơn vị đã có tỷ lệ quy đổi theo nguyên liệu.");
        }
        unit.update(code, request.name().trim(), request.dimension(), request.baseFactor()); return toResponse(unit);
    }

    @Transactional
    public UnitResponse setUnitStatus(int id, CatalogStatusUpdateRequest request) { UnitEntity unit = requireUnit(id); unit.setActive(request.active()); return toResponse(unit); }

    @Transactional(readOnly = true)
    public List<ConversionResponse> listConversions(Long ingredientId) {
        List<IngredientUnitConversionEntity> conversions = ingredientId == null ? conversionRepository.findAll() : conversionRepository.findByIdIngredientId(ingredientId);
        return conversions.stream().map(this::toResponse).toList();
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
        return toResponse(new IngredientUnitConversionEntity(id, request.gramsPerUnit(), request.approximate()));
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
        if (request.active()) requireConvertibleUnit(requireUnit(unitId));
        conversion.setActive(request.active());
        return toResponse(conversion);
    }

    public void rejectHardDelete(String resource) { throw conflict("HARD_DELETE_NOT_SUPPORTED", "Không hỗ trợ xóa vĩnh viễn " + resource + "; hãy chuyển sang trạng thái ngừng sử dụng."); }

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
    }
    private IngredientUnitConversionEntity requireConversion(long ingredientId, int unitId) { return conversionRepository.findById(new IngredientUnitConversionId(ingredientId, unitId)).orElseThrow(() -> notFound("CONVERSION_NOT_FOUND", "Không tìm thấy tỷ lệ quy đổi.")); }
    private IngredientResponse toResponse(IngredientEntity entity) { return new IngredientResponse(entity.getId(), entity.getName(), entity.getIngredientGroup(), entity.getSourceName(), entity.getSourceUrl(), entity.getReferenceDate(), entity.isNutritionSupported(), entity.getStatus() == CatalogStatus.ACTIVE); }
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
