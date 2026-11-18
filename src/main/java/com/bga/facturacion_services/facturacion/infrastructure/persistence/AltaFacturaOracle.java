package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.AltaFacturaWeb;
import com.bga.facturacion_services.facturacion.application.ResultadoEmision;
import com.bga.facturacion_services.facturacion.domain.Factura;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;

/** Llama a GANADERO.SPR_REGISTRAR_FACTURA_WEB y confirma la transaccion antes de FEEL. */
public final class AltaFacturaOracle implements AltaFacturaWeb {
    private static final String LLAMADA = "{ call GANADERO.SPR_REGISTRAR_FACTURA_WEB("
            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
    private static final String ACTUALIZAR = "{ call GANADERO.SPR_ACTUALIZAR_RESPUESTA_FEEL(?,?,?,?,?,?,?) }";
    private final DataSource oracle;
    private final ObjectMapper json;
    private final String codigoSistemaOrigen;
    private final int codigoCanal;
    private final int codigoTipoFactura;

    public AltaFacturaOracle(DataSource oracle, ObjectMapper json, String codigoSistemaOrigen,
                             int codigoCanal, int codigoTipoFactura) {
        this.oracle = oracle;
        this.json = json;
        this.codigoSistemaOrigen = codigoSistemaOrigen;
        this.codigoCanal = codigoCanal;
        this.codigoTipoFactura = codigoTipoFactura;
    }

    public Resultado registrar(Factura factura) {
        try (Connection conexion = oracle.getConnection()) {
            conexion.setAutoCommit(false);
            try (CallableStatement llamada = conexion.prepareCall(LLAMADA)) {
                cargar(llamada, factura);
                llamada.execute();
                String codigo = llamada.getString(33);
                String descripcion = llamada.getString(34);
                if (!"COD000".equals(codigo)) {
                    conexion.rollback();
                    throw new Rechazo(codigo + (descripcion == null || descripcion.isBlank() ? "" : " " + descripcion));
                }
                long complemento = llamada.getLong(31);
                long solicitud = llamada.getLong(32);
                conexion.commit();
                return new Resultado(complemento, solicitud);
            } catch (Rechazo ex) {
                throw ex;
            } catch (RuntimeException ex) {
                conexion.rollback();
                throw ex;
            }
        } catch (Rechazo ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo registrar la factura en Oracle", ex);
        }
    }

