package co.istad.rentiq_api.features.category.cateogryDto;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;

public record CategoryRequest(
        UUID parentId,
        @JsonAlias("title")
        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Category name cannot exceed 255 characters")
        String name,
        String slug,
        BigDecimal commissionRate,
        String iconUrl,
        Boolean active,
        @Valid
        @Size(max = 50, message = "A category cannot contain more than 50 specification fields")
        List<CategorySpecificationField> specificationFields
) {
}
