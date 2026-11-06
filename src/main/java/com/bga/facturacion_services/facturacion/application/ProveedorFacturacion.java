package com.bga.facturacion_services.facturacion.application;

import com.bga.facturacion_services.facturacion.domain.Factura;

public interface ProveedorFacturacion {
    String codigo();
    boolean simulado();
    ResultadoEmision emitir(Factura factura);
}

