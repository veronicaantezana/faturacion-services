package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.domain.Factura;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mismo sobre que FEEL recibe para una entidad financiera.
 * Compra-venta llena solo facturaCompraVenta; CUF, CUFD, leyenda y dirección quedan vacíos para que FEEL los complete.
 */
public final class DocumentoFiscalFeel {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String[] TIPOS = {
            "facturaAlcanzadaIce", "facturaCompraVenta", "facturaAlquiler", "facturaEntidadFinanciera",
            "facturaColegio", "facturaHospital", "facturaHotel", "facturaServicios", "notaLibreConsignacion",
            "notaExportacion", "notaExportacionPVenta", "facturaComercialExportacionServicio",
            "facturaComercializacionHidrocarburo", "facturaHidrocarburo", "notaMonedaExtranjera",
            "facturaBoletoAereo", "facturaTelecomunicacion", "facturaPrevalorada", "facturaSeguros",
            "facturaSeguridadAlimentaria", "notaTasaCero", "facturaZonaFranca", "notaCreditoDebito",
            "facturaCompraVentaBon", "notaConciliacion", "facturaIceZonaFranca", "facturaTuristicoHospedaje",
            "notaCreditoDebitoDescuento", "notaCreditoDebitoIce", "facturaVentaCombustibleSinSubvencion"
    };

    private DocumentoFiscalFeel() {}

    public static Map<String, Object> desde(Factura factura, String codigoSistemaOrigen, int codigoTipoFactura, boolean sincrono) {
        if (codigoSistemaOrigen == null || codigoSistemaOrigen.isBlank())
            throw new IllegalArgumentException("Falta configurar facturacion.emisor.codigo-sistema-origen");
        long nit;
        try { nit = Long.parseLong(factura.emisor().nit()); }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El NIT del emisor debe ser numerico");
        }
        var venta = factura.venta();
        var documento = new LinkedHashMap<String, Object>();
        for (String tipo : TIPOS) documento.put(tipo, null);
        documento.put("facturaCompraVenta", Map.of(
                "cabecera", cabecera(factura, nit, venta),
                "detalle", detalle(factura)));
        documento.put("idDocFiscalERP", String.valueOf(factura.numero()));
        documento.put("cufd", "");
        documento.put("contingencia", false);
        documento.put("esLote", false);
        documento.put("emailCliente", vacio(venta.correo()));
        documento.put("telefonoCliente", null);
        documento.put("idLoteERP", "0");
        documento.put("ultFacturaLote", false);
        documento.put("codigoSistemaOrigen", codigoSistemaOrigen.trim());
        documento.put("codigoTipoFactura", codigoTipoFactura);
        documento.put("fueraLinea", null);
        documento.put("codigoControl", null);
        documento.put("sincrono", sincrono);
        documento.put("eventoId", null);
        documento.put("pruebaDeConexion", null);
        documento.put("typeTasaCero", null);
        documento.put("firmado", null);
        documento.put("xmlFirmado", null);
        documento.put("esPaquete", null);
        documento.put("cantidadPaquete", null);
        documento.put("codigoPaquete", null);
        documento.put("informacionAdicional", null);
        documento.put("numeracion", null);
        return documento;
    }

    private static Map<String, Object> cabecera(Factura factura, long nit, Factura.DatosVenta venta) {
        var cabecera = new LinkedHashMap<String, Object>();
        cabecera.put("nitEmisor", nit);
        cabecera.put("razonSocialEmisor", factura.emisor().razonSocial());
        cabecera.put("municipio", factura.emisor().municipio());
        cabecera.put("telefono", vacio(factura.emisor().telefono()));
        cabecera.put("numeroFactura", factura.numero());
        cabecera.put("cuf", "");
        cabecera.put("cufd", "");
        cabecera.put("codigoSucursal", factura.emisor().sucursal());
        cabecera.put("direccion", "");
        cabecera.put("codigoPuntoVenta", factura.emisor().puntoVenta());
        cabecera.put("fechaEmision", FECHA.format(factura.fecha()));
        cabecera.put("nombreRazonSocial", factura.cliente().razonSocial());
        cabecera.put("codigoTipoDocumentoIdentidad", String.valueOf(factura.cliente().tipoDocumento()));
        cabecera.put("numeroDocumento", factura.cliente().documento());
        cabecera.put("complemento", factura.cliente().complemento() == null ? "" : factura.cliente().complemento());
        cabecera.put("codigoCliente", factura.cliente().documento());
        cabecera.put("codigoMetodoPago", String.valueOf(factura.metodoPago()));
        cabecera.put("numeroTarjeta", tarjeta(venta.numeroTarjeta()));
        cabecera.put("montoTotal", factura.total());
        cabecera.put("montoTotalSujetoIva", factura.totalSujetoIva());
        cabecera.put("codigoMoneda", String.valueOf(venta.codigoMoneda()));
        cabecera.put("tipoCambio", venta.tipoCambio());
        cabecera.put("montoTotalMoneda", venta.montoTotalMoneda());
        cabecera.put("montoGiftCard", ceroNulo(venta.montoGiftCard()));
        cabecera.put("descuentoAdicional", ceroNulo(factura.descuentoAdicional()));
        cabecera.put("codigoExcepcion", venta.codigoExcepcion() == null ? 0 : venta.codigoExcepcion());
        cabecera.put("cafc", vacio(venta.cafc()));
        cabecera.put("leyenda", "");
        cabecera.put("usuario", factura.usuario());
        cabecera.put("codigoDocumentoSector", 1);
        return cabecera;
    }

    private static List<Map<String, Object>> detalle(Factura factura) {
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

    private static String vacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static BigDecimal ceroNulo(BigDecimal valor) {
        return valor == null || valor.signum() == 0 ? null : valor;
    }

    private static long tarjeta(String valor) {
        if (valor == null || valor.isBlank() || !valor.chars().allMatch(Character::isDigit)) return 0L;
        try { return Long.parseLong(valor); }
        catch (NumberFormatException ex) { return 0L; }
    }
}
