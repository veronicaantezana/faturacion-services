package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.application.ResultadoEmision;
import java.util.UUID;

public final class TransporteSimulado {
    private final String escenario;
    public TransporteSimulado(String escenario) {
        if (!java.util.Set.of("aceptar", "rechazar", "timeout").contains(escenario))
            throw new IllegalArgumentException("Escenario de simulacion invalido");
        this.escenario = escenario;
    }
    public ResultadoEmision enviar(String proveedor, UUID id, String formato, String solicitud) {
        var estado = switch (escenario) {
            case "rechazar" -> ResultadoEmision.Estado.RECHAZADA_SIMULADA;
            case "timeout" -> ResultadoEmision.Estado.PENDIENTE_CONFIRMACION;
            default -> ResultadoEmision.Estado.ACEPTADA_SIMULADA;
        };
        return new ResultadoEmision(estado, proveedor, true, "SIM-" + id,
                "Simulacion local: " + escenario + ". Sin validez fiscal.", formato, solicitud, null, null);
    }
}
