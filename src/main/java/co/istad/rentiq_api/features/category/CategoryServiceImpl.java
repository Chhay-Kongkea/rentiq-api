package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Integer id) {
        return toResponse(findCategoryOrThrow(id));
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

        Category category = new Category();
        category.setParentId(request.parentId());
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setCommissionRate(request.commissionRate());
        category.setIconUrl(request.iconUrl());
        category.setActive(request.active());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        category.setParentId(request.parentId());
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setCommissionRate(request.commissionRate());
        category.setIconUrl(request.iconUrl());
        category.setActive(request.active());

        return toResponse(categoryRepository.save(category));
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
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateStatus(Integer id, Boolean active) {
        Category category = findCategoryOrThrow(id);
        category.setActive(active);
        return toResponse(categoryRepository.save(category));
    }

    private Category findCategoryOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getCommissionRate(),
                category.getIconUrl(),
                category.getActive()
        );
    }
}