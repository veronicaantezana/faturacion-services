package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Factura(UUID id, long numero, String tipo, OffsetDateTime fecha,
                      Emisor emisor, Cliente cliente, int metodoPago, String usuario,
                      List<Linea> detalle, BigDecimal descuentoAdicional,
                      BigDecimal total, BigDecimal totalSujetoIva, DatosVenta venta,
                      int codigoDocumentoSector, FeelMetadata feel) {
    public Factura(UUID id, long numero, String tipo, OffsetDateTime fecha,
                   Emisor emisor, Cliente cliente, int metodoPago, String usuario,
                   List<Linea> detalle, BigDecimal descuentoAdicional,
                   BigDecimal total, BigDecimal totalSujetoIva, DatosVenta venta) {
        this(id, numero, tipo, fecha, emisor, cliente, metodoPago, usuario, detalle, descuentoAdicional,
                total, totalSujetoIva, venta, 1, FeelMetadata.compraVenta());
    }
    public Factura { detalle = List.copyOf(detalle); if (feel == null) feel = FeelMetadata.compraVenta(); }

    public Factura conCodigoDocumentoFeel(String codigo) {
        Cliente actual = cliente;
        return new Factura(id, numero, tipo, fecha, emisor,
                new Cliente(actual.tipoDocumento(), actual.documento(), actual.complemento(), actual.razonSocial(), codigo),
                metodoPago, usuario, detalle, descuentoAdicional, total, totalSujetoIva, venta, codigoDocumentoSector, feel);
    }

    public Factura conCodigosEmision(String tipoDocumento, String metodoPagoErp, String monedaErp, String codigoSistema) {
        int pago = metodoPago;
        if (metodoPagoErp != null && metodoPagoErp.chars().allMatch(Character::isDigit))
            pago = Integer.parseInt(metodoPagoErp);
        return new Factura(id, numero, tipo, fecha, emisor.conCodigoSistema(codigoSistema),
                new Cliente(cliente.tipoDocumento(), cliente.documento(), cliente.complemento(), cliente.razonSocial(), tipoDocumento),
                pago, usuario, detalle, descuentoAdicional, total, totalSujetoIva, venta, codigoDocumentoSector,
                feel.conCodigosErp(monedaErp, metodoPagoErp));
    }
    public record Emisor(String nit, String razonSocial, String municipio, String direccion,
                         int sucursal, int puntoVenta, String telefono, String codigoSistema) {
        public Emisor(String nit, String razonSocial, String municipio, String direccion, int sucursal, int puntoVenta, String telefono) {
            this(nit, razonSocial, municipio, direccion, sucursal, puntoVenta, telefono, null);
        }
        public Emisor(String nit, String razonSocial, String municipio, String direccion, int sucursal, int puntoVenta) {
            this(nit, razonSocial, municipio, direccion, sucursal, puntoVenta, null, null);
        }
        public Emisor conCodigoSistema(String codigo) {
            return new Emisor(nit, razonSocial, municipio, direccion, sucursal, puntoVenta, telefono, codigo);
        }
    }
    public record Cliente(int tipoDocumento, String documento, String complemento, String razonSocial,
                          String codigoDocumentoFeel) {
        public Cliente(int tipoDocumento, String documento, String complemento, String razonSocial) {
            this(tipoDocumento, documento, complemento, razonSocial, null);
        }
    }
    public record Item(String codigo, String codigoSin, String actividad, String descripcion,
                       int unidadMedida, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal descuento,
                       String numeroSerie, String numeroImei) {
        public Item(String codigo, String codigoSin, String actividad, String descripcion, int unidadMedida,
                    BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal descuento) {
            this(codigo, codigoSin, actividad, descripcion, unidadMedida, cantidad, precioUnitario, descuento, null, null);
        }
    }
    public record Linea(Item item, BigDecimal subtotal) {}
    public record DatosVenta(int codigoMoneda, BigDecimal tipoCambio, BigDecimal montoTotalMoneda,
                             BigDecimal montoGiftCard, String numeroTarjeta, Integer codigoExcepcion,
                             String cafc, String correo) {}
}

