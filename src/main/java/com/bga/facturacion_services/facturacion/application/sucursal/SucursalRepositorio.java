package com.bga.facturacion_services.facturacion.application.sucursal;

import java.util.List;

public interface SucursalRepositorio {
    List<Sucursal> listar();

    boolean existeEmpresa(long idEmpresa);

    boolean existePunto(long idEmpresa, int sucursal, int pos);

    Sucursal insertar(NuevaSucursal sucursal, String usuario);
}
