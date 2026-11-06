package com.bga.facturacion_services.facturacion;

import com.bga.facturacion_services.facturacion.domain.*;
import com.bga.facturacion_services.facturacion.infrastructure.provider.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CompraVentaTest {
    Factura crear(BigDecimal cantidad, BigDecimal precio, BigDecimal descuento, BigDecimal adicional) {
        return new CompraVenta().construir(UUID.randomUUID(), 1, OffsetDateTime.parse("2026-11-05T12:00:00-04:00"),
                new Factura.Emisor("123", "EMPRESA & HIJOS", "La Paz", "Direccion", 0, 0),
                new SolicitudFactura("COMPRA_VENTA", new Factura.Cliente(1, "123", null, "Ana <Perez>"),
                        1, "usuario", List.of(new Factura.Item("P1", "99100", "620000", "Producto & servicio",
                        58, cantidad, precio, descuento)), adicional));
    }
    @Test void calculaDescuentosYRedondeaPorLinea() {
        var factura = crear(new BigDecimal("2.125"), new BigDecimal("10.00"), new BigDecimal("1.00"), new BigDecimal("0.25"));
        assertThat(factura.detalle().get(0).subtotal()).isEqualByComparingTo("20.25");
        assertThat(factura.total()).isEqualByComparingTo("20.00");
        assertThat(factura.totalSujetoIva()).isEqualByComparingTo(factura.total());
    }
    @Test void rechazaCantidadesYDescuentosInvalidos() {
        assertThatThrownBy(() -> crear(BigDecimal.ZERO, BigDecimal.TEN, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> crear(BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("11"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> crear(BigDecimal.ONE, BigDecimal.TEN, null, BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> crear(BigDecimal.ONE, new BigDecimal("1.001"), null, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void ambosAdaptadoresMapeanLaMismaFacturaSinModificarla() throws Exception {
        var factura = crear(BigDecimal.ONE, BigDecimal.TEN, null, null);
        var transporte = new TransporteSimulado("aceptar");
        var siat = new SiatAdapter(transporte).emitir(factura);
        var tercero = new TerceroAdapter(new ObjectMapper(), transporte).emitir(factura);
        var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(siat.solicitud().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(doc.getElementsByTagName("razonSocialEmisor").item(0).getTextContent()).isEqualTo("EMPRESA & HIJOS");
        assertThat(doc.getElementsByTagName("montoTotal").item(0).getTextContent()).isEqualTo("10.00");
        assertThat(new ObjectMapper().readTree(tercero.solicitud()).get("total").decimalValue()).isEqualByComparingTo("10");
        assertThat(siat.simulado()).isTrue();
        assertThat(tercero.proveedor()).isEqualTo("tercero");
    }
    @Test void escenariosNoConfundenTimeoutConRechazo() {
        var f = crear(BigDecimal.ONE, BigDecimal.TEN, null, null);
        assertThat(new SiatAdapter(new TransporteSimulado("rechazar")).emitir(f).estado().name()).isEqualTo("RECHAZADA_SIMULADA");
        assertThat(new SiatAdapter(new TransporteSimulado("timeout")).emitir(f).estado().name()).isEqualTo("PENDIENTE_CONFIRMACION");
    }
}
