package com.bga.facturacion_services.facturacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"facturacion.proveedor=tercero", "facturacion.mock-datos=false",
        "spring.datasource.url=jdbc:h2:mem:tercero-tests;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class TerceroSinDemoTest {
    @Autowired MockMvc mvc;
    @Test void aceptaDatosFrontendSinDependerDelMockDeDatos() throws Exception {
        byte[] datos;
        try (var in = new ClassPathResource("demo/compra-venta.json").getInputStream()) { datos = in.readAllBytes(); }
        mvc.perform(post("/api/v1/facturas").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(datos)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.emision.proveedor").value("tercero"))
                .andExpect(jsonPath("$.emision.formato").value("application/json"));
        mvc.perform(get("/api/v1/demo/facturas/compra-venta")).andExpect(status().isNotFound());
    }
}
