package com.bga.facturacion_services.facturacion.infrastructure.config;

import com.bga.facturacion_services.facturacion.application.AltaFacturaWeb;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.infrastructure.persistence.AltaFacturaOracle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OracleAltaConfig {
    @Bean(name = "oracleAltaDataSource")
    @Primary
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

    @Bean(name = "facturacionDataSource")
    @ConditionalOnExpression("'${facturacion.oracle.url:}' != ''")
    DataSource facturacionDataSource(
            @Value("${facturacion.oracle.url}") String url,
            @Value("${facturacion.oracle.facturacion.username}") String usuario,
            @Value("${facturacion.oracle.facturacion.password:}") String clave) {
        if (clave == null || clave.isBlank()) return new DataSourceSinClave();
        var pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername(usuario);
        pool.setPassword(clave);
        pool.setMinimumIdle(0);
        pool.setMaximumPoolSize(4);
        pool.setInitializationFailTimeout(-1);
        pool.setPoolName("oracle-facturacion");
        return pool;
    }

    static final class DataSourceSinClave implements DataSource {
        private SQLException falta() {
            return new SQLException("Falta la clave del usuario FACTURACION. Defina ORACLE_FACTURACION_PASSWORD.");
        }

        public Connection getConnection() throws SQLException { throw falta(); }
        public Connection getConnection(String username, String password) throws SQLException { throw falta(); }
        public PrintWriter getLogWriter() { return null; }
        public void setLogWriter(PrintWriter out) {}
        public void setLoginTimeout(int seconds) {}
        public int getLoginTimeout() { return 0; }
        public Logger getParentLogger() { return Logger.getGlobal(); }
        public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        public boolean isWrapperFor(Class<?> iface) { return false; }
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
