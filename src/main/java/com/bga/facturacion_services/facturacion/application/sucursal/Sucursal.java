package com.bga.facturacion_services.facturacion.application.sucursal;

import java.time.OffsetDateTime;

public record Sucursal(
        long idConfig,
        long idEmpresa,
        String nombreEmpresa,
        String nitEmpresa,
        String codigo,
        String referencia,
        int sucursal,
        int pos,
        String direccion,
        Long nroDocSequence,
        Long nroNotaSequence,
        Integer enabled,
        String lastUser,
        OffsetDateTime lastStamp,
        Integer horaCufd,
        String telefono,
        String municipio,
        String keyName) {}
