package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import com.bga.facturacion_services.facturacion.application.feel.DocumentoFeel;
import com.bga.facturacion_services.facturacion.application.feel.RegistroDocumentosFeel;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.domain.FeelMetadata;
import java.util.LinkedHashMap;
import java.util.Map;

/** Arma el sobre completo: todas las claves nulas salvo el documento seleccionado. */
public final class ArmadorSobreFeel {
    private final RegistroDocumentosFeel registro;

    public ArmadorSobreFeel(RegistroDocumentosFeel registro) {
        this.registro = registro;
    }

    public Map<String, Object> armar(Factura factura, String codigoSistemaOrigen, int codigoTipoFacturaDefecto, boolean sincrono) {
        if (codigoSistemaOrigen == null || codigoSistemaOrigen.isBlank())
            throw new IllegalArgumentException("Falta configurar facturacion.emisor.codigo-sistema-origen");
        DocumentoFeel documento = registro.resolver(factura);
        FeelMetadata meta = factura.feel();
        var sobre = new LinkedHashMap<String, Object>();
        for (String clave : FeelSobreCatalogo.CLAVES) sobre.put(clave, null);
        sobre.put(documento.claveSobre(), documento.armar(factura));
        sobre.put("idDocFiscalERP", meta.idDocFiscalErp(factura.numero()));
        sobre.put("cufd", "");
        sobre.put("contingencia", false);
        sobre.put("esLote", meta.esLote());
        sobre.put("emailCliente", FeelConversiones.vacio(factura.venta().correo()));
        sobre.put("telefonoCliente", null);
        sobre.put("idLoteERP", meta.idLoteErp() == null ? "0" : meta.idLoteErp());
        sobre.put("ultFacturaLote", meta.ultFacturaLote());
        sobre.put("codigoSistemaOrigen", codigoSistemaOrigen.trim());
        sobre.put("codigoTipoFactura", meta.codigoTipoFactura(codigoTipoFacturaDefecto));
        sobre.put("fueraLinea", null);
        sobre.put("codigoControl", null);
        sobre.put("sincrono", sincrono);
        sobre.put("eventoId", meta.eventoId());
        sobre.put("pruebaDeConexion", null);
        sobre.put("typeTasaCero", null);
        sobre.put("firmado", null);
        sobre.put("xmlFirmado", null);
        sobre.put("esPaquete", null);
        sobre.put("cantidadPaquete", null);
        sobre.put("codigoPaquete", null);
        sobre.put("informacionAdicional", null);
        sobre.put("numeracion", null);
        return sobre;
    }
}
