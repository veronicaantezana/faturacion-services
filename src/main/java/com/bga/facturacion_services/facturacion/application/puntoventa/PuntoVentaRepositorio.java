package com.bga.facturacion_services.facturacion.application.puntoventa;

import java.util.List;

public interface PuntoVentaRepositorio {
    List<PuntoVenta> listar();

    SucursalVinculo buscarSucursal(long idConfig);

    boolean existeParaConfig(long idConfig);

    PuntoVenta insertar(NuevoPuntoVenta punto, String usuario);

    record SucursalVinculo(long idConfig, int sucursal, int pos) {}
}
