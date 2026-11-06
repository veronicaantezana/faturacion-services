package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.util.List;

public record SolicitudFactura(String tipo, Factura.Cliente cliente, int metodoPago,
                               String usuario, List<Factura.Item> detalle, BigDecimal descuentoAdicional) {}

