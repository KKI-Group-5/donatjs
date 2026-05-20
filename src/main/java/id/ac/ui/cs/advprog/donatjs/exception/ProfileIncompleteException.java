package id.ac.ui.cs.advprog.donatjs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ProfileIncompleteException extends RuntimeException {
    public ProfileIncompleteException(String message) {
        super(message);
    }
}
