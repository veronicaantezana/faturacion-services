package com.bga.facturacion_services.facturacion.infrastructure.provider.feel;

import java.math.BigDecimal;

final class FeelConversiones {
    private FeelConversiones() {}

    static String tipoDocumentoIdentidad(int codigo) {
        return switch (codigo) {
            case 1 -> "CI";
            case 2 -> "CIEPE";
            case 3 -> "PAS";
            case 4 -> "OTDPREECPJ";
            case 5 -> "NITRUC";
            default -> String.valueOf(codigo);
        };
    }

    /** codigoERP de listarParametros TIPO_MONEDA para el NIT del banco. El 1 de la pantalla es el codigoSin. */
    static String tipoMoneda(int codigoSin) {
        return switch (codigoSin) {
            case 1 -> "0";
            case 2 -> "2225";
            default -> String.valueOf(codigoSin);
        };
    }

    static String tipoDocumentoNumerico(int codigo) {
        return String.valueOf(codigo);
    }

    static BigDecimal ceroNulo(BigDecimal valor) {
        return valor == null || valor.signum() == 0 ? null : valor;
    }

    static String vacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static long tarjeta(String valor) {
        if (valor == null || valor.isBlank() || !valor.chars().allMatch(Character::isDigit)) return 0L;
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    static long nit(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El NIT del emisor debe ser numerico");
        }
    }
}
