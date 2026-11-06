package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.application.*;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Provisional JSON contract: replace these DTOs and mapping when the provider supplies its API. */
public final class TerceroAdapter implements ProveedorFacturacion {
    private final ObjectMapper json;
    private final TransporteSimulado transporte;
    public TerceroAdapter(ObjectMapper json, TransporteSimulado transporte) { this.json = json; this.transporte = transporte; }
    public String codigo() { return "tercero"; }
    public boolean simulado() { return true; }
    public ResultadoEmision emitir(Factura f) {
        if (!"COMPRA_VENTA".equals(f.tipo())) throw new IllegalArgumentException("Tipo no soportado por tercero");
        var solicitud = new SolicitudTercero(f.id().toString(), "PURCHASE_SALE", f.numero(), f.fecha().toString(),
                new EmisorTercero(f.emisor().nit(), f.emisor().razonSocial(), f.emisor().municipio(),
                        f.emisor().direccion(), f.emisor().sucursal(), f.emisor().puntoVenta()),
                new ClienteTercero(f.cliente().tipoDocumento(), f.cliente().documento(),
                        f.cliente().complemento(), f.cliente().razonSocial()),
                "BOB", f.metodoPago(), f.usuario(), f.descuentoAdicional(), f.total(), f.totalSujetoIva(),
                f.detalle().stream().map(l -> new ItemTercero(l.item().codigo(), l.item().descripcion(),
                        l.item().codigoSin(), l.item().actividad(), l.item().unidadMedida(),
                        l.item().cantidad(), l.item().precioUnitario(), l.item().descuento(), l.subtotal())).toList());
        try { return transporte.enviar(codigo(), f.id(), "application/json", json.writeValueAsString(solicitud)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
    public record SolicitudTercero(String externalId, String documentType, long number, String issuedAt,
            EmisorTercero issuer, ClienteTercero customer, String currency, int paymentMethod, String operator,
            java.math.BigDecimal additionalDiscount, java.math.BigDecimal total,
            java.math.BigDecimal taxableAmount, List<ItemTercero> items) {}
    public record EmisorTercero(String taxId, String legalName, String city, String address, int branch, int pointOfSale) {}
    public record ClienteTercero(int documentType, String document, String complement, String legalName) {}
    public record ItemTercero(String sku, String description, String taxProductCode, String economicActivity,
            int unitCode, java.math.BigDecimal quantity,
            java.math.BigDecimal unitPrice, java.math.BigDecimal discount, java.math.BigDecimal subtotal) {}
}
