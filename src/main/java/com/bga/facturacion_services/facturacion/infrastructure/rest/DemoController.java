package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/demo/facturas")
@ConditionalOnProperty(name = "facturacion.mock-datos", havingValue = "true")
public class DemoController {
    private final EmitirFactura facturas;
    private final FacturaRequest ejemplo;
    public DemoController(EmitirFactura facturas, ObjectMapper json, Validator validator) throws IOException {
        this.facturas = facturas;
        try (var in = new ClassPathResource("demo/compra-venta.json").getInputStream()) {
            this.ejemplo = json.readValue(in, FacturaRequest.class);
        }
        if (!validator.validate(ejemplo).isEmpty()) throw new IllegalStateException("Datos demo invalidos");
    }
    @GetMapping("/compra-venta")
    public FacturaRequest ejemplo() { return ejemplo; }
    @PostMapping("/compra-venta")
    public RegistroFactura emitir(@RequestHeader("Idempotency-Key") String clave) {
        return facturas.emitir(clave, ejemplo.toDomain());
    }
}
