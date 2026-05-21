package id.ac.ui.cs.advprog.donatjs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserStatusException extends RuntimeException {
    public UserStatusException(String message) {
        super(message);
    }
}
