package com.bga.facturacion_services.facturacion.application.feel;

import com.bga.facturacion_services.facturacion.domain.Factura;
import java.util.List;

/** Selecciona el armador segun sector; extensible registrando nuevas implementaciones. */
public final class RegistroDocumentosFeel {
    private final List<DocumentoFeel> documentos;

    public RegistroDocumentosFeel(List<DocumentoFeel> documentos) {
        if (documentos == null || documentos.isEmpty())
            throw new IllegalArgumentException("Debe existir al menos un armador FEEL");
        this.documentos = List.copyOf(documentos);
    }

    public DocumentoFeel resolver(Factura factura) {
        return documentos.stream()
                .filter(d -> d.admite(factura))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay armador FEEL para el documento sector " + factura.codigoDocumentoSector()));
    }
}
