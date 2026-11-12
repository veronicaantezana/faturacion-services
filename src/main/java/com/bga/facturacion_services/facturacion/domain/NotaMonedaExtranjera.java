package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class NotaMonedaExtranjera implements ReglaFactura {
    public String tipo() { return "NOTA_MONEDA_EXTRANJERA"; }

    public Factura construir(UUID id, long numero, OffsetDateTime fecha, Factura.Emisor emisor, SolicitudFactura s) {
        if (s.cliente() == null || s.cliente().tipoDocumento() < 1 || s.cliente().tipoDocumento() > 5
                || vacio(s.cliente().documento()) || vacio(s.usuario()) || s.metodoPago() <= 0)
            throw new IllegalArgumentException("Cliente, documento, usuario y metodo de pago son obligatorios");
        if (s.detalle() == null || s.detalle().isEmpty() || s.detalle().size() > 500)
            throw new IllegalArgumentException("La nota requiere entre 1 y 500 items");
        List<Factura.Linea> lineas = s.detalle().stream().map(this::linea).toList();
        BigDecimal descuento = dinero(s.descuentoAdicional() == null ? BigDecimal.ZERO : s.descuentoAdicional());
        BigDecimal suma = lineas.stream().map(Factura.Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (descuento.compareTo(suma) >= 0) throw new IllegalArgumentException("El descuento debe ser menor al subtotal");
        BigDecimal total = suma.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        int moneda = s.codigoMoneda() == null ? 1 : s.codigoMoneda();
        BigDecimal cambio = s.tipoCambio() == null || s.tipoCambio().signum() <= 0
                ? BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP) : s.tipoCambio().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoMoneda = total.divide(cambio, 2, RoundingMode.HALF_UP);
        var venta = new Factura.DatosVenta(moneda, cambio, montoMoneda, BigDecimal.ZERO, vacioNulo(s.numeroTarjeta()),
                s.codigoExcepcion(), vacioNulo(s.cafc()), vacioNulo(s.correo()));
        int sector = s.codigoDocumentoSector() == null ? 9 : s.codigoDocumentoSector();
        FeelMetadata feel = s.feel() != null ? s.feel() : FeelMetadata.notaMonedaExtranjera(
                null, false, "0", BigDecimal.ZERO, 2, cambio, null);
        BigDecimal sujeto = s.totalSujetoIvaForzado() != null ? s.totalSujetoIvaForzado() : BigDecimal.ZERO.setScale(2);
        return new Factura(id, numero, tipo(), fecha, emisor, s.cliente(), s.metodoPago(), s.usuario(),
                lineas, descuento, total, sujeto, venta, sector, feel);
    }

    private Factura.Linea linea(Factura.Item i) {
        if (i == null || vacio(i.codigoSin()) || !i.codigoSin().matches("[0-9]+") || vacio(i.descripcion()))
            throw new IllegalArgumentException("Datos de producto incompletos para moneda extranjera");
        if (i.cantidad() == null || i.cantidad().signum() <= 0)
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        BigDecimal precio = precioUnitario(i.precioUnitario());
        BigDecimal descuento = dinero(i.descuento() == null ? BigDecimal.ZERO : i.descuento());
        BigDecimal subtotal = i.cantidad().multiply(precio).subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        if (subtotal.signum() <= 0) throw new IllegalArgumentException("Subtotal de linea invalido");
        String codigo = vacio(i.codigo()) ? i.codigoSin() : i.codigo();
        int unidad = i.unidadMedida() <= 0 ? 58 : i.unidadMedida();
        return new Factura.Linea(new Factura.Item(codigo, i.codigoSin(), i.actividad(), i.descripcion(),
                unidad, i.cantidad(), precio, descuento, i.numeroSerie(), i.numeroImei()), subtotal);
    }

    private BigDecimal dinero(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.scale() > 2)
            throw new IllegalArgumentException("Importe invalido");
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal precioUnitario(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > 8)
            throw new IllegalArgumentException("Precio unitario invalido");
        return valor;
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
    private String vacioNulo(String valor) { return vacio(valor) ? null : valor.trim(); }
}
