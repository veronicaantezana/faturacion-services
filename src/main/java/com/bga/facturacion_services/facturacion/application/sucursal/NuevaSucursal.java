package com.bga.facturacion_services.facturacion.application.sucursal;

public record NuevaSucursal(
        long idEmpresa,
        String codigo,
        String referencia,
        int sucursal,
        int pos,
        String direccion,
        Long nroDocSequence,
        Long nroNotaSequence,
        int enabled,
        Integer horaCufd,
        String telefono,
        String municipio,
        String keyName) {}
