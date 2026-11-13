package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.sucursal.NuevaSucursal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarSucursalRequest(
        @NotNull Long idEmpresa,
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 200) String referencia,
        @NotNull @Min(0) Integer sucursal,
        @NotNull @Min(0) Integer pos,
        @NotBlank @Size(max = 1000) String direccion,
        @NotNull @Min(0) Long nroDocSequence,
        @NotNull @Min(0) Long nroNotaSequence,
        @NotNull Integer enabled,
        Integer horaCufd,
        @Size(max = 50) String telefono,
        @Size(max = 100) String municipio,
        @Size(max = 100) String keyName) {

    public NuevaSucursal aComando() {
        return new NuevaSucursal(idEmpresa, codigo, referencia, sucursal, pos, direccion,
                nroDocSequence, nroNotaSequence, enabled, horaCufd, telefono, municipio, keyName);
    }
}
