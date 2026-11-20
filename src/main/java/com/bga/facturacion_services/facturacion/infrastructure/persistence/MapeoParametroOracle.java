package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.application.MapeoParametrosFeel;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Lee el codigo ERP y el codigo de sistema por FACTURACION.PKG_REGISTRO. */
public final class MapeoParametroOracle implements MapeoParametrosFeel {
    private static final int CURSOR = -10;
    private static final String LLAMADA =
            "{ call FACTURACION.PKG_REGISTRO.SPR_OBTENER_CODIGOS_EMISION(?,?,?,?,?,?,?,?,?,?) }";
    private static final String LISTAR_METODOS =
            "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_METODOS_PAGO(?,?,?,?) }";
    private final DataSource oracle;

    public MapeoParametroOracle(DataSource oracle) {
        this.oracle = oracle;
    }

    public Codigos consultar(long idEmpresa, int tipoDocumento, int metodoPago, int moneda) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LLAMADA)) {
            llamada.setLong(1, idEmpresa);
            llamada.setString(2, String.valueOf(tipoDocumento));
            llamada.setString(3, String.valueOf(metodoPago));
            llamada.setString(4, String.valueOf(moneda));
            llamada.registerOutParameter(5, Types.VARCHAR);
            llamada.registerOutParameter(6, Types.VARCHAR);
            llamada.registerOutParameter(7, Types.VARCHAR);
            llamada.registerOutParameter(8, Types.VARCHAR);
            llamada.registerOutParameter(9, Types.NUMERIC);
            llamada.registerOutParameter(10, Types.VARCHAR);
            llamada.execute();
            int codigo = llamada.getInt(9);
            if (llamada.wasNull() || codigo != 0) {
                throw fallo(codigo, llamada.getString(10));
            }
            return new Codigos(texto(llamada, 5), texto(llamada, 6), texto(llamada, 7), texto(llamada, 8));
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el mapeo de parametros", ex);
        }
    }

    public List<MetodoPagoMapeado> listarMetodosPago(long idEmpresa) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR_METODOS)) {
            llamada.setLong(1, idEmpresa);
            llamada.registerOutParameter(2, CURSOR);
            llamada.registerOutParameter(3, Types.NUMERIC);
            llamada.registerOutParameter(4, Types.VARCHAR);
            llamada.execute();
            int codigo = llamada.getInt(3);
            if (llamada.wasNull() || codigo != 0) throw fallo(codigo, llamada.getString(4));
            try (ResultSet rs = (ResultSet) llamada.getObject(2)) {
                var metodos = new ArrayList<MetodoPagoMapeado>();
                while (rs.next()) {
                    String numero = rs.getString("CODIGO");
                    if (numero == null || !numero.trim().chars().allMatch(Character::isDigit)) continue;
                    metodos.add(new MetodoPagoMapeado(Integer.parseInt(numero.trim()), rs.getString("DESCRIPCION")));
                }
                return metodos;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer los metodos de pago", ex);
        }
    }

    private static RuntimeException fallo(int codigo, String descripcion) {
        String mensaje = limpiar(descripcion);
        if (codigo == -20001) return new IllegalArgumentException(mensaje);
        return new IllegalStateException(mensaje);
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) return "No se pudo leer el mapeo de parametros";
        String limpio = texto.trim();
        String marca = "ORA-20001:";
        int inicio = limpio.indexOf(marca);
        if (inicio >= 0) limpio = limpio.substring(inicio + marca.length()).trim();
        int salto = limpio.indexOf('\n');
        if (salto > 0) limpio = limpio.substring(0, salto).trim();
        return limpio;
    }

    private static String texto(CallableStatement llamada, int indice) throws SQLException {
        String valor = llamada.getString(indice);
        if (valor == null || valor.isBlank())
            throw new IllegalStateException("El mapeo no devolvio el codigo");
        return valor.trim();
    }
}
