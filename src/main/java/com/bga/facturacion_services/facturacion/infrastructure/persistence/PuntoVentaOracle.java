package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.puntoventa.NuevoPuntoVenta;
import com.bga.facturacion_services.facturacion.application.puntoventa.PuntoVenta;
import com.bga.facturacion_services.facturacion.application.puntoventa.PuntoVentaRepositorio;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class PuntoVentaOracle implements PuntoVentaRepositorio {
    private static final int CURSOR = -10;
    private static final String LISTAR = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_PUNTOS_VENTA(?,?,?) }";
    private static final String LISTAR_SUCURSALES = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_SUCURSALES(?,?,?) }";
    private static final String REGISTRAR = "{ call FACTURACION.PKG_REGISTRO.SPR_REGISTRAR_PUNTO_VENTA("
            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

    private final DataSource oracle;

    public PuntoVentaOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<PuntoVenta> listar() {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR)) {
            llamada.registerOutParameter(1, CURSOR);
            llamada.registerOutParameter(2, Types.NUMERIC);
            llamada.registerOutParameter(3, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 2, 3);
            try (ResultSet rs = (ResultSet) llamada.getObject(1)) {
                var lista = new ArrayList<PuntoVenta>();
                while (rs.next()) lista.add(mapear(rs));
                return lista;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer los puntos de venta por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public SucursalVinculo buscarSucursal(long idConfig) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR_SUCURSALES)) {
            llamada.registerOutParameter(1, CURSOR);
            llamada.registerOutParameter(2, Types.NUMERIC);
            llamada.registerOutParameter(3, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 2, 3);
            try (ResultSet rs = (ResultSet) llamada.getObject(1)) {
                while (rs.next()) {
                    if (rs.getLong("ID_CONFIG") == idConfig) {
                        return new SucursalVinculo(idConfig, rs.getInt("SUCURSAL"), rs.getInt("POS"));
                    }
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar la sucursal por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public boolean existeParaConfig(long idConfig) {
        return listar().stream().anyMatch(punto -> punto.idConfig() == idConfig);
    }

    public PuntoVenta insertar(NuevoPuntoVenta punto, String usuario) {
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (CallableStatement llamada = con.prepareCall(REGISTRAR)) {
                llamada.setLong(1, punto.idConfig());
                texto(llamada, 2, punto.descripcion());
                llamada.setInt(3, punto.pos());
                llamada.setInt(4, punto.tipoPuntoVenta());
                texto(llamada, 5, punto.nombrePuntoVenta());
                llamada.setInt(6, punto.tipoPing());
                llamada.setInt(7, punto.enabled());
                texto(llamada, 8, usuario);
                texto(llamada, 9, punto.direccionIp());
                texto(llamada, 10, punto.numeroContrato());
                texto(llamada, 11, punto.nitComisionista());
                fecha(llamada, 12, punto.fechaInicio());
                fecha(llamada, 13, punto.fechaFin());
                entero(llamada, 14, punto.obtenerCufd());
                entero(llamada, 15, punto.estadoSiat());
                llamada.registerOutParameter(16, Types.NUMERIC);
                llamada.registerOutParameter(17, Types.NUMERIC);
                llamada.registerOutParameter(18, Types.VARCHAR);
                llamada.execute();
                exigir(llamada, 17, 18);
                long idPos = llamada.getLong(16);
                con.commit();
                return listar().stream()
                        .filter(fila -> fila.idPos() == idPos)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("El punto de venta no quedo registrado"));
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el punto de venta por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    private static void exigir(CallableStatement llamada, int codigo, int descripcion) throws SQLException {
        int resultado = llamada.getInt(codigo);
        if (llamada.wasNull() || resultado != 0) {
            throw new IllegalStateException(llamada.getString(descripcion));
        }
    }

    private static PuntoVenta mapear(ResultSet rs) throws SQLException {
        Timestamp sello = rs.getTimestamp("LAST_STAMP");
        return new PuntoVenta(
                rs.getLong("ID_POS"),
                rs.getLong("ID_CONFIG"),
                largo(rs, "ID_EMPRESA"),
                rs.getString("NOMBRE"),
                rs.getString("NIT"),
                rs.getString("CODIGO"),
                rs.getString("REFERENCIA"),
                entero(rs, "SUCURSAL"),
                rs.getString("DESCRIPCION"),
                rs.getString("DIRECCION_IP"),
                rs.getInt("POS"),
                entero(rs, "TIPO_PUNTO_VENTA"),
                rs.getString("NOMBRE_PUNTO_VENTA"),
                entero(rs, "TIPO_PING"),
                entero(rs, "ENABLED"),
                rs.getString("LAST_USER"),
                sello == null ? null : sello.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                rs.getString("NUMERO_CONTRATO"),
                rs.getString("NIT_COMISIONISTA"),
                fecha(rs, "FECHA_INICIO"),
                fecha(rs, "FECHA_FIN"),
                entero(rs, "OBTENER_CUFD"),
                entero(rs, "ESTADO_SIAT"));
    }

    private static Long largo(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    private static Integer entero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        Date valor = rs.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }

    private static void texto(CallableStatement llamada, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) llamada.setNull(indice, Types.VARCHAR);
        else llamada.setString(indice, valor.trim());
    }

    private static void entero(CallableStatement llamada, int indice, Integer valor) throws SQLException {
        if (valor == null) llamada.setNull(indice, Types.NUMERIC);
        else llamada.setInt(indice, valor);
    }

    private static void fecha(CallableStatement llamada, int indice, LocalDate valor) throws SQLException {
        if (valor == null) llamada.setNull(indice, Types.DATE);
        else llamada.setDate(indice, Date.valueOf(valor));
    }
}
