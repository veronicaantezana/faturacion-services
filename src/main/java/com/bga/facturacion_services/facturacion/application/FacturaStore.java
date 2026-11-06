package com.bga.facturacion_services.facturacion.application;

import java.util.Optional;
import java.util.UUID;
import com.bga.facturacion_services.facturacion.domain.SolicitudFactura;

public interface FacturaStore {
    record Reserva(UUID id, long numero, boolean nueva) {}
    Reserva reservar(String clave, SolicitudFactura solicitud);
    void guardar(UUID id, RegistroFactura registro);
    Optional<RegistroFactura> buscar(UUID id);
}

