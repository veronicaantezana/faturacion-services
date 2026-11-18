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
        var cabecera = new CabeceraFeel()
                .poner("nitEmisor", nit)
                .poner("razonSocialEmisor", factura.emisor().razonSocial())
                .poner("municipio", FeelConversiones.vacio(factura.emisor().municipio()))
                .poner("telefono", FeelConversiones.vacio(factura.emisor().telefono()))
                .poner("numeroFactura", factura.feel().numeroFacturaEnCabecera(factura.numero()))
                .poner("cuf", "")
                .poner("cufd", "")
                .poner("codigoSucursal", factura.emisor().sucursal())
                .poner("direccion", FeelConversiones.vacio(factura.emisor().direccion()))
                .poner("codigoPuntoVenta", factura.emisor().puntoVenta())
                .poner("fechaEmision", FECHA.format(factura.fecha()))
                .poner("nombreRazonSocial", factura.cliente().razonSocial())
                .poner("codigoTipoDocumentoIdentidad", FeelConversiones.tipoDocumentoIdentidad(factura.cliente().tipoDocumento()))
                .poner("numeroDocumento", factura.cliente().documento())
                .poner("complemento", FeelConversiones.vacio(factura.cliente().complemento()))
                .poner("codigoCliente", factura.cliente().documento())
                .poner("codigoMetodoPago", String.valueOf(factura.metodoPago()))
                .poner("numeroTarjeta", FeelConversiones.tarjeta(venta.numeroTarjeta()))
                .poner("montoTotal", factura.total())
                .poner("montoTotalSujetoIva", factura.totalSujetoIva())
                .poner("codigoMoneda", FeelConversiones.tipoMoneda(venta.codigoMoneda()))
                .poner("tipoCambio", venta.tipoCambio())
                .poner("montoTotalMoneda", venta.montoTotalMoneda())
                .poner("montoGiftCard", FeelConversiones.ceroNulo(venta.montoGiftCard()))
                .poner("descuentoAdicional", FeelConversiones.ceroNulo(factura.descuentoAdicional()))
                .poner("codigoExcepcion", venta.codigoExcepcion() == null ? 0 : venta.codigoExcepcion())
                .poner("cafc", FeelConversiones.vacio(venta.cafc()))
                .poner("leyenda", null)
                .poner("usuario", factura.usuario())
                .poner("codigoDocumentoSector", sector());
        return Map.of("cabecera", cabecera.campos(), "detalle", detalle(factura));
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
