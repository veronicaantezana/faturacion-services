package com.bga.facturacion_services.facturacion.application;

import com.bga.facturacion_services.facturacion.domain.*;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EmitirFactura {
    private final Map<String, ReglaFactura> reglas;
    private final FacturaStore store;
    private final ProveedorFacturacion proveedor;
    private final Factura.Emisor emisor;
    private final Clock clock;

    public EmitirFactura(List<ReglaFactura> reglas, FacturaStore store, ProveedorFacturacion proveedor,
                         Factura.Emisor emisor, Clock clock) {
        this.reglas = reglas.stream().collect(Collectors.toUnmodifiableMap(ReglaFactura::tipo, Function.identity()));
        this.store = store; this.proveedor = proveedor; this.emisor = emisor; this.clock = clock;
    }

    public RegistroFactura emitir(String clave, SolicitudFactura solicitud) {
        if (clave == null || !clave.matches("[a-zA-Z0-9_-]{8,100}"))
            throw new IllegalArgumentException("Idempotency-Key requiere entre 8 y 100 caracteres alfanumericos, guion o guion bajo");
        ReglaFactura regla = reglas.get(solicitud.tipo());
        if (regla == null) throw new IllegalArgumentException("Tipo de factura no soportado");
        // Validate before reserving an immutable, unique invoice number.
        regla.construir(UUID.randomUUID(), 0, OffsetDateTime.now(clock), emisor, solicitud);
        FacturaStore.Reserva reserva = store.reservar(clave, solicitud);
        if (!reserva.nueva()) return consultar(reserva.id());
        Factura factura = regla.construir(reserva.id(), reserva.numero(), OffsetDateTime.now(clock), emisor, solicitud);
        RegistroFactura preparada = new RegistroFactura(factura, new ResultadoEmision(ResultadoEmision.Estado.PREPARADA,
                proveedor.codigo(), proveedor.simulado(), null, "Preparada para envio", null, null));
        store.guardar(factura.id(), preparada);
        ResultadoEmision resultado;
        try { resultado = proveedor.emitir(factura); }
        catch (RuntimeException ex) {
            resultado = new ResultadoEmision(ResultadoEmision.Estado.PENDIENTE_CONFIRMACION,
                    proveedor.codigo(), proveedor.simulado(), null, "Resultado no confirmado; no reenviar automaticamente", null, null);
        }
        RegistroFactura registro = new RegistroFactura(factura, resultado);
        store.guardar(factura.id(), registro);
        return registro;
    }
    public RegistroFactura consultar(UUID id) {
        return store.buscar(id).orElseThrow(() -> new FacturaNoEncontrada(id));
    }
    public static final class FacturaNoEncontrada extends RuntimeException {
        public FacturaNoEncontrada(UUID id) { super("Factura no encontrada: " + id); }
    }
}

