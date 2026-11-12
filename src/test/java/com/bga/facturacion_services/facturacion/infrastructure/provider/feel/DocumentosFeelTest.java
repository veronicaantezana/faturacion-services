package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import com.bga.facturacion_services.facturacion.application.feel.RegistroDocumentosFeel;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.domain.FeelMetadata;
import com.bga.facturacion_services.facturacion.domain.NotaMonedaExtranjera;
import com.bga.facturacion_services.facturacion.domain.SolicitudFactura;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosFeelTest {
    @Test
    void compraVentaUsaFacturaCompraVenta() throws Exception {
        var factura = facturaSector1();
        var armador = new ArmadorSobreFeel(new RegistroDocumentosFeel(
                List.of(new FeelCompraVentaDocumento(), new FeelNotaMonedaExtranjeraDocumento())));
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(
                armador.armar(factura, "SIST", 1, false)));
        assertThat(json.get("facturaCompraVenta").isNull()).isFalse();
        assertThat(json.get("notaMonedaExtranjera").isNull()).isTrue();
        assertThat(json.get("codigoTipoFactura").asInt()).isEqualTo(1);
    }

    @Test
    void monedaExtranjeraUsaNotaMonedaExtranjeraComoEnFeel() throws Exception {
        var solicitud = new SolicitudFactura("NOTA_MONEDA_EXTRANJERA",
                new Factura.Cliente(1, "3530204", "", "AGUIRRE,VERONICA"), 7, "WCA",
                List.of(new Factura.Item("1003962", "1003962", null, "COMPRA ME", 58,
                        new BigDecimal("8420"), new BigDecimal("12.80618"), null)),
                null, 0, 0, null, null, 2, new BigDecimal("12.80618"), null, 1, null, 9,
                FeelMetadata.notaMonedaExtranjera("19120709", true, "523842",
                        new BigDecimal("0.4"), 2, new BigDecimal("13.20225"), "1112"),
                BigDecimal.ZERO.setScale(2));
        var factura = new NotaMonedaExtranjera().construir(UUID.randomUUID(), 99,
                OffsetDateTime.parse("2026-10-22T10:10:10.153-04:00"),
                new Factura.Emisor("1028421024", "BANCO GANADERO S.A.", "SANTA CRUZ", "Dir", 0, 0), solicitud);
        var armador = new ArmadorSobreFeel(new RegistroDocumentosFeel(
                List.of(new FeelCompraVentaDocumento(), new FeelNotaMonedaExtranjeraDocumento())));
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(
                armador.armar(factura, "SIST", 1, false)));
        assertThat(json.get("notaMonedaExtranjera").get("cabecera").get("codigoDocumentoSector").asInt()).isEqualTo(9);
        assertThat(json.get("notaMonedaExtranjera").get("cabecera").get("codigoTipoDocumentoIdentidad").asText()).isEqualTo("CI");
        assertThat(json.get("notaMonedaExtranjera").get("cabecera").get("codigoMoneda").asText()).isEqualTo("1112");
        assertThat(json.get("facturaCompraVenta").isNull()).isTrue();
        assertThat(json.get("idDocFiscalERP").asText()).isEqualTo("19120709");
        assertThat(json.get("esLote").asBoolean()).isTrue();
        assertThat(json.get("codigoTipoFactura").asInt()).isEqualTo(2);
    }

    private Factura facturaSector1() {
        var solicitud = new SolicitudFactura("COMPRA_VENTA", new Factura.Cliente(1, "123", null, "Cliente"),
                1, "admin", List.of(new Factura.Item("P1", "99100", "620000", "Producto", 58,
                BigDecimal.ONE, BigDecimal.TEN, null)), null,
                null, null, null, null, null, null, null, null, null, 1,
                FeelMetadata.compraVenta(), null);
        return new com.bga.facturacion_services.facturacion.domain.CompraVenta().construir(
                UUID.randomUUID(), 1, OffsetDateTime.parse("2026-11-05T12:00:00-04:00"),
                new Factura.Emisor("123", "EMPRESA", "La Paz", "Dir", 0, 0), solicitud);
    }
}
