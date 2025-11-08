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
        // инфраструктура: БД/транзакции и т.д.
        return new Class<?>[]{ HibernateConfig.class };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // веб-слой: контроллеры, конвертеры, multipart-резолвер и пр. лежат в WebConfig
        return new Class<?>[]{ WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        // все контроллеры доступны под /api/**
        return new String[]{ "/api/*" };
    }

    @Override
    protected Filter[] getServletFilters() {
        // CORS
        var corsCfg = new CorsConfiguration();
        corsCfg.setAllowCredentials(true);
        corsCfg.setAllowedOrigins(List.of("http://localhost:53062", "http://127.0.0.1:53062"));
        corsCfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        corsCfg.setAllowedHeaders(List.of("Origin","Accept","Content-Type","Authorization","X-Requested-With"));
        corsCfg.setExposedHeaders(List.of("Location"));
        corsCfg.setMaxAge(3600L);

        var corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", corsCfg);

        // Единая кодировка на вход/выход (важно для multipart + JSON)
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
        // Нужен для StandardServletMultipartResolver (иначе UT010057)
        // location=null => системная temp-директория контейнера
        long maxFileSize = 10L * 1024 * 1024;     // 10 MB
        long maxRequestSize = 20L * 1024 * 1024;  // 20 MB
        int fileSizeThreshold = 0;                // сразу писать во временный файл

        registration.setMultipartConfig(new MultipartConfigElement(
                null,              // location
                maxFileSize,       // maxFileSize
                maxRequestSize,    // maxRequestSize
                fileSizeThreshold  // fileSizeThreshold
        ));

        // Чтобы OPTIONS запросы корректно обрабатывались диспетчером (для CORS preflight)
        registration.setInitParameter("dispatchOptionsRequest", "true");

        // (опционально) Если хочешь 404 отдавать через DispatcherServlet:
        // registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    }
}
