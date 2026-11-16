package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.RegistrarEmpresa;
import com.bga.facturacion_services.facturacion.infrastructure.persistence.ProductoOracle;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion/empresas")
public class EmpresaController {
    private final RegistrarEmpresa empresas;
    private final ProductoOracle productos;

    public EmpresaController(RegistrarEmpresa empresas, ProductoOracle productos) {
        this.empresas = empresas;
        this.productos = productos;
    }

    @GetMapping("/{idEmpresa}/productos")
    public List<ProductoEmpresaResponse> productos(@PathVariable long idEmpresa) {
        return productos.listarPorEmpresa(idEmpresa);
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
