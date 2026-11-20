package com.bga.facturacion_services.facturacion.infrastructure.config;

import com.bga.facturacion_services.facturacion.application.*;
import com.bga.facturacion_services.facturacion.domain.*;
import com.bga.facturacion_services.facturacion.application.feel.DocumentoFeel;
import com.bga.facturacion_services.facturacion.application.feel.RegistroDocumentosFeel;
import com.bga.facturacion_services.facturacion.infrastructure.provider.*;
import com.bga.facturacion_services.facturacion.infrastructure.provider.feel.ArmadorSobreFeel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.*;
import java.util.List;

@Configuration
public class FacturacionConfiguration {
    @Bean ReglaFactura compraVenta() { return new CompraVenta(); }
    @Bean ReglaFactura notaMonedaExtranjera() { return new NotaMonedaExtranjera(); }
    @Bean RegistroDocumentosFeel registroFeel(List<DocumentoFeel> documentos) { return new RegistroDocumentosFeel(documentos); }
    @Bean ArmadorSobreFeel armadorFeel(RegistroDocumentosFeel registro) { return new ArmadorSobreFeel(registro); }
    @Bean Clock reloj() { return Clock.system(ZoneId.of("America/La_Paz")); }
    @Bean Factura.Emisor emisor(
            @Value("${facturacion.emisor.nit}") String nit,
            @Value("${facturacion.emisor.razon-social}") String nombre,
            @Value("${facturacion.emisor.municipio}") String municipio,
            @Value("${facturacion.emisor.direccion}") String direccion,
            @Value("${facturacion.emisor.sucursal}") int sucursal,
            @Value("${facturacion.emisor.punto-venta}") int punto,
            @Value("${facturacion.emisor.telefono:}") String telefono) {
        return new Factura.Emisor(nit, nombre, municipio, direccion, sucursal, punto, telefono);
    }
    @Bean CatalogoProductosFeel catalogoProductosFeel(RestClient feelRestClient, ObjectMapper json,
            @Value("${feel.base-url:}") String feelUrl,
            @Value("${facturacion.emisor.nit}") String nit) {
        return new CatalogoProductosFeel(feelRestClient, json, feelUrl, nit);
    }
    @Bean RestClient feelRestClient() {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder().requestFactory(fabrica).build();
    }
    @Bean ProveedorFacturacion proveedor(ObjectMapper json, RestClient feelRestClient, ArmadorSobreFeel armador,
            @Value("${facturacion.proveedor}") String proveedor,
            @Value("${facturacion.transporte}") String transporte,
            @Value("${facturacion.simulacion.escenario}") String escenario,
            @Value("${feel.base-url:}") String feelUrl,
            @Value("${feel.sincrono:true}") boolean feelSincrono,
            @Value("${facturacion.emisor.codigo-sistema-origen:}") String codigoSistema,
            @Value("${facturacion.emisor.codigo-tipo-factura:1}") int codigoTipoFactura) {
        if ("feel".equals(proveedor))
            return new FeelAdapter(json, new FeelClienteHttp(feelRestClient, feelUrl, feelSincrono), armador,
                    codigoSistema, codigoTipoFactura, feelSincrono);
        if (!"simulado".equals(transporte))
            throw new IllegalArgumentException("El transporte real de SIAT aun no esta implementado; use simulado o el proveedor feel");
        var simulador = new TransporteSimulado(escenario);
        return switch (proveedor) {
            case "siat" -> new SiatAdapter(simulador);
            case "tercero" -> new TerceroAdapter(json, simulador);
            default -> throw new IllegalArgumentException("Proveedor invalido: " + proveedor);
        };
    }
    @Bean EmitirFactura emitirFactura(List<ReglaFactura> reglas, FacturaStore store,
            ProveedorFacturacion proveedor, Factura.Emisor emisor, Clock clock, AltaFacturaWeb altaOracle,
            MapeoParametrosFeel mapeo) {
        return new EmitirFactura(reglas, store, proveedor, emisor, clock, altaOracle, mapeo);
    }
}
