package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.sucursal.NuevaSucursal;
import com.bga.facturacion_services.facturacion.application.sucursal.Sucursal;
import com.bga.facturacion_services.facturacion.application.sucursal.SucursalRepositorio;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    private static final String COLUMNAS = """
            c.ID_CONFIG, c.ID_EMPRESA, e.NOMBRE, e.NIT, c.CODIGO, c.REFERENCIA, c.SUCURSAL, c.POS,
            c.DIRECCION, c.NRO_DOC_SEQUENCE, c.NRO_NOTA_SEQUENCE, c.ENABLED, c.LAST_USER, c.LAST_STAMP,
            c.HORA_CUFD, c.TELEFONO, c.MUNICIPIO, c.KEYNAME
            """;

    private final DataSource oracle;
    private volatile String secuencia;

    public SucursalOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<Sucursal> listar() {
        String sql = "SELECT " + COLUMNAS + """
                 FROM FACTURACION.F_CONFIG c
                 LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
                 ORDER BY c.ID_EMPRESA, c.SUCURSAL, c.POS
                """;
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            var lista = new ArrayList<Sucursal>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer FACTURACION.F_CONFIG", ex);
        }
    }

    public boolean existeEmpresa(long idEmpresa) {
        return existe("SELECT 1 FROM FACTURACION.F_EMPRESA WHERE ID_EMPRESA = ?", idEmpresa);
    }

    public boolean existePunto(long idEmpresa, int sucursal, int pos) {
        String sql = "SELECT 1 FROM FACTURACION.F_CONFIG WHERE ID_EMPRESA = ? AND SUCURSAL = ? AND POS = ?";
        try (Connection con = oracle.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idEmpresa);
            ps.setInt(2, sucursal);
            ps.setInt(3, pos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar la sucursal en FACTURACION.F_CONFIG", ex);
        }
    }

    public Sucursal insertar(NuevaSucursal sucursal, String usuario) {
        String sql = """
                INSERT INTO FACTURACION.F_CONFIG (
                    ID_CONFIG, ID_EMPRESA, CODIGO, REFERENCIA, SUCURSAL, POS, DIRECCION,
                    NRO_DOC_SEQUENCE, NRO_NOTA_SEQUENCE, ENABLED, LAST_USER, LAST_STAMP,
                    HORA_CUFD, TELEFONO, MUNICIPIO, KEYNAME
                ) VALUES (
                    %s.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, ?, ?, ?
                )
                """.formatted(secuencia());
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, sucursal.idEmpresa());
                ps.setString(i++, sucursal.codigo().trim());
                ps.setString(i++, sucursal.referencia().trim());
                ps.setInt(i++, sucursal.sucursal());
                ps.setInt(i++, sucursal.pos());
                ps.setString(i++, sucursal.direccion().trim());
                ps.setLong(i++, sucursal.nroDocSequence());
                ps.setLong(i++, sucursal.nroNotaSequence());
                ps.setInt(i++, sucursal.enabled());
                ps.setString(i++, usuario);
                entero(ps, i++, sucursal.horaCufd());
                texto(ps, i++, sucursal.telefono());
                texto(ps, i++, sucursal.municipio());
                texto(ps, i, sucursal.keyName());
                ps.executeUpdate();
            }
            Sucursal creada = buscar(con, sucursal.idEmpresa(), sucursal.sucursal(), sucursal.pos());
            con.commit();
            return creada;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar la sucursal en FACTURACION.F_CONFIG", ex);
        }
    }

    private Sucursal buscar(Connection con, long idEmpresa, int sucursal, int pos) throws SQLException {
        String sql = "SELECT " + COLUMNAS + """
                 FROM FACTURACION.F_CONFIG c
                 LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
                 WHERE c.ID_EMPRESA = ? AND c.SUCURSAL = ? AND c.POS = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idEmpresa);
            ps.setInt(2, sucursal);
            ps.setInt(3, pos);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("La sucursal no quedo registrada");
                return mapear(rs);
            }
        }
    }

    private String secuencia() {
        String conocida = secuencia;
        if (conocida != null) return conocida;
        String sql = """
                SELECT sequence_owner || '.' || sequence_name
                FROM all_sequences
                WHERE sequence_owner = 'FACTURACION'
                  AND sequence_name IN ('S_F_CONFIG', 'SEQ_F_CONFIG', 'S_CONFIG')
                ORDER BY CASE sequence_name WHEN 'S_F_CONFIG' THEN 1 WHEN 'SEQ_F_CONFIG' THEN 2 ELSE 3 END
                """;
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("No existe la secuencia de FACTURACION.F_CONFIG");
            secuencia = rs.getString(1);
            return secuencia;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo ubicar la secuencia de FACTURACION.F_CONFIG", ex);
        }
    }

    private boolean existe(String sql, long id) {
        try (Connection con = oracle.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar la empresa de la sucursal", ex);
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

    private static void texto(PreparedStatement ps, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) ps.setNull(indice, Types.VARCHAR);
        else ps.setString(indice, valor.trim());
    }

    private static void entero(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) ps.setNull(indice, Types.NUMERIC);
        else ps.setInt(indice, valor);
    }
}
