package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.UserProfileDTO;
import id.ac.ui.cs.advprog.donatjs.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void testGetMyProfile_Success() throws Exception {
        UserProfileDTO mockDto = new UserProfileDTO("Aldebaran", "aldebaran@ui.ac.id", "Bio", null);

        when(profileService.getUserProfile(anyString())).thenReturn(mockDto);

        mockMvc.perform(get("/api/profile/me")
                        .with(oauth2Login()
                                .attributes(attrs -> {
                                    attrs.put("email", "aldebaran@ui.ac.id");
                                    attrs.put("name", "Aldebaran");
                                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aldebaran"))
                .andExpect(jsonPath("$.email").value("aldebaran@ui.ac.id"));
    }

    @Test
    void testUpdateMyProfile_Success() throws Exception {
        UserProfileDTO updatedDto = new UserProfileDTO("New Name", "aldebaran@ui.ac.id", "New Bio", null);

        when(profileService.updateUserProfile(anyString(), any())).thenReturn(updatedDto);

        // Change 'post' to 'put' to match your @PutMapping annotation
        mockMvc.perform(put("/api/profile/update")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "aldebaran@ui.ac.id")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\", \"bio\":\"New Bio\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.bio").value("New Bio"));
    }
}