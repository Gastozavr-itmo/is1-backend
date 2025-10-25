package ru.se.ifmo.is1.config;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import java.util.List;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{ HibernateConfig.class };
    }
    @Override protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{ WebConfig.class };
    }
    @Override protected String[] getServletMappings() {
        return new String[]{ "/api/*" };
    }

    @Override
    protected Filter[] getServletFilters() {
        // Готовим конфиг прямо здесь
        var cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Origin","Accept","Content-Type","Authorization","X-Requested-With"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return new Filter[] { new CorsFilter(source) };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // чтобы OPTIONS обрабатывался диспетчером при необходимости
        registration.setInitParameter("dispatchOptionsRequest", "true");
    }
}
