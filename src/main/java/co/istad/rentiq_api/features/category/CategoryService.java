package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.cateogryDto.CategoryRequest;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(UUID id);

    List<CategoryResponse> getChildCategories(UUID parentId);

    List<Map<String, Object>> getItemsInCategory(UUID categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    void deleteCategory(UUID id);

    CategoryResponse updateCommissionRate(UUID id, BigDecimal commissionRate);

    CategoryResponse updateStatus(UUID id, Boolean active);
}
