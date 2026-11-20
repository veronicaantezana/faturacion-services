package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.infrastructure.rest.EmpresaResponse;
import com.bga.facturacion_services.facturacion.infrastructure.rest.RegistrarEmpresaRequest;
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
public class EmpresaOracle {
    private static final int CURSOR = -10;
    private static final String LISTAR = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_EMPRESAS(?,?,?) }";
    private static final String LISTAR_POR_ACTIVIDAD =
            "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_EMPRESAS_POR_ACTIVIDAD(?,?,?,?) }";
    private static final String REGISTRAR = "{ call FACTURACION.PKG_REGISTRO.SPR_REGISTRAR_EMPRESA("
            + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";

    private final DataSource oracle;

    public EmpresaOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<EmpresaResponse> listar() {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR)) {
            llamada.registerOutParameter(1, CURSOR);
            llamada.registerOutParameter(2, Types.NUMERIC);
            llamada.registerOutParameter(3, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 2, 3);
            try (ResultSet rs = (ResultSet) llamada.getObject(1)) {
                var empresas = new ArrayList<EmpresaResponse>();
                while (rs.next()) empresas.add(mapear(rs));
                return empresas;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer las empresas por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public List<EmpresaResponse> listarPorActividades(String actividades) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR_POR_ACTIVIDAD)) {
            llamada.setString(1, actividades);
            llamada.registerOutParameter(2, CURSOR);
            llamada.registerOutParameter(3, Types.NUMERIC);
            llamada.registerOutParameter(4, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 3, 4);
            try (ResultSet rs = (ResultSet) llamada.getObject(2)) {
                var empresas = new ArrayList<EmpresaResponse>();
                while (rs.next()) empresas.add(mapear(rs));
                return empresas;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer las empresas por actividad", ex);
        }
    }

    public boolean existeNit(String nit) {
        return listar().stream().anyMatch(empresa -> nit.equals(empresa.nit()));
    }

    public EmpresaResponse insertar(RegistrarEmpresaRequest solicitud, String usuario) {
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (CallableStatement llamada = con.prepareCall(REGISTRAR)) {
                texto(llamada, 1, solicitud.nit());
                texto(llamada, 2, solicitud.nombre());
                texto(llamada, 3, solicitud.estado());
                llamada.setInt(4, solicitud.modalidad());
                texto(llamada, 5, usuario);
                texto(llamada, 6, solicitud.codigoAutorizacion());
                texto(llamada, 7, solicitud.keyName());
                texto(llamada, 8, solicitud.codigoSistema());
                entero(llamada, 9, solicitud.adminSequence());
                entero(llamada, 10, solicitud.adminSeqNota());
                entero(llamada, 11, solicitud.enviarCorreo());
                entero(llamada, 12, solicitud.completarDatos());
                texto(llamada, 13, solicitud.urlDescarga());
                texto(llamada, 14, solicitud.destNotificacion());
                texto(llamada, 15, solicitud.docSectorValidate());
                entero(llamada, 16, solicitud.generarPdf());
                entero(llamada, 17, solicitud.tokenEnabled());
                entero(llamada, 18, solicitud.correoIndividual());
                texto(llamada, 19, solicitud.planVigente());
                texto(llamada, 20, solicitud.periodoPago());
                entero(llamada, 21, solicitud.maxFacturas());
                entero(llamada, 22, solicitud.conciliacion());
                texto(llamada, 23, solicitud.nombreComercial());
                entero(llamada, 24, solicitud.funcionalida());
                entero(llamada, 25, solicitud.generarPdfCodSistema());
                llamada.registerOutParameter(26, Types.NUMERIC);
                llamada.registerOutParameter(27, Types.NUMERIC);
                llamada.registerOutParameter(28, Types.VARCHAR);
                llamada.execute();
                exigir(llamada, 27, 28);
                long idEmpresa = llamada.getLong(26);
                con.commit();
                return listar().stream()
                        .filter(empresa -> empresa.idEmpresa() == idEmpresa)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("La empresa no quedo registrada"));
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar la empresa por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    private static void exigir(CallableStatement llamada, int codigo, int descripcion) throws SQLException {
        int resultado = llamada.getInt(codigo);
        if (llamada.wasNull() || resultado != 0) {
            throw new IllegalStateException(llamada.getString(descripcion));
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

    private static void texto(CallableStatement llamada, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) llamada.setNull(indice, Types.VARCHAR);
        else llamada.setString(indice, valor.trim());
    }

    private static void entero(CallableStatement llamada, int indice, Integer valor) throws SQLException {
        if (valor == null) llamada.setNull(indice, Types.NUMERIC);
        else llamada.setInt(indice, valor);
    }
}
