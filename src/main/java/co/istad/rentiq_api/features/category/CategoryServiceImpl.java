package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryRequest;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryResponse;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import co.istad.rentiq_api.common.exception.InvalidOperationException;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final AdminAuditService adminAuditService;

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        return categoryMapper.toResponse(findCategoryOrThrow(id));
    }

    @Override
    public List<CategoryResponse> getChildCategories(UUID parentId) {
        return categoryRepository.findAll().stream()
                .filter(category -> parentId.equals(category.getParentId()))
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<Map<String, Object>> getItemsInCategory(UUID categoryId) {
        return List.of();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        validateSpecificationFields(request);
        if (categoryRepository.findAll().stream().anyMatch(category -> category.getName().equalsIgnoreCase(request.name()))) {
            throw new DuplicateCategoryException(request.name());
        }

        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);

        adminAuditService.record(
                AdminAuditAction.CATEGORY_CREATED,
                AdminAuditTargetType.CATEGORY,
                saved.getId().toString(),
                null,
                categorySnapshot(saved),
                null);

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        validateSpecificationFields(request);
        Category category = findCategoryOrThrow(id);
        Map<String, Object> before = categorySnapshot(category);

        categoryMapper.updateEntityFromRequest(request, category);
        Category saved = categoryRepository.save(category);

        adminAuditService.record(
                AdminAuditAction.CATEGORY_UPDATED,
                AdminAuditTargetType.CATEGORY,
                id.toString(),
                before,
                categorySnapshot(saved),
                null);

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = findCategoryOrThrow(id);
        Map<String, Object> before = categorySnapshot(category);

        categoryRepository.delete(category);

        adminAuditService.record(
                AdminAuditAction.CATEGORY_DELETED,
                AdminAuditTargetType.CATEGORY,
                id.toString(),
                before,
                null,
                null);
    }

    @Override
    @Transactional
    public CategoryResponse updateCommissionRate(UUID id, BigDecimal commissionRate) {
        Category category = findCategoryOrThrow(id);
        BigDecimal previousRate = category.getCommissionRate();

        category.setCommissionRate(commissionRate);
        Category saved = categoryRepository.save(category);

        adminAuditService.record(
                AdminAuditAction.COMMISSION_RATE_UPDATED,
                AdminAuditTargetType.CATEGORY,
                id.toString(),
                Map.of("commissionRate", previousRate),
                Map.of("commissionRate", commissionRate),
                null);

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateStatus(UUID id, Boolean active) {
        Category category = findCategoryOrThrow(id);
        Boolean previousActive = category.getActive();

        category.setActive(active);
        Category saved = categoryRepository.save(category);

        adminAuditService.record(
                AdminAuditAction.CATEGORY_STATUS_CHANGED,
                AdminAuditTargetType.CATEGORY,
                id.toString(),
                Map.of("active", previousActive),
                Map.of("active", active),
                null);

        return categoryMapper.toResponse(saved);
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private Map<String, Object> categorySnapshot(Category category) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", category.getName());
        snapshot.put("parentId", category.getParentId());
        snapshot.put("commissionRate", category.getCommissionRate());
        snapshot.put("active", category.getActive());
        snapshot.put("specificationFields", category.getSpecificationFields());
        return snapshot;
    }

    private void validateSpecificationFields(CategoryRequest request) {
        List<co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField> fields =
                request.specificationFields() == null ? List.of() : request.specificationFields();
        long distinctKeys = fields.stream()
                .map(field -> field.key().toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .count();
        if (distinctKeys != fields.size()) {
            throw new InvalidOperationException("Category specification field keys must be unique");
        }
    }

}
