package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import com.bga.facturacion_services.facturacion.application.feel.DocumentoFeel;
import com.bga.facturacion_services.facturacion.domain.Factura;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class FeelCompraVentaDocumento implements DocumentoFeel {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public int sector() { return 1; }
    public String claveSobre() { return "facturaCompraVenta"; }

    public boolean admite(Factura factura) {
        return factura.codigoDocumentoSector() == 1;
    }

    public Map<String, Object> armar(Factura factura) {
        long nit = FeelConversiones.nit(factura.emisor().nit());
        var venta = factura.venta();
        var cabecera = new LinkedHashMap<String, Object>();
        cabecera.put("nitEmisor", nit);
        cabecera.put("razonSocialEmisor", factura.emisor().razonSocial());
        cabecera.put("municipio", factura.emisor().municipio());
        cabecera.put("telefono", FeelConversiones.vacio(factura.emisor().telefono()));
        cabecera.put("numeroFactura", factura.feel().numeroFacturaEnCabecera(factura.numero()));
        cabecera.put("cuf", "");
        cabecera.put("cufd", "");
        cabecera.put("codigoSucursal", factura.emisor().sucursal());
        cabecera.put("direccion", null);
        cabecera.put("codigoPuntoVenta", factura.emisor().puntoVenta());
        cabecera.put("fechaEmision", FECHA.format(factura.fecha()));
        cabecera.put("nombreRazonSocial", factura.cliente().razonSocial());
        cabecera.put("codigoTipoDocumentoIdentidad", FeelConversiones.tipoDocumentoIdentidad(factura.cliente().tipoDocumento()));
        cabecera.put("numeroDocumento", factura.cliente().documento());
        cabecera.put("complemento", FeelConversiones.vacio(factura.cliente().complemento()));
        cabecera.put("codigoCliente", factura.cliente().documento());
        cabecera.put("codigoMetodoPago", String.valueOf(factura.metodoPago()));
        cabecera.put("numeroTarjeta", FeelConversiones.tarjeta(venta.numeroTarjeta()));
        cabecera.put("montoTotal", factura.total());
        cabecera.put("montoTotalSujetoIva", factura.totalSujetoIva());
        cabecera.put("codigoMoneda", FeelConversiones.tipoMoneda(venta.codigoMoneda()));
        cabecera.put("tipoCambio", venta.tipoCambio());
        cabecera.put("montoTotalMoneda", venta.montoTotalMoneda());
        cabecera.put("montoGiftCard", FeelConversiones.ceroNulo(venta.montoGiftCard()));
        cabecera.put("descuentoAdicional", FeelConversiones.ceroNulo(factura.descuentoAdicional()));
        cabecera.put("codigoExcepcion", venta.codigoExcepcion() == null ? 0 : venta.codigoExcepcion());
        cabecera.put("cafc", FeelConversiones.vacio(venta.cafc()));
        cabecera.put("leyenda", null);
        cabecera.put("usuario", factura.usuario());
        cabecera.put("codigoDocumentoSector", sector());
        return Map.of("cabecera", cabecera, "detalle", detalle(factura));
    }

    private List<Map<String, Object>> detalle(Factura factura) {
        var lineas = new ArrayList<Map<String, Object>>();
        for (var linea : factura.detalle()) {
            var item = new LinkedHashMap<String, Object>();
            item.put("actividadEconomica", linea.item().actividad());
            item.put("codigoProductoSin", Long.valueOf(linea.item().codigoSin()));
            item.put("codigoProducto", linea.item().codigo());
            item.put("descripcion", linea.item().descripcion());
            item.put("cantidad", linea.item().cantidad());
            item.put("unidadMedida", linea.item().unidadMedida());
            item.put("precioUnitario", linea.item().precioUnitario());
            item.put("montoDescuento", linea.item().descuento() == null ? BigDecimal.ZERO.setScale(2) : linea.item().descuento());
            item.put("subTotal", linea.subtotal());
            lineas.add(item);
        }
        return lineas;
    }
}
