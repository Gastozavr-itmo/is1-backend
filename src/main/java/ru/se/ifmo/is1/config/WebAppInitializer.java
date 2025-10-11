package ru.se.ifmo.is1.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override protected Class<?>[] getRootConfigClasses() { return new Class<?>[]{ HibernateConfig.class }; }
    @Override protected Class<?>[] getServletConfigClasses() { return new Class<?>[]{ WebConfig.class }; }
    @Override protected String[] getServletMappings() { return new String[]{ "/api/*" }; } // базовый префикс API
}
