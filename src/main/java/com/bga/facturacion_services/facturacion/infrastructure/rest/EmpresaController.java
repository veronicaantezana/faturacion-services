package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.RegistrarEmpresa;
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
@RequestMapping("/api/v1/facturacion/empresas")
public class EmpresaController {
    private final RegistrarEmpresa empresas;

    public EmpresaController(RegistrarEmpresa empresas) {
        this.empresas = empresas;
    }

    @GetMapping
    public List<EmpresaResponse> listar() {
        return empresas.listar();
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> registrar(
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @Valid @RequestBody RegistrarEmpresaRequest solicitud) {
        EmpresaResponse creada = empresas.registrar(solicitud, usuario);
        return ResponseEntity.created(URI.create("/api/v1/facturacion/empresas/" + creada.idEmpresa())).body(creada);
    }
}
