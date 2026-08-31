package co.istad.rentiq_api.features.item.service;

import co.istad.rentiq_api.common.exception.InvalidOperationException;
import co.istad.rentiq_api.features.category.Category;
import co.istad.rentiq_api.features.category.CategoryRepository;
import co.istad.rentiq_api.features.category.cateogryDto.CategorySpecificationField;
import co.istad.rentiq_api.features.category.enums.SpecificationFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemSpecificationValidatorTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ItemSpecificationValidator validator = new ItemSpecificationValidator(categoryRepository);
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(categoryId);
        category.setSpecificationFields(List.of(
                new CategorySpecificationField(
                        "brand", "Brand", SpecificationFieldType.SELECT, true,
                        List.of("Dell", "HP", "Lenovo", "Apple")
                ),
                new CategorySpecificationField(
                        "notes", "Notes", SpecificationFieldType.TEXT, false, List.of()
                )
        ));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
    }

    @Test
    void acceptsSpecificationsMatchingCategorySchema() {
        assertThatCode(() -> validator.validate(
                categoryId, Map.of("brand", "Apple", "notes", "M3 model")
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingRequiredSpecification() {
        assertThatThrownBy(() -> validator.validate(categoryId, Map.of()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("brand");
    }

    @Test
    void rejectsSelectValueOutsideConfiguredOptions() {
        assertThatThrownBy(() -> validator.validate(categoryId, Map.of("brand", "Asus")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("brand");
    }

    @Test
    void rejectsUnknownSpecificationKey() {
        assertThatThrownBy(() -> validator.validate(
                categoryId, Map.of("brand", "Dell", "cpu", "Intel Core i7")
        )).isInstanceOf(InvalidOperationException.class)
          .hasMessageContaining("cpu");
    }
}
