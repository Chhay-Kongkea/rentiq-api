package co.istad.rentiq_api.features.item.controller;

import co.istad.rentiq_api.features.item.dto.request.AdminItemFilter;
import co.istad.rentiq_api.features.item.dto.respone.AdminItemResponse;
import co.istad.rentiq_api.features.item.dto.respone.PageResponse;
import co.istad.rentiq_api.features.item.enums.ItemApprovalStatus;
import co.istad.rentiq_api.features.item.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminItemControllerTest {

    private final ItemService itemService = mock(ItemService.class);
    private final AdminItemController controller = new AdminItemController(itemService);

    @Test
    void listEndpoint_delegatesPendingModerationQuery() {
        AdminItemFilter filter = new AdminItemFilter("camera", ItemApprovalStatus.PENDING);
        PageResponse<AdminItemResponse> response = new PageResponse<>(
                List.of(), 0, 20, 0, 0, true, true, false, false
        );
        when(itemService.getAdminItems(filter, 0, 20, "createdAt", "desc"))
                .thenReturn(response);

        assertThat(controller.getItems(filter, 0, 20, "createdAt", "desc").getBody())
                .isSameAs(response);
        verify(itemService).getAdminItems(filter, 0, 20, "createdAt", "desc");
    }

    @Test
    void detailEndpoint_delegatesWithoutPublicApprovalRestriction() {
        UUID itemId = UUID.randomUUID();
        AdminItemResponse response = mock(AdminItemResponse.class);
        when(itemService.getAdminItemById(itemId)).thenReturn(response);

        assertThat(controller.getItem(itemId).getBody()).isSameAs(response);
    }

    @Test
    void readEndpointsExistAndControllerRemainsAdminOnly() throws Exception {
        PreAuthorize authorization = AdminItemController.class.getAnnotation(PreAuthorize.class);
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");

        Method list = AdminItemController.class.getMethod(
                "getItems", AdminItemFilter.class, int.class, int.class, String.class, String.class
        );
        Method detail = AdminItemController.class.getMethod("getItem", UUID.class);
        assertThat(list.getAnnotation(GetMapping.class).value()).isEmpty();
        assertThat(detail.getAnnotation(GetMapping.class).value()).containsExactly("/{itemId}");
    }
}
