package com.bga.facturacion_services.facturacion.application.puntoventa;

import java.time.LocalDate;

public record NuevoPuntoVenta(
        long idConfig,
        String descripcion,
        String direccionIp,
        int pos,
        int tipoPuntoVenta,
        String nombrePuntoVenta,
        int tipoPing,
        int enabled,
        String numeroContrato,
        String nitComisionista,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer obtenerCufd,
        Integer estadoSiat) {}
