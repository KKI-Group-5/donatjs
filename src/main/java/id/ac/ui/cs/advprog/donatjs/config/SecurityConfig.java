package id.ac.ui.cs.advprog.donatjs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabled for local development convenience
                .authorizeHttpRequests(auth -> auth
                        // Open to guests
                        .requestMatchers("/", "/h2-console/**", "/api/campaigns/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/auth/**").permitAll()
                        // Require the user to be logged in
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.defaultSuccessUrl("/api/profile/me", true))
                .oauth2Login(oauth2 -> oauth2
                        // If login is successful, send user to this URL
                        .defaultSuccessUrl("/api/profile/me", true)
                );

        // Required so that the H2 database console still works
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }
}