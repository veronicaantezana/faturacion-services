package com.bga.facturacion_services.facturacion.infrastructure.rest;

import com.bga.facturacion_services.facturacion.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record FacturaRequest(
        @NotBlank String tipo,
        @NotNull @Valid ClienteRequest cliente,
        @Min(1) int metodoPago,
        @NotBlank @Size(max = 100) String usuario,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid ItemRequest> detalle,
        @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal descuentoAdicional) {
    public record ClienteRequest(@Min(1) @Max(5) int tipoDocumento,
            @NotBlank @Size(max = 30) String documento,
            @Size(max = 10) String complemento, @Size(max = 250) String razonSocial) {}
    public record ItemRequest(
            @NotBlank @Size(max = 50) String codigo,
            @NotBlank @Pattern(regexp = "[0-9]{1,20}") String codigoSin,
            @NotBlank @Size(max = 20) String actividad,
            @NotBlank @Size(max = 500) String descripcion,
            @Min(1) int unidadMedida,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 5) BigDecimal cantidad,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 14, fraction = 2) BigDecimal precioUnitario,
            @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal descuento) {}
    public SolicitudFactura toDomain() {
        return new SolicitudFactura(tipo,
                new Factura.Cliente(cliente.tipoDocumento(), cliente.documento(), cliente.complemento(), cliente.razonSocial()),
                metodoPago, usuario,
                detalle.stream().map(i -> new Factura.Item(i.codigo(), i.codigoSin(), i.actividad(), i.descripcion(),
                        i.unidadMedida(), i.cantidad(), i.precioUnitario(), i.descuento())).toList(), descuentoAdicional);
    }
}
