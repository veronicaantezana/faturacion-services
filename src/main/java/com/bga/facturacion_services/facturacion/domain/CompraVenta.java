package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CompraVenta implements ReglaFactura {
    public String tipo() { return "COMPRA_VENTA"; }

    public Factura construir(UUID id, long numero, OffsetDateTime fecha, Factura.Emisor emisor, SolicitudFactura s) {
        if (s.cliente() == null || s.cliente().tipoDocumento() < 1 || s.cliente().tipoDocumento() > 5
                || vacio(s.cliente().documento()) || vacio(s.usuario()) || s.metodoPago() <= 0)
            throw new IllegalArgumentException("Cliente, documento, usuario y metodo de pago son obligatorios");
        if (s.detalle() == null || s.detalle().isEmpty() || s.detalle().size() > 500)
            throw new IllegalArgumentException("La factura requiere entre 1 y 500 items");
        if (s.metodoPago() != 1)
            throw new IllegalArgumentException("Esta primera version soporta efectivo (metodoPago=1), moneda BOB y sin Gift Card");
        List<Factura.Linea> lineas = s.detalle().stream().map(this::linea).toList();
        BigDecimal descuento = dinero(s.descuentoAdicional() == null ? BigDecimal.ZERO : s.descuentoAdicional());
        BigDecimal suma = lineas.stream().map(Factura.Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (descuento.compareTo(suma) >= 0) throw new IllegalArgumentException("El descuento debe ser menor al subtotal");
        BigDecimal total = suma.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        return new Factura(id, numero, tipo(), fecha, emisor, s.cliente(), s.metodoPago(), s.usuario(),
                lineas, descuento, total, total);
    }

    private Factura.Linea linea(Factura.Item i) {
        if (i == null || vacio(i.codigo()) || vacio(i.codigoSin()) || !i.codigoSin().matches("[0-9]+")
                || vacio(i.actividad()) || vacio(i.descripcion()) || i.unidadMedida() <= 0)
            throw new IllegalArgumentException("Datos de producto incompletos");
        if (i.cantidad() == null || i.cantidad().signum() <= 0 || i.cantidad().scale() > 5)
            throw new IllegalArgumentException("Cantidad debe ser positiva con hasta 5 decimales");
        BigDecimal precio = dinero(i.precioUnitario());
        if (precio.signum() <= 0) throw new IllegalArgumentException("Precio debe ser positivo");
        BigDecimal descuento = dinero(i.descuento() == null ? BigDecimal.ZERO : i.descuento());
        BigDecimal subtotal = i.cantidad().multiply(precio).subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        if (subtotal.signum() <= 0) throw new IllegalArgumentException("Descuento de item invalido");
        return new Factura.Linea(new Factura.Item(i.codigo(), i.codigoSin(), i.actividad(), i.descripcion(),
                i.unidadMedida(), i.cantidad(), precio, descuento), subtotal);
    }

    private BigDecimal dinero(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.scale() > 2 || valor.precision() > 16)
            throw new IllegalArgumentException("Importe invalido: use valores no negativos con hasta 2 decimales y 16 digitos");
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}