    public void guardarRespuestaFeel(Resultado alta, ResultadoEmision emision) {
        boolean enviado = emision.estado() == ResultadoEmision.Estado.ACEPTADA
                || emision.estado() == ResultadoEmision.Estado.CONTINGENCIA;
        try (Connection conexion = oracle.getConnection()) {
            conexion.setAutoCommit(false);
            try (CallableStatement llamada = conexion.prepareCall(ACTUALIZAR)) {
                llamada.setLong(1, alta.idComplemento());
                llamada.setLong(2, alta.nroSolicitud());
                llamada.setInt(3, enviado ? 1 : 0);
                texto(llamada, 4, emision.mensaje());
                if (emision.respuestaJson() == null || emision.respuestaJson().isBlank()) llamada.setNull(5, Types.CLOB);
                else llamada.setString(5, emision.respuestaJson());
                llamada.registerOutParameter(6, Types.VARCHAR);
                llamada.registerOutParameter(7, Types.VARCHAR);
                llamada.execute();
                String codigo = llamada.getString(6);
                String descripcion = llamada.getString(7);
                if (!"COD000".equals(codigo)) {
                    conexion.rollback();
                    throw new IllegalStateException(codigo + (descripcion == null || descripcion.isBlank() ? "" : " " + descripcion));
                }
                conexion.commit();
            } catch (RuntimeException ex) {
                conexion.rollback();
                throw ex;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar la respuesta de FEEL en CI_FACTURA_COMPLEMENTO", ex);
        }
    }

    private void cargar(CallableStatement llamada, Factura factura) throws Exception {
        var emisor = factura.emisor();
        var cliente = factura.cliente();
        var venta = factura.venta();
        var feel = factura.feel();
        llamada.setLong(1, Long.parseLong(emisor.nit()));
        texto(llamada, 2, emisor.razonSocial());
        llamada.setInt(3, factura.codigoDocumentoSector());
        llamada.setInt(4, codigoTipoFactura);
        llamada.setInt(5, emisor.sucursal());
        llamada.setInt(6, emisor.puntoVenta());
        texto(llamada, 7, emisor.municipio());
        texto(llamada, 8, cliente.razonSocial());
        llamada.setInt(9, cliente.tipoDocumento());
        texto(llamada, 10, cliente.documento());
        texto(llamada, 11, cliente.complemento());
        texto(llamada, 12, cliente.documento());
        llamada.setInt(13, factura.metodoPago());
        numero(llamada, 14, venta.numeroTarjeta());
        llamada.setInt(15, venta.codigoMoneda());
        decimal(llamada, 16, venta.tipoCambio());
        decimal(llamada, 17, feel.tipoCambioOficial() == null ? venta.tipoCambio() : feel.tipoCambioOficial());
        decimal(llamada, 18, factura.total());
        decimal(llamada, 19, venta.montoTotalMoneda());
        decimal(llamada, 20, factura.totalSujetoIva());
        llamada.setInt(21, feel.codigoTipoOperacion() == null ? 0 : feel.codigoTipoOperacion());
        decimal(llamada, 22, feel.ingresoDiferenciaCambio() == null ? BigDecimal.ZERO : feel.ingresoDiferenciaCambio());
        texto(llamada, 23, venta.correo());
        llamada.setNull(24, Types.VARCHAR);
        if (venta.codigoExcepcion() == null) llamada.setInt(25, 0);
        else llamada.setInt(25, venta.codigoExcepcion());
        texto(llamada, 26, venta.cafc());
        llamada.setString(27, json.writeValueAsString(detalle(factura)));
        texto(llamada, 28, codigoSistemaOrigen);
        llamada.setInt(29, codigoCanal);
        texto(llamada, 30, factura.usuario());
        llamada.registerOutParameter(31, Types.NUMERIC);
        llamada.registerOutParameter(32, Types.NUMERIC);
        llamada.registerOutParameter(33, Types.VARCHAR);
        llamada.registerOutParameter(34, Types.VARCHAR);
    }

    private java.util.List<Map<String, Object>> detalle(Factura factura) {
        var lineas = new ArrayList<Map<String, Object>>();
        for (var linea : factura.detalle()) {
            var item = new LinkedHashMap<String, Object>();
            item.put("actividadEconomica", linea.item().actividad());
            item.put("codigoProductoSin", linea.item().codigoSin());
            item.put("codigoProducto", linea.item().codigo());
            item.put("descripcion", linea.item().descripcion());
            item.put("cantidad", linea.item().cantidad());
            item.put("unidadMedida", linea.item().unidadMedida());
            item.put("precioUnitario", linea.item().precioUnitario());
            item.put("montoDescuento", linea.item().descuento() == null ? BigDecimal.ZERO : linea.item().descuento());
            item.put("subTotal", linea.subtotal());
            lineas.add(item);
        }
        return lineas;
    }

    private static void texto(CallableStatement llamada, int indice, String valor) throws Exception {
        if (valor == null || valor.isBlank()) llamada.setNull(indice, Types.VARCHAR);
        else llamada.setString(indice, valor.trim());
    }

    private static void decimal(CallableStatement llamada, int indice, BigDecimal valor) throws Exception {
        if (valor == null) llamada.setNull(indice, Types.NUMERIC);
        else llamada.setBigDecimal(indice, valor);
    }

    private static void numero(CallableStatement llamada, int indice, String valor) throws Exception {
        if (valor == null || !valor.chars().allMatch(Character::isDigit)) llamada.setLong(indice, 0);
        else llamada.setLong(indice, Long.parseLong(valor));
    }
}
