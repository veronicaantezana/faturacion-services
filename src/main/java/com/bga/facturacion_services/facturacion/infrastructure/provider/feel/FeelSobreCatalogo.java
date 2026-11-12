package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import java.util.Arrays;
import java.util.List;

/** Claves del sobre FEEL; al agregar un tipo nuevo solo se implementa su armador. */
public final class FeelSobreCatalogo {
    public static final List<String> CLAVES = List.of(
            "facturaAlcanzadaIce", "facturaCompraVenta", "facturaAlquiler", "facturaEntidadFinanciera",
            "facturaColegio", "facturaHospital", "facturaHotel", "facturaServicios", "notaLibreConsignacion",
            "notaExportacion", "notaExportacionPVenta", "facturaComercialExportacionServicio",
            "facturaComercializacionHidrocarburo", "facturaHidrocarburo", "notaMonedaExtranjera",
            "facturaBoletoAereo", "facturaTelecomunicacion", "facturaPrevalorada", "facturaSeguros",
            "facturaSeguridadAlimentaria", "notaTasaCero", "facturaZonaFranca", "notaCreditoDebito",
            "facturaCompraVentaBon", "notaConciliacion", "facturaIceZonaFranca", "facturaTuristicoHospedaje",
            "notaCreditoDebitoDescuento", "notaCreditoDebitoIce", "facturaVentaCombustibleSinSubvencion");

    private FeelSobreCatalogo() {}
}
