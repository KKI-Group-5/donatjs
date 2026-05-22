package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.RegisterRequest;
import id.ac.ui.cs.advprog.donatjs.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.registerUser(request);
            return ResponseEntity.ok("User registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify")
    public org.springframework.web.servlet.ModelAndView showVerifyPage(@RequestParam(value = "token", required = false) String token) {
        org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView("auth/verify");
        if (token != null) {
            boolean isVerified = authService.verifyEmail(token);
            mav.addObject("success", isVerified);
        }
        return mav;
    }

    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<String> verifyEmailPost(@RequestBody java.util.Map<String, String> payload) {
        String token = payload.get("token");
        boolean isVerified = authService.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired code");
        }
    }
}
