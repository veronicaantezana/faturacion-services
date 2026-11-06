package com.bga.facturacion_services.facturacion.domain;

import java.util.UUID;
import java.time.OffsetDateTime;

public interface ReglaFactura {
    String tipo();
    Factura construir(UUID id, long numero, OffsetDateTime fecha, Factura.Emisor emisor, SolicitudFactura solicitud);
}

