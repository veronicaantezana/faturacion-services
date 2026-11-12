package com.bga.facturacion_services.facturacion.infrastructure.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeelClienteHttpTest {
    @Test
    void leeElCufDesdeLaCabeceraCuandoProcesoNoLoTrae() {
        var respuesta = FeelClienteHttp.interpretar("""
                {"facturaCompraVenta":{"cabecera":{"cuf":"CUFCABECERA","numeroFactura":16}},
                "respuesta":{"codRespuesta":"0","txtRespuesta":"Exito"},
                "proceso":{"idDocFiscalFEEL":"99","cufd":"CUFDTEST","fueraLinea":false}}
                """);
        assertThat(respuesta.cuf()).isEqualTo("CUFCABECERA");
        assertThat(respuesta.cufd()).isEqualTo("CUFDTEST");
        assertThat(respuesta.fueraLinea()).isFalse();
    }
}
