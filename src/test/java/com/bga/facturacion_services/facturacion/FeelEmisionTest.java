package com.bga.facturacion_services.facturacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FeelEmisionTest {
    static final AtomicReference<String> ULTIMO = new AtomicReference<>();
    static final AtomicReference<String> RESPUESTA = new AtomicReference<>("""
            {"respuesta":{"codRespuesta":"0","txtRespuesta":"OK"},"proceso":{"cuf":"CUFTEST","cufd":"CUFDTEST","idDocFiscalFEEL":"99","codEstado":"E","fueraLinea":false,"codigoRecepcion":908}}
            """);
    static final HttpServer SERVER = servidor();
    static final int PORT = SERVER.getAddress().getPort();

    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("feel.base-url", () -> "http://127.0.0.1:" + PORT);
        registro.add("feel.sincrono", () -> "true");
        registro.add("facturacion.proveedor", () -> "feel");
        registro.add("facturacion.emisor.codigo-sistema-origen", () -> "SIST");
        registro.add("facturacion.emisor.nit", () -> "1028421024");
        registro.add("spring.datasource.url", () -> "jdbc:h2:mem:feel-emision;DB_CLOSE_DELAY=-1");
    }

    @AfterAll
    static void apagar() { SERVER.stop(0); }

    @Test void emiteCompraVentaDelFrontendEnFeel() throws Exception {
        RESPUESTA.set("""
                {"respuesta":{"codRespuesta":"0","txtRespuesta":"OK"},"proceso":{"cuf":"CUFTEST","cufd":"CUFDTEST","idDocFiscalFEEL":"99","codEstado":"E","fueraLinea":false,"codigoRecepcion":908}}
                """);
        mvc.perform(post("/api/v1/facturacion/facturas/emitir").contentType(MediaType.APPLICATION_JSON).content(cuerpo("CLIENTE DE PRUEBA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuf").value("CUFTEST"))
                .andExpect(jsonPath("$.cufd").value("CUFDTEST"))
                .andExpect(jsonPath("$.estado").value("VALIDADA"))
                .andExpect(jsonPath("$.codigoDocumentoSector").value(1))
                .andExpect(jsonPath("$.montoTotal").value(93.0))
                .andExpect(jsonPath("$.codigoSucursal").value(0));
        var enviado = new ObjectMapper().readTree(ULTIMO.get());
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("codigoDocumentoSector").asInt()).isEqualTo(1);
        assertThat(enviado.get("facturaEntidadFinanciera").isNull()).isTrue();
        assertThat(enviado.get("facturaAlquiler").isNull()).isTrue();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("numeroFactura").isNumber()).isTrue();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("codigoSucursal").isNumber()).isTrue();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("tipoCambio").isNumber()).isTrue();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("cuf").asText()).isEmpty();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("cufd").asText()).isEmpty();
        assertThat(enviado.get("facturaCompraVenta").get("cabecera").get("leyenda").asText()).isEmpty();
        assertThat(enviado.get("cufd").asText()).isEmpty();
        assertThat(enviado.get("codigoSistemaOrigen").asText()).isEqualTo("SIST");
        assertThat(enviado.get("emailCliente").asText()).isEqualTo("a@b.com");
        assertThat(enviado.get("telefonoCliente").isNull()).isTrue();
        assertThat(enviado.get("sincrono").asBoolean()).isTrue();
        assertThat(enviado.get("idLoteERP").asText()).isEqualTo("0");
        assertThat(enviado.has("montoTotalArrendamientoFinanciero")).isFalse();
    }

    @Test void rechazoDeFeelLlegaAlFrontend() throws Exception {
        RESPUESTA.set("""
                {"respuesta":{"codRespuesta":"1","txtRespuesta":"NIT no habilitado"},"proceso":null}
                """);
        mvc.perform(post("/api/v1/facturacion/facturas/emitir").contentType(MediaType.APPLICATION_JSON).content(cuerpo("OTRO CLIENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDADA"));
        assertThat(ULTIMO.get()).contains("\"facturaCompraVenta\"");
    }

    private String cuerpo(String cliente) {
        return """
                {"codigoSucursal":0,"codigoPuntoVenta":0,"codigoTipoDocumentoIdentidad":1,
                "numeroDocumento":"1234567","nombreRazonSocial":"%s","correoCliente":"a@b.com",
                "codigoMetodoPago":1,"codigoMoneda":1,"tipoCambio":1,"descuentoAdicional":2,
                "usuario":"admin","codigoDocumentoSector":1,"items":[{"codigoProducto":"PROD-001",
                "descripcion":"Producto de demostracion","actividadEconomica":"620000","codigoProductoSin":99100,
                "unidadMedida":58,"cantidad":2,"precioUnitario":50.00,"montoDescuento":5.00}]}
                """.formatted(cliente);
    }

    private static HttpServer servidor() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ws-send-docfiscal/rest/ws-send-docfiscal/enviarsync", exchange -> {
                ULTIMO.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] body = RESPUESTA.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
