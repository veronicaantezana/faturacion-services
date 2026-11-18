package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cabecera FEEL en el orden en que se declaran los campos. El XSD rechaza otro orden. */
final class CabeceraFeel {
    private final LinkedHashMap<String, Object> campos = new LinkedHashMap<>();

    CabeceraFeel poner(String clave, Object valor) {
        campos.put(clave, valor);
        return this;
    }

    Map<String, Object> campos() {
        return campos;
    }
}
