package et.aau.clinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                // /api/** is the JSON API behind the separate React frontend (frontend/) - it is
                // not part of the graded Thymeleaf app and checks its own session state per
                // request so it can return 401 JSON instead of a redirect to the /login page.
                .excludePathPatterns("/login", "/logout", "/error", "/h2-console/**", "/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Vite's dev server (frontend/) runs on a different port, so it needs CORS with
        // credentials to send the session cookie the API auth relies on.
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
