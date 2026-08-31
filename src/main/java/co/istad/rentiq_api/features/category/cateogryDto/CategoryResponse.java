package co.istad.rentiq_api.features.category.cateogryDto;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        BigDecimal commissionRate,
        String iconUrl,
        Boolean active,
        List<CategorySpecificationField> specificationFields
) {
    public CategoryResponse {
        specificationFields = specificationFields == null ? List.of() : List.copyOf(specificationFields);
    }

    @JsonProperty("categoryId")
    public UUID categoryId() {
        return id;
    }

    @JsonProperty("title")
    public String title() {
        return name;
    }
}
