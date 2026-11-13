package com.bga.facturacion_services.facturacion.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarEmpresaRequest(
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Size(max = 200) String nombre,
        @NotBlank @Size(max = 5) String estado,
        @jakarta.validation.constraints.NotNull Integer modalidad,
        @Size(max = 100) String codigoAutorizacion,
        @Size(max = 100) String keyName,
        @Size(max = 80) String codigoSistema,
        Integer enviarCorreo,
        Integer completarDatos,
        @Size(max = 300) String urlDescarga,
        @Size(max = 200) String destNotificacion,
        @Size(max = 50) String docSectorValidate,
        Integer generarPdf,
        Integer tokenEnabled,
        Integer correoIndividual,
        @Size(max = 50) String planVigente,
        @Size(max = 50) String periodoPago,
        Integer maxFacturas,
        Integer conciliacion,
        @Size(max = 200) String nombreComercial,
        Integer funcionalida,
        Integer generarPdfCodSistema,
        Integer adminSequence,
        Integer adminSeqNota) {}
