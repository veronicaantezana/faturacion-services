package com.bga.facturacion_services.facturacion.application;

import com.bga.facturacion_services.facturacion.domain.Factura;

/** Alta en Oracle del canal web. Sin URL configurada no hace nada. */
public interface AltaFacturaWeb {
    Resultado registrar(Factura factura);

    default void guardarRespuestaFeel(Resultado alta, ResultadoEmision emision) {}

    record Resultado(long idComplemento, long nroSolicitud) {}

    class Rechazo extends RuntimeException {
        public Rechazo(String mensaje) { super(mensaje); }
    }
}
