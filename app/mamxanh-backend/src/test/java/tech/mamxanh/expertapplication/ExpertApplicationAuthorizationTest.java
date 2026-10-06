package tech.mamxanh.expertapplication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.config.MethodSecurityConfiguration;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.expertapplication.controller.ExpertApplicationController;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@WebMvcTest(ExpertApplicationController.class)
@Import({MethodSecurityConfiguration.class, GlobalExceptionHandler.class})
class ExpertApplicationAuthorizationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ExpertApplicationService service;
    @MockitoBean private UserRepository userRepository;

    @Test
    void expertAndAdminAreForbiddenFromCustomerSubmissionEndpoint() throws Exception {
        String body = """
                {"experience":"Experience in vegetarian cooking","vegetarianType":"VEGAN","sampleRecipeSummary":"A detailed vegetarian recipe sample summary."}
                """;

        mockMvc.perform(post("/api/v1/expert-applications").with(user("expert").roles("EXPERT")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/expert-applications").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
