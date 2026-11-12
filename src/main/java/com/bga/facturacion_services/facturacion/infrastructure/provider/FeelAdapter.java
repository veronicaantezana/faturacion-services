package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.application.ProveedorFacturacion;
import com.bga.facturacion_services.facturacion.application.ResultadoEmision;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.infrastructure.provider.feel.ArmadorSobreFeel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class FeelAdapter implements ProveedorFacturacion {
    private final ObjectMapper json;
    private final FeelClienteHttp cliente;
    private final ArmadorSobreFeel armador;
    private final String codigoSistemaOrigen;
    private final int codigoTipoFactura;
    private final boolean sincrono;

    public FeelAdapter(ObjectMapper json, FeelClienteHttp cliente, ArmadorSobreFeel armador, String codigoSistemaOrigen,
                       int codigoTipoFactura, boolean sincrono) {
        this.json = json;
        this.cliente = cliente;
        this.armador = armador;
        this.codigoSistemaOrigen = codigoSistemaOrigen;
        this.codigoTipoFactura = codigoTipoFactura;
        this.sincrono = sincrono;
    }

    public String codigo() { return "feel"; }
    public boolean simulado() { return false; }

    public ResultadoEmision emitir(Factura factura) {
        var documento = armador.armar(factura, codigoSistemaOrigen, codigoTipoFactura, sincrono);
        String solicitud;
        try { solicitud = json.writeValueAsString(documento); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No se pudo armar el documento FEEL", ex); }
        try {
            var respuesta = cliente.enviar(documento);
            var estado = respuesta.fueraLinea() ? ResultadoEmision.Estado.CONTINGENCIA : ResultadoEmision.Estado.ACEPTADA;
            return new ResultadoEmision(estado, codigo(), false, respuesta.idDocFiscalFeel(), respuesta.mensaje(),
                    "application/json", solicitud, respuesta.cuf(), respuesta.cufd());
        } catch (FeelClienteHttp.FeelRechazoException ex) {
            return new ResultadoEmision(ResultadoEmision.Estado.RECHAZADA, codigo(), false, null, ex.getMessage(),
                    "application/json", solicitud, null, null);
        }
    }
}
