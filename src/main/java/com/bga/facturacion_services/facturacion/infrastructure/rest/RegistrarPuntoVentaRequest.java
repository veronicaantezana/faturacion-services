package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.puntoventa.NuevoPuntoVenta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegistrarPuntoVentaRequest(
        @NotNull Long idConfig,
        @NotBlank @Size(max = 200) String descripcion,
        @Size(max = 50) String direccionIp,
        @NotNull @Min(0) Integer pos,
        @NotNull Integer tipoPuntoVenta,
        @NotBlank @Size(max = 100) String nombrePuntoVenta,
        @NotNull Integer tipoPing,
        @NotNull Integer enabled,
        @Size(max = 50) String numeroContrato,
        @Size(max = 20) String nitComisionista,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer obtenerCufd,
        Integer estadoSiat) {

    public NuevoPuntoVenta aComando() {
        return new NuevoPuntoVenta(idConfig, descripcion, direccionIp, pos, tipoPuntoVenta, nombrePuntoVenta,
                tipoPing, enabled, numeroContrato, nitComisionista, fechaInicio, fechaFin, obtenerCufd, estadoSiat);
    }
}
