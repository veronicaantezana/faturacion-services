package com.bga.facturacion_services.facturacion.infrastructure.rest;

public record ProductoEmpresaResponse(
        long id,
        String codigoInterno,
        String descripcion,
        String codigoActividad,
        String descripcionActividad,
        long codigoProductoSin,
        boolean activo) {}
