package tech.mamxanh.recipe.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.config.MethodSecurityConfiguration;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.response.RecipeMediaResponse;
import tech.mamxanh.recipe.dto.response.UploadImageResponse;
import tech.mamxanh.recipe.service.RecipeMediaService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeMediaController.class)
@Import({SecurityConfig.class, MethodSecurityConfiguration.class, GlobalExceptionHandler.class})
class RecipeMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeMediaService recipeMediaService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    @DisplayName("POST /api/v1/recipes/media/upload with ROLE_EXPERT returns 201 Created")
    void uploadImage_expert_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salad.jpg",
                "image/jpeg",
                new byte[100]
        );

        when(recipeMediaService.uploadImage(any())).thenReturn(
                new UploadImageResponse("https://blob/salad.jpg", "image/jpeg", 100)
        );

        mockMvc.perform(multipart("/api/v1/recipes/media/upload")
                        .file(file)
                        .with(user("expert").roles("EXPERT"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.blobUrl").value("https://blob/salad.jpg"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/media/upload with ROLE_CUSTOMER returns 403 Forbidden")
    void uploadImage_customer_forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salad.jpg",
                "image/jpeg",
                new byte[100]
        );

        mockMvc.perform(multipart("/api/v1/recipes/media/upload")
                        .file(file)
                        .with(user("customer").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/recipes/media/upload without authentication returns 401 Unauthorized")
    void uploadImage_unauthenticated_unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salad.jpg",
                "image/jpeg",
                new byte[100]
        );

        mockMvc.perform(multipart("/api/v1/recipes/media/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{recipeId}/media is publicly accessible")
    void getRecipeMedia_public() throws Exception {
        when(recipeMediaService.getRecipeMedia(100L)).thenReturn(List.of(
                new RecipeMediaResponse(1L, 100L, "https://blob/1.jpg", "image/jpeg", 1, true)
        ));

        mockMvc.perform(get("/api/v1/recipes/100/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].blobUrl").value("https://blob/1.jpg"))
                .andExpect(jsonPath("$.data[0].isCover").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/recipes/{recipeId}/media with ROLE_EXPERT returns 200 OK")
    void updateRecipeMedia_expert_success() throws Exception {
        String jsonPayload = """
                {
                    "mediaItems": [
                        {
                            "blobUrl": "https://blob/1.jpg",
                            "mimeType": "image/jpeg",
                            "displayOrder": 1,
                            "isCover": true
                        }
                    ]
                }
                """;

        when(recipeMediaService.setRecipeMedia(eq(100L), any())).thenReturn(List.of(
                new RecipeMediaResponse(1L, 100L, "https://blob/1.jpg", "image/jpeg", 1, true)
        ));

        mockMvc.perform(put("/api/v1/recipes/100/media")
                        .with(user("expert").roles("EXPERT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/recipes/{recipeId}/media with ROLE_EXPERT returns 200 OK")
    void deleteRecipeMedia_expert_success() throws Exception {
        mockMvc.perform(delete("/api/v1/recipes/100/media")
                        .with(user("expert").roles("EXPERT"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(recipeMediaService).deleteRecipeMedia(100L);
    }
}
