package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Factura(UUID id, long numero, String tipo, OffsetDateTime fecha,
                      Emisor emisor, Cliente cliente, int metodoPago, String usuario,
                      List<Linea> detalle, BigDecimal descuentoAdicional,
                      BigDecimal total, BigDecimal totalSujetoIva, DatosVenta venta) {
    public Factura { detalle = List.copyOf(detalle); }
    public record Emisor(String nit, String razonSocial, String municipio, String direccion,
                         int sucursal, int puntoVenta, String telefono) {
        public Emisor(String nit, String razonSocial, String municipio, String direccion, int sucursal, int puntoVenta) {
            this(nit, razonSocial, municipio, direccion, sucursal, puntoVenta, null);
        }
    }
    public record Cliente(int tipoDocumento, String documento, String complemento, String razonSocial) {}
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

