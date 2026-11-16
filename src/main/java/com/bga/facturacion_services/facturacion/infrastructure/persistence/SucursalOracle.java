package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.sucursal.NuevaSucursal;
import com.bga.facturacion_services.facturacion.application.sucursal.Sucursal;
import com.bga.facturacion_services.facturacion.application.sucursal.SucursalRepositorio;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class SucursalOracle implements SucursalRepositorio {
    private static final int CURSOR = -10;
    private static final String LISTAR = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_SUCURSALES(?,?,?) }";
    private static final String LISTAR_EMPRESAS = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_EMPRESAS(?,?,?) }";
    private static final String REGISTRAR = "{ call FACTURACION.PKG_REGISTRO.SPR_REGISTRAR_SUCURSAL("
            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

    private final DataSource oracle;

    public SucursalOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<Sucursal> listar() {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR)) {
            llamada.registerOutParameter(1, CURSOR);
            llamada.registerOutParameter(2, Types.NUMERIC);
            llamada.registerOutParameter(3, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 2, 3);
            try (ResultSet rs = (ResultSet) llamada.getObject(1)) {
                var lista = new ArrayList<Sucursal>();
                while (rs.next()) lista.add(mapear(rs));
                return lista;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer las sucursales por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public boolean existeEmpresa(long idEmpresa) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR_EMPRESAS)) {
            llamada.registerOutParameter(1, CURSOR);
            llamada.registerOutParameter(2, Types.NUMERIC);
            llamada.registerOutParameter(3, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 2, 3);
            try (ResultSet rs = (ResultSet) llamada.getObject(1)) {
                while (rs.next()) {
                    if (rs.getLong("ID_EMPRESA") == idEmpresa) return true;
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar la empresa por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public boolean existePunto(long idEmpresa, int sucursal, int pos) {
        return listar().stream().anyMatch(fila ->
                fila.idEmpresa() == idEmpresa && fila.sucursal() == sucursal && fila.pos() == pos);
    }

    public Sucursal insertar(NuevaSucursal sucursal, String usuario) {
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (CallableStatement llamada = con.prepareCall(REGISTRAR)) {
                llamada.setLong(1, sucursal.idEmpresa());
                texto(llamada, 2, sucursal.codigo());
                texto(llamada, 3, sucursal.referencia());
                llamada.setInt(4, sucursal.sucursal());
                llamada.setInt(5, sucursal.pos());
                texto(llamada, 6, sucursal.direccion());
                llamada.setLong(7, sucursal.nroDocSequence());
                llamada.setLong(8, sucursal.nroNotaSequence());
                llamada.setInt(9, sucursal.enabled());
                texto(llamada, 10, usuario);
                entero(llamada, 11, sucursal.horaCufd());
                texto(llamada, 12, sucursal.telefono());
                texto(llamada, 13, sucursal.municipio());
                texto(llamada, 14, sucursal.keyName());
                llamada.registerOutParameter(15, Types.NUMERIC);
                llamada.registerOutParameter(16, Types.NUMERIC);
                llamada.registerOutParameter(17, Types.VARCHAR);
                llamada.execute();
                exigir(llamada, 16, 17);
                long idConfig = llamada.getLong(15);
                con.commit();
                return listar().stream()
                        .filter(fila -> fila.idConfig() == idConfig)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("La sucursal no quedo registrada"));
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar la sucursal por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    private static void exigir(CallableStatement llamada, int codigo, int descripcion) throws SQLException {
        int resultado = llamada.getInt(codigo);
        if (llamada.wasNull() || resultado != 0) {
            throw new IllegalStateException(llamada.getString(descripcion));
        }
    }

    private static Sucursal mapear(ResultSet rs) throws SQLException {
        Timestamp sello = rs.getTimestamp("LAST_STAMP");
        return new Sucursal(
                rs.getLong("ID_CONFIG"),
                rs.getLong("ID_EMPRESA"),
                rs.getString("NOMBRE"),
                rs.getString("NIT"),
                rs.getString("CODIGO"),
                rs.getString("REFERENCIA"),
                rs.getInt("SUCURSAL"),
                rs.getInt("POS"),
                rs.getString("DIRECCION"),
                largo(rs, "NRO_DOC_SEQUENCE"),
                largo(rs, "NRO_NOTA_SEQUENCE"),
                entero(rs, "ENABLED"),
                rs.getString("LAST_USER"),
                sello == null ? null : sello.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                entero(rs, "HORA_CUFD"),
                rs.getString("TELEFONO"),
                rs.getString("MUNICIPIO"),
                rs.getString("KEYNAME"));
    }

    private static Long largo(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    private static Integer entero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static void texto(CallableStatement llamada, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) llamada.setNull(indice, Types.VARCHAR);
        else llamada.setString(indice, valor.trim());
    }

    private static void entero(CallableStatement llamada, int indice, Integer valor) throws SQLException {
        if (valor == null) llamada.setNull(indice, Types.NUMERIC);
        else llamada.setInt(indice, valor);
    }
}
