package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.infrastructure.rest.EmpresaResponse;
import com.bga.facturacion_services.facturacion.infrastructure.rest.RegistrarEmpresaRequest;
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
public class EmpresaOracle {
    private static final String COLUMNAS = """
            ID_EMPRESA, NIT, NOMBRE, ESTADO, MODALIDAD, CODIGO_AUTORIZACION, KEY_NAME, CODIGO_SISTEMA,
            ADMIN_SEQUENCE, ADMIN_SEQ_NOTA, LAST_USER, LAST_STAMP, ENVIAR_CORREO, COMPLETAR_DATOS,
            URL_DESCARGA, DEST_NOTIFICACION, DOC_SECTOR_VALIDATE, GENERAR_PDF, TOKEN_ENABLED,
            CORREO_INDIVIDUAL, PLAN_VIGENTE, PERIODO_PAGO, MAX_FACTURAS, CONCILIACION, NOMBRECOMERCIAL,
            FUNCIONALIDA, GENERAR_PDF_COD_SISTEMA
            """;

    private final DataSource oracle;

    public EmpresaOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<EmpresaResponse> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM FACTURACION.F_EMPRESA ORDER BY ID_EMPRESA";
        try (Connection con = oracle.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            var empresas = new ArrayList<EmpresaResponse>();
            while (rs.next()) empresas.add(mapear(rs));
            return empresas;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer FACTURACION.F_EMPRESA", ex);
        }
    }

    public boolean existeNit(String nit) {
        String sql = "SELECT 1 FROM FACTURACION.F_EMPRESA WHERE NIT = ?";
        try (Connection con = oracle.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el NIT en FACTURACION.F_EMPRESA", ex);
        }
    }

    public EmpresaResponse insertar(RegistrarEmpresaRequest solicitud, String usuario) {
        String sql = """
                INSERT INTO FACTURACION.F_EMPRESA (
                    ID_EMPRESA, NIT, NOMBRE, ESTADO, MODALIDAD, CODIGO_AUTORIZACION, KEY_NAME, CODIGO_SISTEMA,
                    ADMIN_SEQUENCE, ADMIN_SEQ_NOTA, LAST_USER, LAST_STAMP, ENVIAR_CORREO, COMPLETAR_DATOS,
                    URL_DESCARGA, DEST_NOTIFICACION, DOC_SECTOR_VALIDATE, GENERAR_PDF, TOKEN_ENABLED,
                    CORREO_INDIVIDUAL, PLAN_VIGENTE, PERIODO_PAGO, MAX_FACTURAS, CONCILIACION, NOMBRECOMERCIAL,
                    FUNCIONALIDA, GENERAR_PDF_COD_SISTEMA
                ) VALUES (
                    FACTURACION.S_F_EMPRESA.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, solicitud.nit().trim());
                ps.setString(i++, solicitud.nombre().trim());
                ps.setString(i++, solicitud.estado().trim());
                ps.setInt(i++, solicitud.modalidad());
                texto(ps, i++, solicitud.codigoAutorizacion());
                texto(ps, i++, solicitud.keyName());
                texto(ps, i++, solicitud.codigoSistema());
                entero(ps, i++, solicitud.adminSequence());
                entero(ps, i++, solicitud.adminSeqNota());
                ps.setString(i++, usuario);
                entero(ps, i++, solicitud.enviarCorreo());
                entero(ps, i++, solicitud.completarDatos());
                texto(ps, i++, solicitud.urlDescarga());
                texto(ps, i++, solicitud.destNotificacion());
                texto(ps, i++, solicitud.docSectorValidate());
                entero(ps, i++, solicitud.generarPdf());
                entero(ps, i++, solicitud.tokenEnabled());
                entero(ps, i++, solicitud.correoIndividual());
                texto(ps, i++, solicitud.planVigente());
                texto(ps, i++, solicitud.periodoPago());
                entero(ps, i++, solicitud.maxFacturas());
                entero(ps, i++, solicitud.conciliacion());
                texto(ps, i++, solicitud.nombreComercial());
                entero(ps, i++, solicitud.funcionalida());
                entero(ps, i, solicitud.generarPdfCodSistema());
                ps.executeUpdate();
            }
            EmpresaResponse creada = buscarPorNit(con, solicitud.nit().trim());
            con.commit();
            return creada;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar la empresa en FACTURACION.F_EMPRESA", ex);
        }
    }

    private EmpresaResponse buscarPorNit(Connection con, String nit) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM FACTURACION.F_EMPRESA WHERE NIT = ? ORDER BY ID_EMPRESA DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nit);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("La empresa no quedo registrada");
                return mapear(rs);
            }
        }
    }

    private static EmpresaResponse mapear(ResultSet rs) throws SQLException {
        Timestamp sello = rs.getTimestamp("LAST_STAMP");
        return new EmpresaResponse(
                rs.getLong("ID_EMPRESA"),
                rs.getString("NIT"),
                rs.getString("NOMBRE"),
                rs.getString("ESTADO"),
                entero(rs, "MODALIDAD"),
                rs.getString("CODIGO_AUTORIZACION"),
                rs.getString("KEY_NAME"),
                rs.getString("CODIGO_SISTEMA"),
                entero(rs, "ADMIN_SEQUENCE"),
                entero(rs, "ADMIN_SEQ_NOTA"),
                rs.getString("LAST_USER"),
                sello == null ? null : sello.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                entero(rs, "ENVIAR_CORREO"),
                entero(rs, "COMPLETAR_DATOS"),
                rs.getString("URL_DESCARGA"),
                rs.getString("DEST_NOTIFICACION"),
                rs.getString("DOC_SECTOR_VALIDATE"),
                entero(rs, "GENERAR_PDF"),
                entero(rs, "TOKEN_ENABLED"),
                entero(rs, "CORREO_INDIVIDUAL"),
                rs.getString("PLAN_VIGENTE"),
                rs.getString("PERIODO_PAGO"),
                entero(rs, "MAX_FACTURAS"),
                entero(rs, "CONCILIACION"),
                rs.getString("NOMBRECOMERCIAL"),
                entero(rs, "FUNCIONALIDA"),
                entero(rs, "GENERAR_PDF_COD_SISTEMA"));
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
