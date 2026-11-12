package com.bga.facturacion_services.facturacion.infrastructure.config;

import com.bga.facturacion_services.facturacion.application.AltaFacturaWeb;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.infrastructure.persistence.AltaFacturaOracle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OracleAltaConfig {
    @Bean(name = "oracleAltaDataSource")
    @ConditionalOnExpression("'${facturacion.oracle.url:}' != ''")
    DataSource oracleAltaDataSource(
            @Value("${facturacion.oracle.url}") String url,
            @Value("${facturacion.oracle.username}") String usuario,
            @Value("${facturacion.oracle.password:}") String clave) {
        var pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername(usuario);
        pool.setPassword(clave);
        pool.setMaximumPoolSize(4);
        pool.setPoolName("oracle-alta-web");
        return pool;
    }

    @Bean
    @ConditionalOnExpression("'${facturacion.oracle.url:}' != ''")
    AltaFacturaWeb altaFacturaOracle(
            @Qualifier("oracleAltaDataSource") DataSource oracleAltaDataSource,
            ObjectMapper json,
            @Value("${facturacion.emisor.codigo-sistema-origen:FACTWEB}") String origen,
            @Value("${facturacion.oracle.codigo-canal:0}") int canal,
            @Value("${facturacion.emisor.codigo-tipo-factura:1}") int tipoFactura) {
        String sistema = origen == null || origen.isBlank() ? "FACTWEB" : origen.trim();
        return new AltaFacturaOracle(oracleAltaDataSource, json, sistema, canal, tipoFactura);
    }

    @Bean
    @ConditionalOnMissingBean(AltaFacturaWeb.class)
    AltaFacturaWeb altaFacturaWebInactiva() {
        return new AltaFacturaWeb() {
            public Resultado registrar(Factura factura) { return null; }
        };
    }
}
