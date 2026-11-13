package com.bga.facturacion_services.facturacion.infrastructure.rest;

import java.time.OffsetDateTime;

public record EmpresaResponse(
        Long idEmpresa,
        String nit,
        String nombre,
        String estado,
        Integer modalidad,
        String codigoAutorizacion,
        String keyName,
        String codigoSistema,
        Integer adminSequence,
        Integer adminSeqNota,
        String lastUser,
        OffsetDateTime lastStamp,
        Integer enviarCorreo,
        Integer completarDatos,
        String urlDescarga,
        String destNotificacion,
        String docSectorValidate,
        Integer generarPdf,
        Integer tokenEnabled,
        Integer correoIndividual,
        String planVigente,
        String periodoPago,
        Integer maxFacturas,
        Integer conciliacion,
        String nombreComercial,
        Integer funcionalida,
        Integer generarPdfCodSistema) {}
