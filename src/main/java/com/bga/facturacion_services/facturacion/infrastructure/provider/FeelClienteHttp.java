package com.bga.facturacion_services.facturacion.infrastructure.provider;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

public final class FeelClienteHttp {
    private static final String RUTA_SINCRONA = "ws-send-docfiscal/rest/ws-send-docfiscal/enviarsync"; 
    private static final String RUTA_ASINCRONA = "ws-send-docfiscal/rest/ws-send-docfiscal/enviar";
    private final RestClient restClient;
    private final String baseUrl;
    private final boolean sincrono;

    public FeelClienteHttp(RestClient restClient, String baseUrl, boolean sincrono) {
        this.restClient = restClient;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.sincrono = sincrono;
    }

    public FeelRespuesta enviar(Object documento) {
        if (baseUrl.isEmpty())
            throw new IllegalArgumentException("Falta configurar feel.base-url con la URL de FEEL");
        String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                + "/" + (sincrono ? RUTA_SINCRONA : RUTA_ASINCRONA);
        String cuerpo;
        try {
            cuerpo = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(documento)
                    .retrieve().body(String.class);
        } catch (RestClientResponseException ex) {
            throw new FeelRechazoException("FEEL respondio " + ex.getStatusCode().value() + ": " + recortar(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            throw ex;
        }
        if (cuerpo == null || cuerpo.isBlank()) throw new FeelRechazoException("FEEL no devolvio contenido");
        return interpretar(cuerpo);
    }

    @SuppressWarnings("unchecked")
    static FeelRespuesta interpretar(String cuerpo) {
        Map<String, Object> raiz;
        try { raiz = new org.springframework.boot.json.JacksonJsonParser().parseMap(cuerpo); }
        catch (RuntimeException ex) { throw new FeelRechazoException("FEEL devolvio un contenido que no es JSON"); }
        Map<String, Object> respuesta = mapa(raiz.get("respuesta"));
        Map<String, Object> proceso = mapa(raiz.get("proceso"));
        String codigo = texto(respuesta == null ? null : respuesta.get("codRespuesta"));
        String texto = texto(respuesta == null ? null : respuesta.get("txtRespuesta"));
        if (!"0".equals(codigo) || proceso == null)
            throw new FeelRechazoException(texto.isBlank() ? "FEEL rechazo la factura" : "FEEL: " + texto);
        Integer recepcion = entero(proceso.get("codigoRecepcion"));
        if (recepcion != null && recepcion != 0 && recepcion != 908) {
            String detalle = mensajes(proceso.get("listaMensajes"));
            throw new FeelRechazoException(detalle.isBlank() ? "FEEL rechazo la factura" : "FEEL: " + detalle);
        }
        return new FeelRespuesta(primero(proceso, "cuf", "Cuf"), texto(proceso.get("cufd")),
                texto(proceso.get("idDocFiscalFEEL")), esVerdadero(proceso.get("fueraLinea")),
                texto.isBlank() ? "Factura enviada a FEEL" : texto);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : null;
    }

    private static String mensajes(Object valor) {
        if (!(valor instanceof List<?> lista)) return "";
        StringBuilder texto = new StringBuilder();
        for (Object item : lista) {
            Map<String, Object> mensaje = mapa(item);
            if (mensaje == null) continue;
            String descripcion = texto(mensaje.get("descripcion"));
            if (descripcion.isBlank()) continue;
            if (!texto.isEmpty()) texto.append(' ');
            texto.append(descripcion);
        }
        return texto.toString();
    }

    private static String primero(Map<String, Object> mapa, String... claves) {
        for (String clave : claves) {
            String valor = texto(mapa.get(clave));
            if (!valor.isBlank()) return valor;
        }
        return "";
    }

    private static String texto(Object valor) { return valor == null ? "" : String.valueOf(valor).trim(); }

    private static Integer entero(Object valor) {
        if (valor instanceof Number numero) return numero.intValue();
        if (valor == null || String.valueOf(valor).isBlank()) return null;
        return Integer.valueOf(String.valueOf(valor));
    }

    private static boolean esVerdadero(Object valor) {
        return Boolean.TRUE.equals(valor) || "true".equalsIgnoreCase(texto(valor));
    }

    private static String recortar(String texto) {
        if (texto == null || texto.isBlank()) return "sin detalle";
        String limpio = texto.replaceAll("\\s+", " ").trim();
        return limpio.length() > 300 ? limpio.substring(0, 300) : limpio;
    }

    public record FeelRespuesta(String cuf, String cufd, String idDocFiscalFeel, boolean fueraLinea, String mensaje) {}

    public static final class FeelRechazoException extends RuntimeException {
        public FeelRechazoException(String mensaje) { super(mensaje); }
    }
}
