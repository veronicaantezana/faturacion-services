package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.sucursal.RegistrarSucursal;
import com.bga.facturacion_services.facturacion.application.sucursal.Sucursal;
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
@RequestMapping("/api/v1/facturacion/sucursales")
public class SucursalController {
    private final RegistrarSucursal sucursales;

    public SucursalController(RegistrarSucursal sucursales) {
        this.sucursales = sucursales;
    }

    @GetMapping
    public List<Sucursal> listar() {
        return sucursales.listar();
    }

    @PostMapping
    public ResponseEntity<Sucursal> registrar(
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @Valid @RequestBody RegistrarSucursalRequest solicitud) {
        Sucursal creada = sucursales.registrar(solicitud.aComando(), usuario);
        return ResponseEntity.created(URI.create("/api/v1/facturacion/sucursales/" + creada.idConfig())).body(creada);
    }
}
