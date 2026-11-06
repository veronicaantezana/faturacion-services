package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.*;
import com.bga.facturacion_services.facturacion.domain.SolicitudFactura;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository
public class JpaFacturaStore implements FacturaStore {
    private final EntityManager em;
    private final ObjectMapper json;
    public JpaFacturaStore(EntityManager em, ObjectMapper json) { this.em = em; this.json = json; }

    @Transactional
    public Reserva reservar(String clave, SolicitudFactura solicitud) {
        String contenido = escribir(solicitud);
        var existentes = em.createQuery("select f from FacturaEntity f where f.clave = :clave", FacturaEntity.class)
                .setParameter("clave", clave).getResultList();
        if (!existentes.isEmpty()) {
            var existente = existentes.get(0);
            if (!existente.solicitud.equals(contenido))
                throw new Conflicto("La clave ya fue utilizada con otros datos");
            if (existente.registro == null)
                throw new Conflicto("La factura esta en proceso; consulte nuevamente");
            return new Reserva(UUID.fromString(existente.uuid), existente.numero, false);
        }
        var entidad = new FacturaEntity();
        entidad.uuid = UUID.randomUUID().toString(); entidad.clave = clave; entidad.solicitud = contenido;
        em.persist(entidad); em.flush();
        return new Reserva(UUID.fromString(entidad.uuid), entidad.numero, true);
    }

    @Transactional
    public void guardar(UUID id, RegistroFactura registro) {
        var entidad = em.createQuery("select f from FacturaEntity f where f.uuid = :id", FacturaEntity.class)
                .setParameter("id", id.toString()).getSingleResult();
        entidad.registro = escribir(registro);
    }
    @Transactional(readOnly = true)
    public Optional<RegistroFactura> buscar(UUID id) {
        return em.createQuery("select f from FacturaEntity f where f.uuid = :id", FacturaEntity.class)
                .setParameter("id", id.toString()).getResultStream()
                .filter(e -> e.registro != null).findFirst().map(e -> leer(e.registro));
    }
    private String escribir(Object valor) {
        try { return json.writeValueAsString(valor); }
        catch (Exception ex) { throw new IllegalStateException("No se pudo serializar la factura", ex); }
    }
    private RegistroFactura leer(String valor) {
        try { return json.readValue(valor, RegistroFactura.class); }
        catch (Exception ex) { throw new IllegalStateException("No se pudo leer la factura", ex); }
    }
    public static class Conflicto extends RuntimeException {
        public Conflicto(String mensaje) { super(mensaje); }
    }
}
