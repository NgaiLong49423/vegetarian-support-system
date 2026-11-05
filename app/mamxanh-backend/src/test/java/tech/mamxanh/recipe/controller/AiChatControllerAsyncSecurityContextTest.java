package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.service.AiChatCancellationSignal;
import tech.mamxanh.recipe.service.AiChatService;
import tech.mamxanh.recipe.service.GuestChatRateLimiter;

class AiChatControllerAsyncSecurityContextTest {

    @Test
    void streamingWorkerRetainsAuthenticatedPrincipal() throws Exception {
        AiChatService service = mock(AiChatService.class);
        AiChatCancellationSignal cancellationSignal = new AiChatCancellationSignal();
        CountDownLatch streamed = new CountDownLatch(1);
        AtomicReference<String> principal = new AtomicReference<>();
        org.mockito.Mockito.when(service.registerStreamCancellation(any())).thenReturn(cancellationSignal);
        doAnswer(invocation -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            principal.set(authentication == null ? null : authentication.getName());
            streamed.countDown();
            return null;
        }).when(service).stream(any(), any(), any());

        AiChatController controller = new AiChatController(service,
                new GuestChatRateLimiter(Clock.systemUTC()));
        var authentication = new UsernamePasswordAuthenticationToken("10", "",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        AiChatRequest request = new AiChatRequest(ContextType.GENERAL, null,
                "Món chay nào giàu đạm?", List.of());
        HttpServletRequest httpRequest = new MockHttpServletRequest();

        controller.stream(request, authentication, httpRequest);

        assertThat(streamed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(principal.get()).isEqualTo("10");
    }
}
