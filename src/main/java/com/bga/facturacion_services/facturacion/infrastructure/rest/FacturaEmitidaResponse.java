package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.application.RegistroFactura;
import com.bga.facturacion_services.facturacion.application.ResultadoEmision;
import com.bga.facturacion_services.facturacion.domain.Factura;
import java.math.BigDecimal;
import java.util.List;

/** Respuesta que consume el comprobante del frontend. */
public record FacturaEmitidaResponse(
        long id, long nitEmisor, String razonSocialEmisor, String municipio, String telefono,
        long numeroFactura, String cuf, String cufd, String codigoControlCufd, int codigoSucursal,
        String nombreSucursal, String direccion, Integer codigoPuntoVenta, String nombrePuntoVenta,
        String fechaEmision, String nombreRazonSocial, int codigoTipoDocumentoIdentidad,
        String tipoDocumentoDescripcion, String numeroDocumento, String complemento, String codigoCliente,
        String correoCliente, int codigoMetodoPago, String metodoPagoDescripcion, String numeroTarjeta,
        BigDecimal montoTotal, BigDecimal montoTotalSujetoIva, int codigoMoneda, BigDecimal tipoCambio,
        BigDecimal montoTotalMoneda, BigDecimal montoGiftCard, BigDecimal descuentoAdicional,
        Integer codigoExcepcion, String cafc, String leyenda, String usuario, int codigoDocumentoSector,
        String modoEmision, String estado, boolean reversionAnulacionUtilizada, List<ItemEmitido> items) {

    public record ItemEmitido(String actividadEconomica, long codigoProductoSin, String codigoProducto,
                              String descripcion, BigDecimal cantidad, int unidadMedida, String unidadMedidaDescripcion,
                              BigDecimal precioUnitario, BigDecimal montoDescuento, BigDecimal subTotal,
                              String numeroSerie, String numeroImei) {}

    public static FacturaEmitidaResponse desde(RegistroFactura registro) {
        Factura factura = registro.factura();
        ResultadoEmision emision = registro.emision();
        var venta = factura.venta();
        boolean contingencia = emision.estado() == ResultadoEmision.Estado.CONTINGENCIA;
        return new FacturaEmitidaResponse(
                factura.numero(), Long.parseLong(factura.emisor().nit()), factura.emisor().razonSocial(),
                factura.emisor().municipio(), factura.emisor().telefono() == null ? "" : factura.emisor().telefono(),
                factura.numero(), emision.cuf() == null ? "" : emision.cuf(), emision.cufd() == null ? "" : emision.cufd(),
                "", factura.emisor().sucursal(), "Sucursal " + factura.emisor().sucursal(),
                factura.emisor().direccion() == null ? "" : factura.emisor().direccion(),
                factura.emisor().puntoVenta(), "Punto de venta " + factura.emisor().puntoVenta(),
                factura.fecha().toString(), factura.cliente().razonSocial(), factura.cliente().tipoDocumento(),
                tipoDocumento(factura.cliente().tipoDocumento()), factura.cliente().documento(),
                factura.cliente().complemento(), factura.cliente().documento(),
                venta.correo() == null ? "" : venta.correo(), factura.metodoPago(), metodoPago(factura.metodoPago()),
                venta.numeroTarjeta(), factura.total(), factura.totalSujetoIva(), venta.codigoMoneda(),
                venta.tipoCambio(), venta.montoTotalMoneda(), venta.montoGiftCard(), factura.descuentoAdicional(),
                venta.codigoExcepcion(), venta.cafc(), "", factura.usuario(), 1,
                contingencia ? "CONTINGENCIA" : "EN_LINEA",
                contingencia ? "PENDIENTE_CONTINGENCIA" : "VALIDADA",
                false,
                factura.detalle().stream().map(linea -> new ItemEmitido(
                        linea.item().actividad(), Long.parseLong(linea.item().codigoSin()), linea.item().codigo(),
                        linea.item().descripcion(), linea.item().cantidad(), linea.item().unidadMedida(), null,
                        linea.item().precioUnitario(), linea.item().descuento(), linea.subtotal(),
                        linea.item().numeroSerie(), linea.item().numeroImei())).toList());
    }

    private static String tipoDocumento(int codigo) {
        return switch (codigo) {
            case 1 -> "CI - Cedula de Identidad";
            case 2 -> "CEX - Cedula de Identidad de Extranjero";
            case 3 -> "PAS - Pasaporte";
            case 4 -> "OD - Otro Documento de Identidad";
            case 5 -> "NIT - Numero de Identificacion Tributaria";
            default -> "Documento " + codigo;
        };
    }

    private static String metodoPago(int codigo) {
        return switch (codigo) {
            case 1 -> "Efectivo";
            case 2 -> "Tarjeta";
            case 7 -> "Transferencia Bancaria";
            case 32 -> "Pago QR Billetera Movil";
            default -> "Metodo " + codigo;
        };
    }
}
