package id.ac.ui.cs.advprog.donatjs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserStatusInterceptor userStatusInterceptor;

    @Override
    @SuppressWarnings("null")
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Apply status checks to all API endpoints
        registry.addInterceptor(userStatusInterceptor)
                .addPathPatterns("/api/**");
    }
}
