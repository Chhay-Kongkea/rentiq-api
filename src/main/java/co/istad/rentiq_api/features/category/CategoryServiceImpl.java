package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.cateogryDto.CategoryRequest;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryResponse;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Integer id) {
        return categoryMapper.toResponse(findCategoryOrThrow(id));
    }

    @Override
    public List<CategoryResponse> getChildCategories(Integer parentId) {
        return categoryRepository.findAll().stream()
                .filter(category -> parentId.equals(category.getParentId()))
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<Map<String, Object>> getItemsInCategory(Integer categoryId) {
        return List.of();
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findAll().stream().anyMatch(category -> category.getName().equalsIgnoreCase(request.name()))) {
            throw new DuplicateCategoryException(request.name());
        }

        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        categoryMapper.updateEntityFromRequest(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Integer id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse updateCommissionRate(Integer id, Double commissionRate) {
        Category category = findCategoryOrThrow(id);
        category.setCommissionRate(commissionRate);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateStatus(Integer id, Boolean active) {
        Category category = findCategoryOrThrow(id);
        category.setActive(active);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    private Category findCategoryOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

}