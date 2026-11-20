package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.MapeoParametrosFeel;
import com.bga.facturacion_services.facturacion.application.RegistrarEmpresa;
import com.bga.facturacion_services.facturacion.infrastructure.persistence.ProductoOracle;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion/empresas")
public class EmpresaController {
    private final RegistrarEmpresa empresas;
    private final ProductoOracle productos;
    private final MapeoParametrosFeel mapeo;

    public EmpresaController(RegistrarEmpresa empresas, ProductoOracle productos, MapeoParametrosFeel mapeo) {
        this.empresas = empresas;
        this.productos = productos;
        this.mapeo = mapeo;
    }

    @GetMapping("/{idEmpresa}/metodos-pago")
    public List<MapeoParametrosFeel.MetodoPagoMapeado> metodosPago(@PathVariable long idEmpresa) {
        return mapeo.listarMetodosPago(idEmpresa);
    }

    @GetMapping("/{idEmpresa}/productos")
    public List<ProductoEmpresaResponse> productos(@PathVariable long idEmpresa) {
        return productos.listarPorEmpresa(idEmpresa);
    }

    @GetMapping("/{idEmpresa}/productos-sin")
    public List<ProductoSinResponse> productosSin(@PathVariable long idEmpresa) {
        return productos.listarSinPorEmpresa(idEmpresa);
    }

    @PostMapping("/{idEmpresa}/productos")
    public ResponseEntity<ProductoEmpresaResponse> registrarProducto(
            @PathVariable long idEmpresa,
            @RequestParam long idProductoSin,
            @RequestHeader(value = "X-Usuario", required = false) String usuario) {
        ProductoEmpresaResponse creado = productos.registrar(idEmpresa, idProductoSin, usuario);
        return ResponseEntity.created(URI.create(
                "/api/v1/facturacion/empresas/" + idEmpresa + "/productos/" + creado.id())).body(creado);
    }

    @GetMapping
    public List<EmpresaResponse> listar(@RequestParam(value = "actividades", required = false) String actividades) {
        if (actividades == null || actividades.isBlank()) return empresas.listar();
        return empresas.listarPorActividades(actividades);
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> registrar(
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @Valid @RequestBody RegistrarEmpresaRequest solicitud) {
        EmpresaResponse creada = empresas.registrar(solicitud, usuario);
        return ResponseEntity.created(URI.create("/api/v1/facturacion/empresas/" + creada.idEmpresa())).body(creada);
    }
}
