package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import com.bga.facturacion_services.facturacion.application.feel.DocumentoFeel;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.domain.FeelMetadata;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class FeelNotaMonedaExtranjeraDocumento implements DocumentoFeel {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public int sector() { return 9; }
    public String claveSobre() { return "notaMonedaExtranjera"; }

    public boolean admite(Factura factura) {
        return factura.codigoDocumentoSector() == 9;
    }

    public Map<String, Object> armar(Factura factura) {
        FeelMetadata meta = factura.feel();
        long nit = FeelConversiones.nit(factura.emisor().nit());
        var venta = factura.venta();
        String monedaCatalogo = FeelConversiones.codigoMoneda(factura);
        var cabecera = new CabeceraFeel()
                .poner("nitEmisor", nit)
                .poner("razonSocialEmisor", factura.emisor().razonSocial())
                .poner("municipio", FeelConversiones.vacio(factura.emisor().municipio()))
                .poner("telefono", FeelConversiones.vacio(factura.emisor().telefono()))
                .poner("numeroFactura", String.valueOf(meta.numeroFacturaEnCabecera(factura.numero())))
                .poner("cuf", "")
                .poner("cufd", "")
                .poner("codigoSucursal", String.valueOf(factura.emisor().sucursal()))
                .poner("direccion", FeelConversiones.vacio(factura.emisor().direccion()))
                .poner("codigoPuntoVenta", String.valueOf(factura.emisor().puntoVenta()))
                .poner("fechaEmision", FECHA.format(factura.fecha()))
                .poner("nombreRazonSocial", factura.cliente().razonSocial())
                .poner("codigoTipoDocumentoIdentidad", FeelConversiones.tipoDocumentoIdentidad(factura.cliente()))
                .poner("numeroDocumento", factura.cliente().documento())
                .poner("complemento", factura.cliente().complemento() == null ? "" : factura.cliente().complemento())
                .poner("codigoCliente", factura.cliente().documento())
                .poner("codigoTipoOperacion", meta.codigoTipoOperacion() == null ? 2 : meta.codigoTipoOperacion())
                .poner("codigoMetodoPago", FeelConversiones.codigoMetodoPago(factura))
                .poner("numeroTarjeta", String.valueOf(FeelConversiones.tarjeta(venta.numeroTarjeta())))
                .poner("montoTotal", factura.total())
                .poner("montoTotalSujetoIva", factura.totalSujetoIva())
                .poner("ingresoDiferenciaCambio", meta.ingresoDiferenciaCambio() == null ? BigDecimal.ZERO : meta.ingresoDiferenciaCambio())
                .poner("codigoMoneda", monedaCatalogo)
                .poner("tipoCambio", venta.tipoCambio().toPlainString())
                .poner("montoTotalMoneda", venta.montoTotalMoneda())
                .poner("descuentoAdicional", FeelConversiones.ceroNulo(factura.descuentoAdicional()))
                .poner("codigoExcepcion", venta.codigoExcepcion() == null ? 0 : venta.codigoExcepcion())
                .poner("cafc", FeelConversiones.vacio(venta.cafc()))
                .poner("leyenda", "")
                .poner("usuario", factura.usuario())
                .poner("tipoCambioOficial", meta.tipoCambioOficial() == null ? venta.tipoCambio() : meta.tipoCambioOficial())
                .poner("codigoDocumentoSector", sector());
        return Map.of("cabecera", cabecera.campos(), "detalle", detalle(factura));
    }

    private List<Map<String, Object>> detalle(Factura factura) {
        var lineas = new ArrayList<Map<String, Object>>();
        for (var linea : factura.detalle()) {
            var item = new LinkedHashMap<String, Object>();
            item.put("actividadEconomica", FeelConversiones.vacio(linea.item().actividad()));
            long sin = Long.parseLong(linea.item().codigoSin());
            item.put("codigoProductoSin", sin);
            item.put("codigoProducto", parseCodigoProducto(linea.item().codigo(), sin));
            item.put("descripcion", linea.item().descripcion());
            item.put("cantidad", linea.item().cantidad());
            item.put("unidadMedida", BigDecimal.valueOf(linea.item().unidadMedida()));
            item.put("precioUnitario", linea.item().precioUnitario());
            item.put("montoDescuento", FeelConversiones.ceroNulo(linea.item().descuento()));
            item.put("subTotal", linea.subtotal());
            lineas.add(item);
        }
        return lineas;
    }

    private Object parseCodigoProducto(String codigo, long sin) {
        if (codigo != null && codigo.chars().allMatch(Character::isDigit)) {
            try { return Long.parseLong(codigo); }
            catch (NumberFormatException ignored) { }
        }
        return sin;
    }
}
