package id.ac.ui.cs.advprog.donatjs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserStatusInterceptor userStatusInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply status checks to all API endpoints
        registry.addInterceptor(userStatusInterceptor)
                .addPathPatterns("/api/**");
    }
}
