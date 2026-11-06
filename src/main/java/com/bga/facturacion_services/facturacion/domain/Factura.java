package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Factura(UUID id, long numero, String tipo, OffsetDateTime fecha,
                      Emisor emisor, Cliente cliente, int metodoPago, String usuario,
                      List<Linea> detalle, BigDecimal descuentoAdicional,
                      BigDecimal total, BigDecimal totalSujetoIva) {
    public Factura { detalle = List.copyOf(detalle); }
    public record Emisor(String nit, String razonSocial, String municipio, String direccion,
                         int sucursal, int puntoVenta) {}
    public record Cliente(int tipoDocumento, String documento, String complemento, String razonSocial) {}
    public record Item(String codigo, String codigoSin, String actividad, String descripcion,
                       int unidadMedida, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal descuento) {}
    public record Linea(Item item, BigDecimal subtotal) {}
}

