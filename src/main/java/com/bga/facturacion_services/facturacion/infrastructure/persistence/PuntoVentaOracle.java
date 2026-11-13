package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.puntoventa.NuevoPuntoVenta;
import com.bga.facturacion_services.facturacion.application.puntoventa.PuntoVenta;
import com.bga.facturacion_services.facturacion.application.puntoventa.PuntoVentaRepositorio;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
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
    private static final String COLUMNAS = """
            p.ID_POS, p.ID_CONFIG, c.ID_EMPRESA, e.NOMBRE, e.NIT, c.CODIGO, c.REFERENCIA, c.SUCURSAL,
            p.DESCRIPCION, p.DIRECCION_IP, p.POS, p.TIPO_PUNTO_VENTA, p.NOMBRE_PUNTO_VENTA, p.TIPO_PING,
            p.ENABLED, p.LAST_USER, p.LAST_STAMP, p.NUMERO_CONTRATO, p.NIT_COMISIONISTA,
            p.FECHA_INICIO, p.FECHA_FIN, p.OBTENER_CUFD, p.ESTADO_SIAT
            """;

    private final DataSource oracle;
    private volatile String secuencia;

    public PuntoVentaOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<PuntoVenta> listar() {
        String sql = "SELECT " + COLUMNAS + """
                 FROM FACTURACION.F_POS p
                 LEFT JOIN FACTURACION.F_CONFIG c ON c.ID_CONFIG = p.ID_CONFIG
                 LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
                 ORDER BY p.ID_POS DESC
                """;
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            var lista = new ArrayList<PuntoVenta>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } catch (SQLException ex) {
            throw new ConsultaRechazada("No se pudo leer FACTURACION.F_POS. " + ex.getMessage()
                    + ". " + diagnostico());
        }
    }

    public SucursalVinculo buscarSucursal(long idConfig) {
        String sql = "SELECT ID_CONFIG, SUCURSAL, POS FROM FACTURACION.F_CONFIG WHERE ID_CONFIG = ?";
        try (Connection con = oracle.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idConfig);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new SucursalVinculo(rs.getLong("ID_CONFIG"), rs.getInt("SUCURSAL"), rs.getInt("POS"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar la sucursal en FACTURACION.F_CONFIG", ex);
        }
    }

    public boolean existeParaConfig(long idConfig) {
        String sql = "SELECT 1 FROM FACTURACION.F_POS WHERE ID_CONFIG = ?";
        try (Connection con = oracle.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idConfig);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el punto de venta en FACTURACION.F_POS", ex);
        }
    }

    public PuntoVenta insertar(NuevoPuntoVenta punto, String usuario) {
        String sql = """
                INSERT INTO FACTURACION.F_POS (
                    ID_POS, ID_CONFIG, DESCRIPCION, DIRECCION_IP, POS, TIPO_PUNTO_VENTA, NOMBRE_PUNTO_VENTA,
                    TIPO_PING, ENABLED, LAST_USER, LAST_STAMP, NUMERO_CONTRATO, NIT_COMISIONISTA,
                    FECHA_INICIO, FECHA_FIN, OBTENER_CUFD, ESTADO_SIAT
                ) VALUES (
                    %s.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, ?, ?, ?, ?, ?
                )
                """.formatted(secuencia());
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, punto.idConfig());
                ps.setString(i++, punto.descripcion().trim());
                texto(ps, i++, punto.direccionIp());
                ps.setInt(i++, punto.pos());
                ps.setInt(i++, punto.tipoPuntoVenta());
                ps.setString(i++, punto.nombrePuntoVenta().trim());
                ps.setInt(i++, punto.tipoPing());
                ps.setInt(i++, punto.enabled());
                ps.setString(i++, usuario);
                texto(ps, i++, punto.numeroContrato());
                texto(ps, i++, punto.nitComisionista());
                fecha(ps, i++, punto.fechaInicio());
                fecha(ps, i++, punto.fechaFin());
                entero(ps, i++, punto.obtenerCufd());
                entero(ps, i, punto.estadoSiat());
                ps.executeUpdate();
            }
            PuntoVenta creado = buscar(con, punto.idConfig());
            con.commit();
            return creado;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el punto de venta en FACTURACION.F_POS", ex);
        }
    }

    private PuntoVenta buscar(Connection con, long idConfig) throws SQLException {
        String sql = "SELECT " + COLUMNAS + """
                 FROM FACTURACION.F_POS p
                 LEFT JOIN FACTURACION.F_CONFIG c ON c.ID_CONFIG = p.ID_CONFIG
                 LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
                 WHERE p.ID_CONFIG = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idConfig);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("El punto de venta no quedo registrado");
                return mapear(rs);
            }
        }
    }

    private String diagnostico() {
        String sql = """
                SELECT SYS_CONTEXT('USERENV','SESSION_USER') AS SESION,
                       (SELECT LISTAGG(owner || ' ' || object_type, ', ') WITHIN GROUP (ORDER BY owner)
                          FROM all_objects WHERE object_name = 'F_POS') AS OBJETOS,
                       (SELECT LISTAGG(grantee || ' ' || privilege, ', ') WITHIN GROUP (ORDER BY grantee)
                          FROM all_tab_privs WHERE table_name = 'F_POS') AS PRIVILEGIOS
                FROM dual
                """;
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return "Sin diagnostico de privilegios";
            String objetos = rs.getString("OBJETOS");
            String privilegios = rs.getString("PRIVILEGIOS");
            return "Sesion " + rs.getString("SESION")
                    + ". Objetos F_POS visibles: " + (objetos == null ? "ninguno" : objetos)
                    + ". Privilegios: " + (privilegios == null ? "ninguno" : privilegios);
        } catch (SQLException ex) {
            return "No se pudo leer el diagnostico de privilegios: " + ex.getMessage();
        }
    }

    private String secuencia() {
        String conocida = secuencia;
        if (conocida != null) return conocida;
        String sql = """
                SELECT sequence_owner || '.' || sequence_name
                FROM all_sequences
                WHERE sequence_owner = 'FACTURACION'
                  AND sequence_name IN ('S_F_POS', 'SEQ_F_POS', 'S_POS')
                ORDER BY CASE sequence_name WHEN 'S_F_POS' THEN 1 WHEN 'SEQ_F_POS' THEN 2 ELSE 3 END
                """;
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("No existe la secuencia de FACTURACION.F_POS");
            secuencia = rs.getString(1);
            return secuencia;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo ubicar la secuencia de FACTURACION.F_POS", ex);
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

    private static void texto(PreparedStatement ps, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) ps.setNull(indice, Types.VARCHAR);
        else ps.setString(indice, valor.trim());
    }

    private static void entero(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) ps.setNull(indice, Types.NUMERIC);
        else ps.setInt(indice, valor);
    }

    public static final class ConsultaRechazada extends RuntimeException {
        public ConsultaRechazada(String message) { super(message); }
    }

    private static void fecha(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        if (valor == null) ps.setNull(indice, Types.DATE);
        else ps.setDate(indice, Date.valueOf(valor));
    }
}
