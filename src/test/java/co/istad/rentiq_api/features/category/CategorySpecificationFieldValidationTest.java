package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField;
import co.istad.rentiq_api.features.category.enums.SpecificationFieldType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategorySpecificationFieldValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void selectRequiresOptions() {
        var field = new CategorySpecificationField(
                "brand", "Brand", SpecificationFieldType.SELECT, true, List.of()
        );

        assertThat(validator.validate(field)).isNotEmpty();
    }

    @Test
    void validSelectSchemaIsAccepted() {
        var field = new CategorySpecificationField(
                "brand", "Brand", SpecificationFieldType.SELECT, true,
                List.of("Dell", "HP")
        );

        assertThat(validator.validate(field)).isEmpty();
    }
}
