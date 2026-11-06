package com.bga.facturacion_services.facturacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FacturaApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    String ejemplo() throws Exception {
        try (var in = new ClassPathResource("demo/compra-venta.json").getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    @Test void emiteConsultaYRepiteSinDuplicar() throws Exception {
        var clave = UUID.randomUUID().toString();
        var primero = mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(ejemplo())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.factura.total").value(93.0))
                .andExpect(jsonPath("$.emision.simulado").value(true)).andReturn().getResponse().getContentAsString();
        var id = json.readTree(primero).at("/factura/id").asText();
        var segundo = mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(ejemplo())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(segundo).at("/factura/id").asText()).isEqualTo(id);
        mvc.perform(get("/api/v1/facturas/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/facturas/" + id + "/solicitud")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        var cambiado = json.readTree(ejemplo());
        ((com.fasterxml.jackson.databind.node.ObjectNode)cambiado).put("usuario", "otro");
        mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cambiado)))
                .andExpect(status().isConflict());
    }
    @Test void validaEntradaYFacturaInexistente() throws Exception {
        mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/facturas/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/facturas").contentType(MediaType.APPLICATION_JSON).content(ejemplo()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", "invalida")
                .contentType(MediaType.APPLICATION_JSON).content(ejemplo().replace("COMPRA_VENTA", "OTRA")))
                .andExpect(status().isBadRequest());
    }
    @Test void demoUtilizaElMismoFlujo() throws Exception {
        mvc.perform(post("/api/v1/demo/facturas/compra-venta").header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.factura.total").value(93.0));
    }
}
