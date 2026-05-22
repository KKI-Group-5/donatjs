package id.ac.ui.cs.advprog.donatjs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionClassTest {

    @Test
    void profileIncompleteException_storesMessage() {
        ProfileIncompleteException ex = new ProfileIncompleteException("Profile not complete");
        assertThat(ex.getMessage()).isEqualTo("Profile not complete");
    }

    @Test
    void profileIncompleteException_isRuntimeException() {
        assertThatThrownBy(() -> { throw new ProfileIncompleteException("missing fields"); })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("missing fields");
    }

    @Test
    void userStatusException_storesMessage() {
        UserStatusException ex = new UserStatusException("User is banned");
        assertThat(ex.getMessage()).isEqualTo("User is banned");
    }

    @Test
    void userStatusException_isRuntimeException() {
        assertThatThrownBy(() -> { throw new UserStatusException("suspended"); })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("suspended");
    }
}
