package id.ac.ui.cs.advprog.donatjs.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomOAuth2UserTest {

    @Test
    void testCustomOAuth2UserNonAdmin() {
        OAuth2User delegate = mock(OAuth2User.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@test.com");
        
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("SCOPE_read"))).when(delegate).getAuthorities();
        when(delegate.getAttributes()).thenReturn(attributes);
        when(delegate.getName()).thenReturn("Test Name");
        when(delegate.getAttribute("email")).thenReturn("test@test.com");

        CustomOAuth2User user = new CustomOAuth2User(delegate, false);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("SCOPE_read")));
        
        assertEquals(attributes, user.getAttributes());
        assertEquals("Test Name", user.getName());
        assertEquals("test@test.com", user.getAttribute("email"));
    }

    @Test
    void testCustomOAuth2UserAdmin() {
        OAuth2User delegate = mock(OAuth2User.class);
        
        when(delegate.getAuthorities()).thenReturn(null);

        CustomOAuth2User user = new CustomOAuth2User(delegate, true);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
