package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemSpecificationValidator {

    private final CategoryRepository categoryRepository;

    public void validate(UUID categoryId, Map<String, Object> specifications) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        var fields = category.getSpecificationFields();
        if (fields == null || fields.isEmpty()) {
            return; // Preserve free-form specifications for legacy categories.
        }

        Map<String, Object> values = specifications == null ? Map.of() : specifications;
        Set<String> allowedKeys = new HashSet<>();
        for (CategorySpecificationField field : fields) {
            allowedKeys.add(field.key());
            Object value = values.get(field.key());
            if (field.required() && isMissing(value)) {
                throw invalid("Required specification '%s' is missing".formatted(field.key()));
            }
            if (!isMissing(value)) {
                validateValue(field, value);
            }
        }

        values.keySet().stream()
                .filter(key -> !allowedKeys.contains(key))
                .findFirst()
                .ifPresent(key -> { throw invalid("Unknown specification '%s'".formatted(key)); });
    }

    private void validateValue(CategorySpecificationField field, Object value) {
        boolean valid = switch (field.type()) {
            case TEXT -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case SELECT -> value instanceof String text && field.options().contains(text);
        };
        if (!valid) {
            throw invalid("Invalid value for specification '%s'".formatted(field.key()));
        }
    }

    private boolean isMissing(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }

    private InvalidOperationException invalid(String message) {
        return new InvalidOperationException(message);
    }
}
