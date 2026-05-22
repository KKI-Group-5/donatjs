package id.ac.ui.cs.advprog.donatjs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenTest {

    @Test
    void testConstructor_InitializesFieldsAndExpiryDate() {
        AppUser user = new AppUser();
        user.setEmail("test@gmail.com");

        VerificationToken token = new VerificationToken("my-token", user);

        assertEquals("my-token", token.getToken());
        assertEquals(user, token.getUser());
        assertNotNull(token.getExpiryDate());
        assertTrue(token.getExpiryDate().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void testSettersAndGetters() {
        VerificationToken token = new VerificationToken();
        AppUser user = new AppUser();

        token.setId(java.util.UUID.randomUUID());
        token.setToken("another-token");
        token.setUser(user);
        LocalDateTime expiry = LocalDateTime.now();
        token.setExpiryDate(expiry);

        assertNotNull(token.getId());
        assertEquals("another-token", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals(expiry, token.getExpiryDate());
    }
}
