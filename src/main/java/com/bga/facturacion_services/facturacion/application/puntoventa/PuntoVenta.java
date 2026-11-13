package com.bga.facturacion_services.facturacion.application.puntoventa;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PuntoVenta(
        long idPos,
        long idConfig,
        Long idEmpresa,
        String nombreEmpresa,
        String nitEmpresa,
        String codigoSucursal,
        String referenciaSucursal,
        Integer sucursalSin,
        String descripcion,
        String direccionIp,
        int pos,
        Integer tipoPuntoVenta,
        String nombrePuntoVenta,
        Integer tipoPing,
        Integer enabled,
        String lastUser,
        OffsetDateTime lastStamp,
        String numeroContrato,
        String nitComisionista,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer obtenerCufd,
        Integer estadoSiat) {}
