package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.cateogryDto.CategoryRequest;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryResponse;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Integer id);

    List<CategoryResponse> getChildCategories(Integer parentId);

    List<Map<String, Object>> getItemsInCategory(Integer categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Integer id, CategoryRequest request);

    void deleteCategory(Integer id);

    CategoryResponse updateCommissionRate(Integer id, Double commissionRate);

    CategoryResponse updateStatus(Integer id, Boolean active);
}