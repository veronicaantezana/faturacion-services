package com.bga.facturacion_services.facturacion.application;

import java.util.List;

/** Codigos que FEEL tiene mapeados para una empresa en FACTURACION.F_MAPEO_PARAM. */
public interface MapeoParametrosFeel {
    Codigos consultar(long idEmpresa, int tipoDocumento, int metodoPago, int moneda);

    default List<MetodoPagoMapeado> listarMetodosPago(long idEmpresa) {
        return List.of();
    }

    record Codigos(String tipoDocumento, String metodoPago, String moneda, String codigoSistema) {}

    record MetodoPagoMapeado(int codigo, String descripcion) {}
}
