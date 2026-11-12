package com.bga.facturacion_services.facturacion.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Lee el catálogo de productos que FEEL ya tiene habilitado para el NIT. */
public final class CatalogoProductosFeel {
    private final RestClient restClient;
    private final ObjectMapper json;
    private final String baseUrl;
    private final String nit;

    public CatalogoProductosFeel(RestClient restClient, ObjectMapper json, String baseUrl, String nit) {
        this.restClient = restClient;
        this.json = json;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.nit = nit == null ? "" : nit.trim();
    }

    public List<ProductoCatalogo> listar() {
        if (baseUrl.isEmpty())
            throw new CatalogoNoDisponible("Falta configurar feel.base-url");
        if (nit.isEmpty())
            throw new CatalogoNoDisponible("Falta configurar el NIT del emisor");
        String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                + "/ws-catalog/cat/catalogo/listarProductos";
        String cuerpo;
        try {
            cuerpo = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("nit", nit)).retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            throw new CatalogoNoDisponible("FEEL respondio " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            throw new CatalogoNoDisponible("No se pudo consultar el catalogo de productos de FEEL");
        }
        if (cuerpo == null || cuerpo.isBlank())
            throw new CatalogoNoDisponible("FEEL no devolvio el catalogo de productos");
        return interpretar(cuerpo, json);
    }

    static List<ProductoCatalogo> interpretar(String cuerpo, ObjectMapper json) {
        JsonNode raiz;
        try {
            raiz = json.readTree(cuerpo);
        } catch (Exception ex) {
            throw new CatalogoNoDisponible("FEEL devolvio un catalogo que no es JSON");
        }
        JsonNode respuesta = raiz.get("respuesta");
        String codigo = texto(respuesta == null ? null : respuesta.get("codRespuesta"));
        if (!"0".equals(codigo)) {
            String detalle = texto(respuesta == null ? null : respuesta.get("txtRespuesta"));
            throw new CatalogoNoDisponible(detalle.isBlank() ? "FEEL rechazo el catalogo de productos" : "FEEL: " + detalle);
        }
        JsonNode lista = raiz.get("list");
        if (lista == null || !lista.isArray()) return List.of();
        var unicos = new LinkedHashMap<String, ProductoCatalogo>();
        long id = 1;
        for (JsonNode item : lista) {
            String actividad = texto(item.get("codigoActividad"));
            String codigoSin = texto(item.get("codigoSin"));
            String erp = codigoInterno(texto(item.get("codigoERP")), codigoSin);
            String descripcion = texto(item.get("descripcion"));
            if (codigoSin.isBlank() || descripcion.isBlank()) continue;
            String clave = actividad + "|" + codigoSin + "|" + erp + "|" + descripcion;
            if (unicos.containsKey(clave)) continue;
            unicos.put(clave, new ProductoCatalogo(id++, erp, descripcion, null, actividad,
                    Long.parseLong(codigoSin), null, null, true));
        }
        return List.copyOf(unicos.values());
    }

    /** codigoERP a veces llega con una coma final. Si no hay ERP, el codigo SIN identifica el producto. */
    private static String codigoInterno(String erp, String codigoSin) {
        String limpio = erp.replace(",", "").trim();
        return limpio.isEmpty() ? codigoSin : limpio;
    }

    private static String texto(JsonNode nodo) {
        if (nodo == null || nodo.isNull()) return "";
        return nodo.asText("").trim();
    }

    public record ProductoCatalogo(long id, String codigoInterno, String descripcion, BigDecimal precioUnitario,
                                   String codigoActividad, long codigoProductoSin, Integer codigoUnidadMedida,
                                   String unidadMedidaDescripcion, boolean activo) {}

    public static final class CatalogoNoDisponible extends RuntimeException {
        public CatalogoNoDisponible(String mensaje) { super(mensaje); }
    }
}
