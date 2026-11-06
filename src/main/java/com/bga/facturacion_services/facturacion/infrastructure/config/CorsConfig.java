package com.bga.facturacion_services.facturacion.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origenes;

    public CorsConfig(@Value("${app.cors.origenes:http://localhost:4200}") String origenes) {
        this.origenes = origenes.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var patrones = new String[origenes.length + 2];
        System.arraycopy(origenes, 0, patrones, 0, origenes.length);
        patrones[origenes.length] = "http://localhost:*";
        patrones[origenes.length + 1] = "http://127.0.0.1:*";
        registry.addMapping("/api/**").allowedOriginPatterns(patrones)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS").allowedHeaders("*");
    }
}
