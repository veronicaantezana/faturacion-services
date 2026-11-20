package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;
import java.util.List;

public record SolicitudFactura(String tipo, Factura.Cliente cliente, int metodoPago,
                               String usuario, List<Factura.Item> detalle, BigDecimal descuentoAdicional,
                               Integer sucursal, Integer puntoVenta, String correo, String numeroTarjeta,
                               Integer codigoMoneda, BigDecimal tipoCambio, BigDecimal montoGiftCard,
                               Integer codigoExcepcion, String cafc, Integer codigoDocumentoSector,
                               FeelMetadata feel, BigDecimal totalSujetoIvaForzado,
                               String nitEmisor, String razonSocialEmisor, String municipio,
                               String direccion, String telefono, Long idEmpresa) {
    public SolicitudFactura(String tipo, Factura.Cliente cliente, int metodoPago, String usuario,
                            List<Factura.Item> detalle, BigDecimal descuentoAdicional) {
        this(tipo, cliente, metodoPago, usuario, detalle, descuentoAdicional,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }
}

