package com.bga.facturacion_services.facturacion.infrastructure.config;

import com.bga.facturacion_services.facturacion.application.*;
import com.bga.facturacion_services.facturacion.domain.*;
import com.bga.facturacion_services.facturacion.infrastructure.provider.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.*;
import java.util.List;

@Configuration
public class FacturacionConfiguration {
    @Bean ReglaFactura compraVenta() { return new CompraVenta(); }
    @Bean Clock reloj() { return Clock.system(ZoneId.of("America/La_Paz")); }
    @Bean Factura.Emisor emisor(
            @Value("${facturacion.emisor.nit}") String nit,
            @Value("${facturacion.emisor.razon-social}") String nombre,
            @Value("${facturacion.emisor.municipio}") String municipio,
            @Value("${facturacion.emisor.direccion}") String direccion,
            @Value("${facturacion.emisor.sucursal}") int sucursal,
            @Value("${facturacion.emisor.punto-venta}") int punto) {
        return new Factura.Emisor(nit, nombre, municipio, direccion, sucursal, punto);
    }
    @Bean ProveedorFacturacion proveedor(ObjectMapper json,
            @Value("${facturacion.proveedor}") String proveedor,
            @Value("${facturacion.transporte}") String transporte,
            @Value("${facturacion.simulacion.escenario}") String escenario) {
        if (!"simulado".equals(transporte))
            throw new IllegalArgumentException("El transporte real aun no esta implementado; use simulado");
        var simulador = new TransporteSimulado(escenario);
        return switch (proveedor) {
            case "siat" -> new SiatAdapter(simulador);
            case "tercero" -> new TerceroAdapter(json, simulador);
            default -> throw new IllegalArgumentException("Proveedor invalido: " + proveedor);
        };
    }
    @Bean EmitirFactura emitirFactura(List<ReglaFactura> reglas, FacturaStore store,
            ProveedorFacturacion proveedor, Factura.Emisor emisor, Clock clock) {
        return new EmitirFactura(reglas, store, proveedor, emisor, clock);
    }
}
