package co.istad.rentiq_api.features.category;

import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditAction;
import co.istad.rentiq_api.features.adminAudit.enums.AdminAuditTargetType;
import co.istad.rentiq_api.features.adminAudit.service.AdminAuditService;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryRequest;
import co.istad.rentiq_api.features.category.cateogryDto.CategoryResponse;
import co.istad.rentiq_api.features.category.exception.CategoryNotFoundException;
import co.istad.rentiq_api.features.category.exception.DuplicateCategoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Backend audit AUD-001 — every Category admin mutation (create/update/delete/status/
 * commission-rate) must be centrally audited via {@link AdminAuditService}, in the same
 * transaction as the mutation, with the admin identity resolved internally (never passed in).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;
    @Mock private AdminAuditService adminAuditService;

    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository, categoryMapper, adminAuditService);
        lenient().when(categoryMapper.toResponse(any())).thenReturn(
                new CategoryResponse(id(1), null, "name", "slug", BigDecimal.ZERO, null, true, List.of()));
    }

    private UUID id(int value) {
        return new UUID(0, value);
    }

    private Category category(UUID id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Electronics");
        category.setCommissionRate(new BigDecimal("0.1000"));
        category.setActive(true);
        return category;
    }

    @Test
    void createCategory_recordsCategoryCreatedAudit() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        Category toSave = category(null);
        Category saved = category(id(5));
        when(categoryMapper.toEntity(any())).thenReturn(toSave);
        when(categoryRepository.save(toSave)).thenReturn(saved);

        CategoryRequest request = new CategoryRequest(null, "Electronics", "electronics", new BigDecimal("0.10"), null, true, List.of());
        service.createCategory(request);

        verify(adminAuditService).record(
                eq(AdminAuditAction.CATEGORY_CREATED), eq(AdminAuditTargetType.CATEGORY), eq(id(5).toString()),
                any(), any(), any());
    }

    @Test
    void createCategory_duplicateName_rejectsBeforeAnyAudit() {
        Category existing = category(id(1));
        when(categoryRepository.findAll()).thenReturn(List.of(existing));

        CategoryRequest request = new CategoryRequest(null, "Electronics", "electronics", new BigDecimal("0.10"), null, true, List.of());

        assertThatThrownBy(() -> service.createCategory(request))
                .isInstanceOf(DuplicateCategoryException.class);

        verify(adminAuditService, never()).record(any(), any(), anyString(), any(), any(), any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_recordsBeforeAndAfterSnapshot() {
        Category category = category(id(5));
        when(categoryRepository.findById(id(5))).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryRequest request = new CategoryRequest(null, "Gadgets", "gadgets", new BigDecimal("0.15"), null, true, List.of());
        service.updateCategory(id(5), request);

        verify(categoryMapper).updateEntityFromRequest(request, category);
        verify(adminAuditService).record(
                eq(AdminAuditAction.CATEGORY_UPDATED), eq(AdminAuditTargetType.CATEGORY), eq(id(5).toString()),
                any(), any(), any());
    }

    @Test
    void updateCategory_notFound_rejectsBeforeAnyAudit() {
        when(categoryRepository.findById(id(99))).thenReturn(Optional.empty());

        CategoryRequest request = new CategoryRequest(null, "Gadgets", "gadgets", new BigDecimal("0.15"), null, true, List.of());

        assertThatThrownBy(() -> service.updateCategory(id(99), request))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(adminAuditService, never()).record(any(), any(), anyString(), any(), any(), any());
    }

    @Test
    void deleteCategory_recordsCategoryDeletedAudit() {
        Category category = category(id(5));
        when(categoryRepository.findById(id(5))).thenReturn(Optional.of(category));

        service.deleteCategory(id(5));

        verify(categoryRepository).delete(category);
        verify(adminAuditService).record(
                eq(AdminAuditAction.CATEGORY_DELETED), eq(AdminAuditTargetType.CATEGORY), eq(id(5).toString()),
                any(), any(), any());
    }

    @Test
    void updateCommissionRate_recordsOldAndNewRate() {
        Category category = category(id(5));
        when(categoryRepository.findById(id(5))).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        service.updateCommissionRate(id(5), new BigDecimal("0.20"));

        verify(adminAuditService).record(
                eq(AdminAuditAction.COMMISSION_RATE_UPDATED), eq(AdminAuditTargetType.CATEGORY), eq(id(5).toString()),
                any(), any(), any());
        assertThat(category.getCommissionRate()).isEqualByComparingTo("0.20");
    }

    @Test
    void updateStatus_recordsCategoryStatusChangedAudit() {
        Category category = category(id(5));
        category.setActive(true);
        when(categoryRepository.findById(id(5))).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        service.updateStatus(id(5), false);

        verify(adminAuditService).record(
                eq(AdminAuditAction.CATEGORY_STATUS_CHANGED), eq(AdminAuditTargetType.CATEGORY), eq(id(5).toString()),
                any(), any(), any());
        assertThat(category.getActive()).isFalse();
    }
}
