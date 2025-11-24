package ru.se.ifmo.is1.config;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{ HibernateConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{ WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{ "/api/*" };
    }

    @Override
    protected Filter[] getServletFilters() {
        var corsCfg = new CorsConfiguration();
        corsCfg.setAllowCredentials(true);
        corsCfg.setAllowedOrigins(List.of("http://localhost:53062", "http://127.0.0.1:53062"));
        corsCfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        corsCfg.setAllowedHeaders(List.of("Origin","Accept","Content-Type","Authorization","X-Requested-With"));
        corsCfg.setExposedHeaders(List.of("Location"));
        corsCfg.setMaxAge(3600L);

        var corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", corsCfg);

        var encoding = new CharacterEncodingFilter();
        encoding.setEncoding(StandardCharsets.UTF_8.name());
        encoding.setForceEncoding(true);

        return new Filter[] {
                new CorsFilter(corsSource),
                encoding
        };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        long maxFileSize = 10L * 1024 * 1024;
        long maxRequestSize = 20L * 1024 * 1024;
        int fileSizeThreshold = 0;

        registration.setMultipartConfig(new MultipartConfigElement(
                null,
                maxFileSize,
                maxRequestSize,
                fileSizeThreshold
        ));
        registration.setInitParameter("dispatchOptionsRequest", "true");

    }
}
