package id.ac.ui.cs.advprog.donatjs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/")
    public String landingPage() {
        // This tells Spring Boot to look for a file named "index.html"
        return "index";
    }
}