package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoProductosFeelTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void quitaLaComaDelCodigoErpYNoRepiteLaMismaFila() {
        var productos = CatalogoProductosFeel.interpretar("""
                {"respuesta":{"codRespuesta":"0","txtRespuesta":"Exito"},"list":[
                  {"codigoActividad":"641910","codigoSin":"83111","codigoERP":"83111,","descripcion":"COMISION"},
                  {"codigoActividad":"641910","codigoSin":"83111","codigoERP":"83111,","descripcion":"COMISION"},
                  {"codigoActividad":"641900","codigoSin":"71592","codigoERP":null,"descripcion":"CAMBIO"}
                ]}
                """, json);
        assertThat(productos).hasSize(2);
        assertThat(productos.get(0).codigoInterno()).isEqualTo("83111");
        assertThat(productos.get(0).codigoProductoSin()).isEqualTo(83111L);
        assertThat(productos.get(0).precioUnitario()).isNull();
        assertThat(productos.get(1).codigoInterno()).isEqualTo("71592");
        assertThat(productos.get(1).codigoActividad()).isEqualTo("641900");
    }

    @Test
    void rechazaCuandoFeelNoRespondeExito() {
        assertThatThrownBy(() -> CatalogoProductosFeel.interpretar("""
                {"respuesta":{"codRespuesta":"2007","txtRespuesta":"No mapeado"}}
                """, json))
                .isInstanceOf(CatalogoProductosFeel.CatalogoNoDisponible.class)
                .hasMessageContaining("No mapeado");
    }
}
