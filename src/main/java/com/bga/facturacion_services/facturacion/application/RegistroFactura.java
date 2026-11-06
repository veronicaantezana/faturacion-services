package com.bga.facturacion_services.facturacion.application;

import com.bga.facturacion_services.facturacion.domain.Factura;

public record RegistroFactura(Factura factura, ResultadoEmision emision) {}

