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
        String monedaCatalogo = meta.codigoMonedaCatalogo() != null ? meta.codigoMonedaCatalogo() : String.valueOf(venta.codigoMoneda());
        var cabecera = new LinkedHashMap<String, Object>();
        cabecera.put("ingresoDiferenciaCambio", meta.ingresoDiferenciaCambio() == null ? BigDecimal.ZERO : meta.ingresoDiferenciaCambio());
        cabecera.put("codigoTipoOperacion", meta.codigoTipoOperacion() == null ? 2 : meta.codigoTipoOperacion());
        cabecera.put("tipoCambioOficial", meta.tipoCambioOficial() == null ? venta.tipoCambio() : meta.tipoCambioOficial());
        cabecera.put("nitEmisor", nit);
        cabecera.put("razonSocialEmisor", factura.emisor().razonSocial());
        cabecera.put("municipio", factura.emisor().municipio());
        cabecera.put("montoTotalSujetoIva", factura.totalSujetoIva());
        cabecera.put("numeroFactura", String.valueOf(meta.numeroFacturaEnCabecera(factura.numero())));
        cabecera.put("cuf", "");
        cabecera.put("codigoSucursal", String.valueOf(factura.emisor().sucursal()));
        cabecera.put("direccion", "");
        cabecera.put("codigoPuntoVenta", String.valueOf(factura.emisor().puntoVenta()));
        cabecera.put("fechaEmision", FECHA.format(factura.fecha()));
        cabecera.put("leyenda", "");
        cabecera.put("nombreRazonSocial", factura.cliente().razonSocial());
        cabecera.put("codigoTipoDocumentoIdentidad", FeelConversiones.tipoDocumentoIdentidad(factura.cliente().tipoDocumento()));
        cabecera.put("numeroDocumento", factura.cliente().documento());
        cabecera.put("complemento", factura.cliente().complemento() == null ? "" : factura.cliente().complemento());
        cabecera.put("codigoCliente", factura.cliente().documento());
        cabecera.put("codigoMetodoPago", String.valueOf(factura.metodoPago()));
        cabecera.put("numeroTarjeta", String.valueOf(FeelConversiones.tarjeta(venta.numeroTarjeta())));
        cabecera.put("montoTotal", factura.total());
        cabecera.put("codigoMoneda", monedaCatalogo);
        cabecera.put("tipoCambio", venta.tipoCambio().toPlainString());
        cabecera.put("montoTotalMoneda", venta.montoTotalMoneda());
        cabecera.put("usuario", factura.usuario());
        cabecera.put("codigoDocumentoSector", sector());
        cabecera.put("codigoExcepcion", venta.codigoExcepcion() == null ? 0 : venta.codigoExcepcion());
        cabecera.put("cafc", FeelConversiones.vacio(venta.cafc()));
        return Map.of("cabecera", cabecera, "detalle", detalle(factura));
    }

    private List<Map<String, Object>> detalle(Factura factura) {
        var lineas = new ArrayList<Map<String, Object>>();
        for (var linea : factura.detalle()) {
            var item = new LinkedHashMap<String, Object>();
            item.put("actividadEconomica", null);
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
