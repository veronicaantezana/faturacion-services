package com.bga.facturacion_services.facturacion.application.feel;

import com.bga.facturacion_services.facturacion.domain.Factura;
import java.util.Map;

/** Puente hexagonal: traduce una factura de dominio al cuerpo JSON de un tipo de documento FEEL. */
public interface DocumentoFeel {
    int sector();
    String claveSobre();
    boolean admite(Factura factura);
    Map<String, Object> armar(Factura factura);
}
