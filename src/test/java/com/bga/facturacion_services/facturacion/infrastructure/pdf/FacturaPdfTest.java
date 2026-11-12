package com.bga.facturacion_services.facturacion.infrastructure.pdf;

import com.bga.facturacion_services.facturacion.application.RegistroFactura;
import com.bga.facturacion_services.facturacion.application.ResultadoEmision;
import com.bga.facturacion_services.facturacion.domain.CompraVenta;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.domain.SolicitudFactura;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FacturaPdfTest {
    @Test
    void armaElPdfConLosDatosDeLaFacturaGuardada() {
        var factura = new CompraVenta().construir(UUID.randomUUID(), 101, OffsetDateTime.parse("2021-12-01T13:45:00-04:00"),
                new Factura.Emisor("123456012", "Compania Cervecera SAO", "La Paz", "Calle Buganis Nro.78 Zona Central", 0, 0, "0"),
                new SolicitudFactura("COMPRA_VENTA", new Factura.Cliente(5, "9876548020", null, "PRUEBA"),
                        1, "cajero", List.of(new Factura.Item("PT-001-02S", "99100", "110000", "Cerveza Pale Ale",
                        58, new BigDecimal("6.00"), new BigDecimal("9.62"), BigDecimal.ZERO)), null));
        var emision = new ResultadoEmision(ResultadoEmision.Estado.ACEPTADA, "feel", false, "101", "ok",
                "application/json", "{}", "13FEDFAC1094D2BE", "CUFD");

        byte[] pdf = FacturaPdf.generar(new RegistroFactura(factura, emision));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(2000);
        assertThat(FacturaPdf.enLetras(220)).isEqualTo("doscientos veinte");
    }
}
