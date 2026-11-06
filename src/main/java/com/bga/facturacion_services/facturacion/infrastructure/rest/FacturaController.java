package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facturas")
public class FacturaController {
    private final EmitirFactura facturas;
    public FacturaController(EmitirFactura facturas) { this.facturas = facturas; }

    @PostMapping
    public ResponseEntity<RegistroFactura> emitir(@RequestHeader("Idempotency-Key") String clave,
                                                  @Valid @RequestBody FacturaRequest solicitud) {
        var registro = facturas.emitir(clave, solicitud.toDomain());
        return ResponseEntity.created(URI.create("/api/v1/facturas/" + registro.factura().id())).body(registro);
    }
    @GetMapping("/{id}")
    public RegistroFactura consultar(@PathVariable UUID id) { return facturas.consultar(id); }

    @GetMapping("/{id}/solicitud")
    public ResponseEntity<String> solicitud(@PathVariable UUID id) {
        var resultado = facturas.consultar(id).emision();
        if (resultado.solicitud() == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(resultado.formato()))
                .body(resultado.solicitud());
    }
}
