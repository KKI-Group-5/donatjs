package id.ac.ui.cs.advprog.donatjs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import id.ac.ui.cs.advprog.donatjs.security.OAuth2LoginSuccessHandler;
import id.ac.ui.cs.advprog.donatjs.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LOGIN_URL = "/login";
    
    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .authorizeHttpRequests(auth -> auth
                        // Static assets and public API endpoints
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/h2-console/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/**", LOGIN_URL, "/register").permitAll()
                        .requestMatchers("/api/campaigns/**", "/api/saved-campaigns/**").permitAll()
                        // Guests can browse campaigns (M2: allowed only GET for listing and detail)
                        .requestMatchers(HttpMethod.GET, "/campaigns", "/campaigns/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/campaigns/{id:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/**").permitAll()
                        // Leaderboard data is public — allow unauthenticated JS polling
                        .requestMatchers(HttpMethod.GET, "/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donations/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donations/campaign/*/leaderboard").permitAll()
                        // Fraud reporting called by internal modules — requires authentication
                        .requestMatchers("/api/users/report-fraud-activity").authenticated()
                        // Admin moderation endpoints — requires ADMIN role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage(LOGIN_URL)
                        .permitAll()
                        .defaultSuccessUrl("/", false)
                )
                .oauth2Login(oauth2 -> {
                        oauth2.loginPage(LOGIN_URL);
                        if (customOAuth2UserService != null) {
                            oauth2.userInfoEndpoint(userInfo ->
                                    userInfo.userService(customOAuth2UserService));
                        }
                        if (oAuth2LoginSuccessHandler != null) {
                            oauth2.successHandler(oAuth2LoginSuccessHandler);
                        } else {
                            oauth2.defaultSuccessUrl("/", true);
                        }
                });

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}
