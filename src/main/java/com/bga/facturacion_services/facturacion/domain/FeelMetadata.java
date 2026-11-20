package com.bga.facturacion_services.facturacion.domain;

import java.math.BigDecimal;

/** Parametros de envio y campos extra que dependen del tipo de documento fiscal. */
public record FeelMetadata(
        int codigoTipoFactura,
        String idDocFiscalErp,
        boolean esLote,
        String idLoteErp,
        boolean ultFacturaLote,
        String eventoId,
        Long numeroFacturaCabecera,
        BigDecimal ingresoDiferenciaCambio,
        Integer codigoTipoOperacion,
        BigDecimal tipoCambioOficial,
        String codigoMonedaCatalogo,
        String codigoMetodoPagoErp) {

  public static FeelMetadata compraVenta() {
    return new FeelMetadata(1, null, false, "0", false, null, null, null, null, null, null, null);
  }

  public static FeelMetadata notaMonedaExtranjera(
          String idDocFiscalErp,
          boolean esLote,
          String idLoteErp,
          BigDecimal ingresoDiferenciaCambio,
          int codigoTipoOperacion,
          BigDecimal tipoCambioOficial,
          String codigoMonedaCatalogo) {
        String lote = idLoteErp == null || idLoteErp.isBlank() ? "0" : idLoteErp.trim();
        return new FeelMetadata(
            2,
            idDocFiscalErp,
            esLote,
            lote,
            false,
            "",
            0L,
            ingresoDiferenciaCambio,
            codigoTipoOperacion,
            tipoCambioOficial,
            codigoMonedaCatalogo,
            null);
  }

  public FeelMetadata conCodigosErp(String moneda, String metodoPago) {
    return new FeelMetadata(codigoTipoFactura, idDocFiscalErp, esLote, idLoteErp, ultFacturaLote, eventoId,
            numeroFacturaCabecera, ingresoDiferenciaCambio, codigoTipoOperacion, tipoCambioOficial,
            moneda, metodoPago);
  }

  public int codigoTipoFactura(int porDefecto) {
    return codigoTipoFactura > 0 ? codigoTipoFactura : porDefecto;
  }

  public String idDocFiscalErp(long numeroFactura) {
    if (idDocFiscalErp != null && !idDocFiscalErp.isBlank()) return idDocFiscalErp.trim();
    return String.valueOf(numeroFactura);
  }

  public Object numeroFacturaEnCabecera(long numeroFactura) {
    if (numeroFacturaCabecera != null) return numeroFacturaCabecera;
    return numeroFactura;
  }
}
