package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String REGISTER_JSON = """
            {
                "email": "aldebaran@ui.ac.id",
                "password": "securepassword",
                "name": "Aldebaran",
                "bio": "Software developer",
                "dateOfBirth": "2000-01-01"
            }
            """;

    @Test
    void register_ValidRequest_Returns200WithSuccessMessage() throws Exception {
        AppUser newUser = new AppUser();
        newUser.setEmail("aldebaran@ui.ac.id");
        newUser.setName("Aldebaran");
        when(authService.registerUser(any())).thenReturn(newUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void register_DuplicateEmail_Returns400WithErrorMessage() throws Exception {
        when(authService.registerUser(any()))
                .thenThrow(new RuntimeException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is already registered"));
    }
    @Test
    void register_InvalidRequest_Returns400() throws Exception {
        String invalidJson = """
                {
                    "email": "invalid-email",
                    "password": "pwd",
                    "name": "No Bio",
                    "bio": "", 
                    "dateOfBirth": "2000-01-01"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_ValidToken_Returns200() throws Exception {
        when(authService.verifyEmail("valid-token")).thenReturn(true);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/verify")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("auth/verify"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("success", true));
    }

    @Test
    void verify_InvalidToken_Returns400() throws Exception {
        when(authService.verifyEmail("invalid-token")).thenReturn(false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("auth/verify"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("success", false));
    }
}
