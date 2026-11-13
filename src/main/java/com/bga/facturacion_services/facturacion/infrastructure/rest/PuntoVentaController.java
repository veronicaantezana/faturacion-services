package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.puntoventa.PuntoVenta;
import com.bga.facturacion_services.facturacion.application.puntoventa.RegistrarPuntoVenta;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion/puntos-venta")
public class PuntoVentaController {
    private final RegistrarPuntoVenta puntos;

    public PuntoVentaController(RegistrarPuntoVenta puntos) {
        this.puntos = puntos;
    }

    @GetMapping
    public List<PuntoVenta> listar() {
        return puntos.listar();
    }

    @PostMapping
    public ResponseEntity<PuntoVenta> registrar(
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @Valid @RequestBody RegistrarPuntoVentaRequest solicitud) {
        PuntoVenta creado = puntos.registrar(solicitud.aComando(), usuario);
        return ResponseEntity.created(URI.create("/api/v1/facturacion/puntos-venta/" + creado.idPos())).body(creado);
    }
}
