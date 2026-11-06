package com.bga.facturacion_services.facturacion.application;

public record ResultadoEmision(Estado estado, String proveedor, boolean simulado,
                               String referencia, String mensaje, String formato, String solicitud,
                               String cuf, String cufd) {
    public enum Estado { PREPARADA, ACEPTADA_SIMULADA, RECHAZADA_SIMULADA, PENDIENTE_CONFIRMACION, ACEPTADA, RECHAZADA, CONTINGENCIA }
}

