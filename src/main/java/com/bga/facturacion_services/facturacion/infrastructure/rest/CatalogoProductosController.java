package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.infrastructure.provider.CatalogoProductosFeel;
import com.bga.facturacion_services.facturacion.infrastructure.provider.CatalogoProductosFeel.ProductoCatalogo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion/productos")
public class CatalogoProductosController {
    private final CatalogoProductosFeel catalogo;

    public CatalogoProductosController(CatalogoProductosFeel catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<ProductoCatalogo> listar() {
        return catalogo.listar();
    }
}
