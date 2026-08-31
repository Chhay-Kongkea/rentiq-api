package co.istad.rentiq_api.features;

import co.istad.rentiq_api.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;

/**
 * Client disconnects (a browser cancelling a PDF/XLSX download mid-write) surface as
 * AsyncRequestNotUsableException wrapping a Tomcat ClientAbortException. These are normal
 * client-side events, not backend failures, and must never be reported through the generic
 * "Unexpected error" handler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getRequestURI()).thenReturn("/api/v1/bookings/123/receipt");
    }

    @Test
    void handleClientDisconnect_acceptsAsyncRequestNotUsableException_withoutThrowing() {
        AsyncRequestNotUsableException exception =
                new AsyncRequestNotUsableException("ServletOutputStream failed to write");

        assertThatCode(() -> handler.handleClientDisconnect(exception, request)).doesNotThrowAnyException();
    }

    @Test
    void handleClientDisconnect_acceptsClientAbortException_withoutThrowing() {
        ClientAbortException exception = new ClientAbortException(new IOException("Connection reset by peer"));

        assertThatCode(() -> handler.handleClientDisconnect(exception, request)).doesNotThrowAnyException();
    }

    @Test
    void handleClientDisconnect_returnsVoid_noApiErrorResponseIsBuilt() throws NoSuchMethodException {
        // The dedicated handler must never attempt a second response body — void return type is
        // the structural guarantee that nothing gets written back to the (already-dead) client.
        Method method = GlobalExceptionHandler.class.getMethod(
                "handleClientDisconnect", Exception.class, HttpServletRequest.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    void handleClientDisconnect_isRegisteredForBothExceptionTypes() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getMethod(
                "handleClientDisconnect", Exception.class, HttpServletRequest.class);
        ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);

        assertThat(annotation.value()).containsExactlyInAnyOrder(
                AsyncRequestNotUsableException.class, ClientAbortException.class);
    }

    @Test
    void genericRuntimeException_stillHandledByGenericHandler() {
        RuntimeException exception = new RuntimeException("boom");

        ApiErrorResponse response = handler.handleUnexpectedException(exception, request);

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    void genericIOException_isNotClaimedByClientDisconnectHandler_fallsThroughToGenericHandler()
            throws NoSuchMethodException {
        // A plain IOException (unrelated to a client abort — e.g. a real disk/network failure
        // reading a file) must not be silently swallowed. It is not one of the two types
        // registered on handleClientDisconnect, so Spring's dispatcher routes it to the generic
        // Exception handler instead — verified structurally (no IOException.class in the
        // dedicated handler's @ExceptionHandler value) and behaviorally (the generic handler
        // still produces a real error response for it).
        Method method = GlobalExceptionHandler.class.getMethod(
                "handleClientDisconnect", Exception.class, HttpServletRequest.class);
        List<Class<?>> registeredTypes = List.of(method.getAnnotation(ExceptionHandler.class).value());
        assertThat(registeredTypes).doesNotContain(IOException.class);

        IOException exception = new IOException("disk full");
        ApiErrorResponse response = handler.handleUnexpectedException(exception, request);

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
