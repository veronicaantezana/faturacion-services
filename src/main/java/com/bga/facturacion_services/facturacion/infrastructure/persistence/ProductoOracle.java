package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import com.bga.facturacion_services.facturacion.infrastructure.rest.ProductoEmpresaResponse;
import com.bga.facturacion_services.facturacion.infrastructure.rest.ProductoSinResponse;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoOracle {
    private static final int CURSOR = -10;
    private static final String LISTAR = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_PRODUCTOS(?,?,?,?) }";
    private static final String LISTAR_SIN = "{ call FACTURACION.PKG_REGISTRO.SPR_LISTAR_PRODUCTOS_SIN(?,?,?,?) }";
    private static final String REGISTRAR = "{ call FACTURACION.PKG_REGISTRO.SPR_REGISTRAR_PRODUCTO(?,?,?,?,?,?) }";

    private final DataSource oracle;

    public ProductoOracle(@Qualifier("facturacionDataSource") DataSource oracle) {
        this.oracle = oracle;
    }

    public List<ProductoEmpresaResponse> listarPorEmpresa(long idEmpresa) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR)) {
            llamada.setLong(1, idEmpresa);
            llamada.registerOutParameter(2, CURSOR);
            llamada.registerOutParameter(3, Types.NUMERIC);
            llamada.registerOutParameter(4, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 3, 4);
            try (ResultSet rs = (ResultSet) llamada.getObject(2)) {
                var productos = new ArrayList<ProductoEmpresaResponse>();
                while (rs.next()) productos.add(mapear(rs));
                return productos;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer los productos por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public List<ProductoSinResponse> listarSinPorEmpresa(long idEmpresa) {
        try (Connection con = oracle.getConnection();
             CallableStatement llamada = con.prepareCall(LISTAR_SIN)) {
            llamada.setLong(1, idEmpresa);
            llamada.registerOutParameter(2, CURSOR);
            llamada.registerOutParameter(3, Types.NUMERIC);
            llamada.registerOutParameter(4, Types.VARCHAR);
            llamada.execute();
            exigir(llamada, 3, 4);
            try (ResultSet rs = (ResultSet) llamada.getObject(2)) {
                var productos = new ArrayList<ProductoSinResponse>();
                while (rs.next()) {
                    productos.add(new ProductoSinResponse(
                            rs.getLong("ID_PRODUCTO_SIN"),
                            rs.getString("CODIGO_ACTIVIDAD"),
                            rs.getLong("CODIGO_PRODUCTO_SIN"),
                            rs.getString("DESCRIPCION")));
                }
                return productos;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer los productos del SIN por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    public ProductoEmpresaResponse registrar(long idEmpresa, long idProductoSin, String usuario) {
        try (Connection con = oracle.getConnection()) {
            con.setAutoCommit(false);
            try (CallableStatement llamada = con.prepareCall(REGISTRAR)) {
                llamada.setLong(1, idEmpresa);
                llamada.setLong(2, idProductoSin);
                if (usuario == null || usuario.isBlank()) llamada.setNull(3, Types.VARCHAR);
                else llamada.setString(3, usuario.trim());
                llamada.registerOutParameter(4, Types.NUMERIC);
                llamada.registerOutParameter(5, Types.NUMERIC);
                llamada.registerOutParameter(6, Types.VARCHAR);
                llamada.execute();
                int resultado = llamada.getInt(5);
                if (llamada.wasNull() || resultado != 0) {
                    throw new IllegalArgumentException(llamada.getString(6));
                }
                long idMapeo = llamada.getLong(4);
                con.commit();
                return listarPorEmpresa(idEmpresa).stream()
                        .filter(producto -> producto.id() == idMapeo)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("El producto no quedo registrado"));
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el producto por FACTURACION.PKG_REGISTRO", ex);
        }
    }

    private static void exigir(CallableStatement llamada, int codigo, int descripcion) throws SQLException {
        int resultado = llamada.getInt(codigo);
        if (llamada.wasNull() || resultado != 0) {
            throw new IllegalStateException(llamada.getString(descripcion));
        }
    }

    private static ProductoEmpresaResponse mapear(ResultSet rs) throws SQLException {
        return new ProductoEmpresaResponse(
                rs.getLong("ID_MAPEO_PRODUCTO"),
                rs.getString("CODIGO_PRODUCTO"),
                rs.getString("DESCRIPCION"),
                rs.getString("CODIGO_ACTIVIDAD"),
                rs.getString("DESCRIPCION_ACTIVIDAD"),
                rs.getLong("CODIGO_PRODUCTO_SIN"),
                true);
    }
}
