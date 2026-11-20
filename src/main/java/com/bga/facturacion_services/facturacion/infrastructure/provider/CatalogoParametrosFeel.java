package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.bga.facturacion_services.facturacion.infrastructure.provider.CatalogoProductosFeel.CatalogoNoDisponible;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Lee codigoERP de listarParametros para el NIT que emite. */
public final class CatalogoParametrosFeel {
    private static final String PARAMETRO_DOCUMENTO = "TIPO_DOCUMENTO_IDENTIDAD";
    private final RestClient restClient;
    private final ObjectMapper json;
    private final String baseUrl;

    public CatalogoParametrosFeel(RestClient restClient, ObjectMapper json, String baseUrl) {
        this.restClient = restClient;
        this.json = json;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public String codigoDocumento(String nit, int codigoSin) {
        if (baseUrl.isEmpty())
            throw new CatalogoNoDisponible("Falta configurar feel.base-url");
        if (nit == null || nit.isBlank())
            throw new CatalogoNoDisponible("Falta el NIT del emisor");
        String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                + "/ws-catalog/cat/catalogo/listarParametros";
        String cuerpo;
        try {
            cuerpo = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("parametro", PARAMETRO_DOCUMENTO, "nit", nit.trim()))
                    .retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            throw new CatalogoNoDisponible("FEEL respondio " + ex.getStatusCode().value() + " al consultar el tipo de documento");
        } catch (RestClientException ex) {
            throw new CatalogoNoDisponible("No se pudo consultar el tipo de documento en FEEL");
        }
        return codigoErp(cuerpo, json, codigoSin);
    }

    static String codigoErp(String cuerpo, ObjectMapper json, int codigoSin) {
        JsonNode raiz;
        try {
            raiz = json.readTree(cuerpo == null ? "" : cuerpo);
        } catch (Exception ex) {
            throw new CatalogoNoDisponible("FEEL devolvio un catalogo de parametros que no es JSON");
        }
        JsonNode respuesta = raiz.get("respuesta");
        String codigo = texto(respuesta == null ? null : respuesta.get("codRespuesta"));
        if (!"0".equals(codigo)) {
            String detalle = texto(respuesta == null ? null : respuesta.get("txtRespuesta"));
            throw new CatalogoNoDisponible(detalle.isBlank() ? "FEEL rechazo el catalogo de tipo de documento" : "FEEL: " + detalle);
        }
        JsonNode lista = raiz.get("list");
        if (lista == null || !lista.isArray())
            throw new CatalogoNoDisponible("FEEL no devolvio tipos de documento");
        String buscado = String.valueOf(codigoSin);
        for (JsonNode item : lista) {
            if (!buscado.equals(texto(item.get("codigoSin")))) continue;
            String erp = texto(item.get("codigoERP"));
            if (erp.isBlank())
                throw new CatalogoNoDisponible("FEEL no tiene mapeado el tipo de documento " + buscado);
            return erp;
        }
        throw new CatalogoNoDisponible("FEEL no tiene el tipo de documento " + buscado + " para esta empresa");
    }

    private static String texto(JsonNode nodo) {
        if (nodo == null || nodo.isNull()) return "";
        return nodo.asText("").trim();
    }
}
