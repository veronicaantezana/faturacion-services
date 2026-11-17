package com.bga.facturacion_services.facturacion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

public class OracleMetadataCheckTest {

    @Test
    void checkOracle() {
        String url = "jdbc:oracle:thin:@//172.24.0.100:1521/BGDB";
        String user = "FACTURACION";
        String pass = "FACTURACION";

        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement st = con.createStatement()) {

            System.out.println("=== SEQUENCES LIKE %MAPEO% OR %PRODUCTO% ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT sequence_name FROM all_sequences WHERE sequence_owner='FACTURACION' AND (sequence_name LIKE '%MAPEO%' OR sequence_name LIKE '%PROD%')")) {
                while (rs.next()) {
                    System.out.println("SEQUENCE: " + rs.getString(1));
                }
            }

            System.out.println("=== ALL OBJECTS LIKE %PRODUCTO% OR %MAPEO% ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT object_name, object_type FROM all_objects WHERE owner='FACTURACION' AND (object_name LIKE '%MAPEO%' OR object_name LIKE '%PROD%')")) {
                while (rs.next()) {
                    System.out.println("OBJ: " + rs.getString(1) + " (" + rs.getString(2) + ")");
                }
            }

            System.out.println("=== PROCEDURES IN FACTURACION SCHEMA ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT object_name, procedure_name FROM all_procedures WHERE owner='FACTURACION' AND (procedure_name LIKE '%PROD%' OR procedure_name LIKE '%MAPEO%')")) {
                while (rs.next()) {
                    System.out.println("PROC: " + rs.getString(1) + " -> " + rs.getString(2));
                }
            }

            System.out.println("=== TRIGGERS ON F_MAPEO_PRODUCTO ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT trigger_name FROM all_triggers WHERE table_owner='FACTURACION' AND table_name='F_MAPEO_PRODUCTO'")) {
                while (rs.next()) {
                    System.out.println("TRIGGER: " + rs.getString(1));
                }
            }

            System.out.println("=== COLUMNS OF F_MAPEO_PRODUCTO ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT column_name, data_type, nullable, data_default FROM all_tab_columns WHERE owner='FACTURACION' AND table_name='F_MAPEO_PRODUCTO' ORDER BY column_id")) {
                while (rs.next()) {
                    System.out.println("COL: " + rs.getString(1) + " " + rs.getString(2) + " NULL=" + rs.getString(3) + " DEF=" + rs.getString(4));
                }
            }

            System.out.println("=== COLUMNS OF F_PRODUCTO_SIN ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT column_name, data_type, nullable, data_default FROM all_tab_columns WHERE owner='FACTURACION' AND table_name='F_PRODUCTO_SIN' ORDER BY column_id")) {
                while (rs.next()) {
                    System.out.println("COL: " + rs.getString(1) + " " + rs.getString(2) + " NULL=" + rs.getString(3) + " DEF=" + rs.getString(4));
                }
            }

        } catch (Exception ex) {
            System.out.println("Oracle connection exception: " + ex.getMessage());
        }
    }
}
