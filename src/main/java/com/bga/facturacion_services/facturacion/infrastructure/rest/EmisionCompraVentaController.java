package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.EmitirFactura;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facturacion/facturas")
public class EmisionCompraVentaController {
    private final EmitirFactura facturas;

    public EmisionCompraVentaController(EmitirFactura facturas) { this.facturas = facturas; }

    @PostMapping("/emitir")
    public ResponseEntity<FacturaEmitidaResponse> emitir(@Valid @RequestBody EmitirCompraVentaRequest solicitud) {
        String clave = "fe" + UUID.randomUUID().toString().replace("-", "");
        var registro = facturas.emitir(clave, solicitud.toDomain());
        return ResponseEntity.created(URI.create("/api/v1/facturacion/facturas/" + registro.factura().numero()))
                .body(FacturaEmitidaResponse.desde(registro));
    }

    public static final class EmisionNoConfirmada extends RuntimeException {
        public EmisionNoConfirmada(String mensaje) { super(mensaje); }
    }
}
