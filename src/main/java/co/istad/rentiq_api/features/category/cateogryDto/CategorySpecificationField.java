package co.istad.rentiq_api.features.category.cateogryDto;

import co.istad.rentiq_api.features.category.enums.SpecificationFieldType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategorySpecificationField(
        @NotBlank(message = "Specification key is required")
        @Size(max = 100, message = "Specification key cannot exceed 100 characters")
        @Pattern(regexp = "[a-z][a-z0-9_]*", message = "Specification key must use lower-case letters, numbers, and underscores")
        String key,

        @NotBlank(message = "Specification label is required")
        @Size(max = 100, message = "Specification label cannot exceed 100 characters")
        String label,

        @NotNull(message = "Specification type is required")
        SpecificationFieldType type,

        boolean required,

        @Size(max = 100, message = "A specification field cannot contain more than 100 options")
        List<@NotBlank(message = "Specification options cannot be blank")
             @Size(max = 200, message = "Specification options cannot exceed 200 characters") String> options
) {
    public CategorySpecificationField {
        key = key == null ? null : key.trim();
        label = label == null ? null : label.trim();
        options = options == null ? List.of() : options.stream().map(String::trim).toList();
    }

    @AssertTrue(message = "SELECT specification fields require unique options; other field types cannot define options")
    public boolean isOptionsValid() {
        if (type == null) {
            return true;
        }
        if (type != SpecificationFieldType.SELECT) {
            return options.isEmpty();
        }
        return !options.isEmpty() && options.stream().distinct().count() == options.size();
    }
}
