package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.domain.Factura;
import com.bga.facturacion_services.facturacion.domain.FeelMetadata;
import com.bga.facturacion_services.facturacion.domain.SolicitudFactura;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Cuerpo que envía la pantalla de emisión del frontend. */
public record EmitirCompraVentaRequest(
        @NotNull @Min(0) Integer codigoSucursal,
        @NotNull @Min(0) Integer codigoPuntoVenta,
        @NotNull @Min(1) @Max(5) Integer codigoTipoDocumentoIdentidad,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @Size(max = 10) String complemento,
        @NotBlank @Size(max = 250) String nombreRazonSocial,
        @Size(max = 120) String correoCliente,
        @NotNull @Min(1) Integer codigoMetodoPago,
        @Size(max = 30) String numeroTarjeta,
        @Min(1) Integer codigoMoneda,
        @DecimalMin(value = "0", inclusive = false) BigDecimal tipoCambio,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal montoGiftCard,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal descuentoAdicional,
        Integer codigoExcepcion,
        @Size(max = 50) String cafc,
        @NotBlank @Size(max = 100) String usuario,
        Integer codigoDocumentoSector,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid ItemCompraVenta> items,
        @Size(max = 30) String idDocFiscalErp,
        Boolean esLote,
        @Size(max = 30) String idLoteErp,
        @DecimalMin("0") BigDecimal ingresoDiferenciaCambio,
        Integer codigoTipoOperacion,
        @DecimalMin(value = "0", inclusive = false) BigDecimal tipoCambioOficial,
        @Size(max = 10) String codigoMonedaCatalogo,
        @NotBlank @Size(max = 20) String nitEmisor,
        @NotBlank @Size(max = 250) String razonSocialEmisor,
        @Size(max = 100) String municipio,
        @Size(max = 250) String direccion,
        @Size(max = 30) String telefono) {

    public record ItemCompraVenta(
            @NotBlank @Size(max = 50) String codigoProducto,
            @NotBlank @Size(max = 500) String descripcion,
            @Size(max = 20) String actividadEconomica,
            @NotNull @Min(1) Long codigoProductoSin,
            @NotNull @Min(1) Integer unidadMedida,
            String unidadMedidaDescripcion,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 5) BigDecimal cantidad,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 14, fraction = 2) BigDecimal precioUnitario,
            @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal montoDescuento,
            @Size(max = 50) String numeroSerie,
            @Size(max = 50) String numeroImei) {}

    public SolicitudFactura toDomain() {
        int sector = codigoDocumentoSector == null ? 1 : codigoDocumentoSector;
        if (sector != 1 && sector != 9)
            throw new IllegalArgumentException("Sectores soportados: 1 compra-venta y 9 nota moneda extranjera");
        String tipo = sector == 9 ? "NOTA_MONEDA_EXTRANJERA" : "COMPRA_VENTA";
        FeelMetadata feel = sector == 9
                ? FeelMetadata.notaMonedaExtranjera(idDocFiscalErp, Boolean.TRUE.equals(esLote), idLoteErp,
                ingresoDiferenciaCambio, codigoTipoOperacion == null ? 2 : codigoTipoOperacion,
                tipoCambioOficial, codigoMonedaCatalogo)
                : FeelMetadata.compraVenta();
        return new SolicitudFactura(tipo,
                new Factura.Cliente(codigoTipoDocumentoIdentidad, numeroDocumento.trim(), complemento, nombreRazonSocial.trim()),
                codigoMetodoPago, usuario.trim(),
                items.stream().map(i -> new Factura.Item(i.codigoProducto().trim(), String.valueOf(i.codigoProductoSin()),
                        i.actividadEconomica() == null ? "" : i.actividadEconomica().trim(), i.descripcion().trim(),
                        i.unidadMedida(), i.cantidad(), i.precioUnitario(), i.montoDescuento(), i.numeroSerie(), i.numeroImei())).toList(),
                descuentoAdicional, codigoSucursal, codigoPuntoVenta, correoCliente, numeroTarjeta,
                codigoMoneda, tipoCambio, montoGiftCard, codigoExcepcion, cafc, sector, feel,
                sector == 9 ? java.math.BigDecimal.ZERO.setScale(2) : null,
                nitEmisor, razonSocialEmisor, municipio, direccion, telefono);
    }
}
