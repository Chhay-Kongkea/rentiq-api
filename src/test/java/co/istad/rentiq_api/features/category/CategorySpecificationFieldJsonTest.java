package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategorySpecificationFieldJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesRequestContainingLegacyOptionsValidField() throws Exception {
        String json = "{\"key\":\"brand\",\"label\":\"Brand\",\"type\":\"TEXT\",\"required\":true,\"options\":[],\"optionsValid\":true}";

        CategorySpecificationField field = mapper.readValue(json, CategorySpecificationField.class);

        assertThat(field.key()).isEqualTo("brand");
    }

    @Test
    void serializationDoesNotLeakOptionsValid() throws Exception {
        var field = new CategorySpecificationField(
                "brand", "Brand", co.istad.rentiq_api.features.category.enums.SpecificationFieldType.TEXT, true, java.util.List.of()
        );

        String json = mapper.writeValueAsString(field);

        assertThat(json).doesNotContain("optionsValid");
    }
}
