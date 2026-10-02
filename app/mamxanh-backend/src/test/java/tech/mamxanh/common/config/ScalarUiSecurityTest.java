package tech.mamxanh.common.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ScalarUiSecurityTest.TestController.class)
@Import({ SecurityConfig.class, ScalarUiController.class })
class ScalarUiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test")
        String test() {
            return "ok";
        }
    }

    @Test
    void scalarRouteServesTheUiShellWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/scalar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("API Reference")))
                .andExpect(content().string(containsString("@scalar/api-reference@1.72.3")));
    }

    @Test
    void scalarStaticPathUsesTheSamePublicDocumentationPolicy() throws Exception {
        mockMvc.perform(get("/scalar/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/v3/api-docs")));
    }
}
