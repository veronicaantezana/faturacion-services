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
    private final AltaFacturaWeb altaOracle;
    private final MapeoParametrosFeel mapeo;

    public EmitirFactura(List<ReglaFactura> reglas, FacturaStore store, ProveedorFacturacion proveedor,
                         Factura.Emisor emisor, Clock clock, AltaFacturaWeb altaOracle, MapeoParametrosFeel mapeo) {
        this.reglas = reglas.stream().collect(Collectors.toUnmodifiableMap(ReglaFactura::tipo, Function.identity()));
        this.store = store; this.proveedor = proveedor; this.emisor = emisor; this.clock = clock;
        this.altaOracle = altaOracle;
        this.mapeo = mapeo;
    }

    public RegistroFactura emitir(String clave, SolicitudFactura solicitud) {
        if (clave == null || !clave.matches("[a-zA-Z0-9_-]{8,100}"))
            throw new IllegalArgumentException("Idempotency-Key requiere entre 8 y 100 caracteres alfanumericos, guion o guion bajo");
        ReglaFactura regla = reglas.get(solicitud.tipo());
        if (regla == null) throw new IllegalArgumentException("Tipo de factura no soportado");
        Factura.Emisor emisorFactura = emisorDe(solicitud);
        // Validate before reserving an immutable, unique invoice number.
        regla.construir(UUID.randomUUID(), 0, OffsetDateTime.now(clock), emisorFactura, solicitud);
        FacturaStore.Reserva reserva = store.reservar(clave, solicitud);
        if (!reserva.nueva()) return consultar(reserva.id());
        Factura factura = regla.construir(reserva.id(), reserva.numero(), OffsetDateTime.now(clock), emisorFactura, solicitud);
        if (solicitud.idEmpresa() != null) {
            MapeoParametrosFeel.Codigos codigos = mapeo.consultar(solicitud.idEmpresa(),
                    solicitud.cliente().tipoDocumento(), solicitud.metodoPago(),
                    solicitud.codigoMoneda() == null ? 1 : solicitud.codigoMoneda());
            factura = factura.conCodigosEmision(codigos.tipoDocumento(), codigos.metodoPago(),
                    codigos.moneda(), codigos.codigoSistema());
        }
        AltaFacturaWeb.Resultado alta = altaOracle.registrar(factura);
        RegistroFactura preparada = new RegistroFactura(factura, new ResultadoEmision(ResultadoEmision.Estado.PREPARADA,
                proveedor.codigo(), proveedor.simulado(), null, "Preparada para envio", null, null, null, null, null));
        store.guardar(factura.id(), preparada);
        ResultadoEmision resultado;
        try { resultado = proveedor.emitir(factura); }
        catch (IllegalArgumentException ex) { throw ex; }
        catch (RuntimeException ex) {
            String motivo = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "No hubo respuesta de FEEL" : ex.getMessage();
            resultado = new ResultadoEmision(ResultadoEmision.Estado.PENDIENTE_CONFIRMACION,
                    proveedor.codigo(), proveedor.simulado(), null, "No se envió a FEEL: " + motivo,
                    null, null, null, null, null);
        }
        if (alta != null) {
            try { altaOracle.guardarRespuestaFeel(alta, resultado); }
            catch (RuntimeException ex) {
                String aviso = ex.getMessage() == null ? "No se pudo guardar la respuesta en Oracle" : ex.getMessage();
                resultado = new ResultadoEmision(resultado.estado(), resultado.proveedor(), resultado.simulado(),
                        resultado.referencia(), resultado.mensaje() + " " + aviso, resultado.formato(),
                        resultado.solicitud(), resultado.cuf(), resultado.cufd(), resultado.respuestaJson());
            }
        }
        RegistroFactura registro = new RegistroFactura(factura, resultado, alta);
        store.guardar(factura.id(), registro);
        return registro;
    }
    public RegistroFactura consultar(UUID id) {
        return store.buscar(id).orElseThrow(() -> new FacturaNoEncontrada(id));
    }
    private Factura.Emisor emisorDe(SolicitudFactura solicitud) {
        if (solicitud.nitEmisor() == null || solicitud.nitEmisor().isBlank()
                || solicitud.razonSocialEmisor() == null || solicitud.razonSocialEmisor().isBlank())
            throw new IllegalArgumentException("Falta el NIT y la razon social de la empresa");
        if (solicitud.sucursal() == null || solicitud.puntoVenta() == null || solicitud.sucursal() < 0 || solicitud.puntoVenta() < 0)
            throw new IllegalArgumentException("Sucursal y punto de venta invalidos");
        if (solicitud.municipio() == null || solicitud.municipio().isBlank())
            throw new IllegalArgumentException("Falta el municipio de la sucursal");
        String telefono = solicitud.telefono() == null || solicitud.telefono().isBlank() ? null : solicitud.telefono().trim();
        return new Factura.Emisor(solicitud.nitEmisor().trim(), solicitud.razonSocialEmisor().trim(),
                solicitud.municipio().trim(), solicitud.direccion(), solicitud.sucursal(), solicitud.puntoVenta(),
                telefono);
    }
    public static final class FacturaNoEncontrada extends RuntimeException {
        public FacturaNoEncontrada(UUID id) { super("Factura no encontrada: " + id); }
        public FacturaNoEncontrada(long numero) { super("Factura no encontrada: " + numero); }
    }
}

