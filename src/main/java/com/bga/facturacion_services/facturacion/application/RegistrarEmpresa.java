package com.bga.facturacion_services.facturacion.application;

import com.bga.facturacion_services.facturacion.infrastructure.persistence.EmpresaOracle;
import com.bga.facturacion_services.facturacion.infrastructure.rest.EmpresaResponse;
import com.bga.facturacion_services.facturacion.infrastructure.rest.RegistrarEmpresaRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegistrarEmpresa {
    private final EmpresaOracle empresas;

    public RegistrarEmpresa(EmpresaOracle empresas) {
        this.empresas = empresas;
    }

    public List<EmpresaResponse> listar() {
        return empresas.listar();
    }

    public List<EmpresaResponse> listarPorActividades(String actividades) {
        return empresas.listarPorActividades(actividadesNormalizadas(actividades));
    }

    static String actividadesNormalizadas(String actividades) {
        if (actividades == null || actividades.isBlank()) {
            throw new IllegalArgumentException("Faltan las actividades");
        }
        var codigos = new java.util.ArrayList<String>();
        for (String parte : actividades.split(",")) {
            String codigo = parte.trim();
            if (!codigo.matches("\\d{1,12}")) {
                throw new IllegalArgumentException("Actividad invalida");
            }
            codigos.add(codigo);
        }
        if (codigos.isEmpty()) throw new IllegalArgumentException("Faltan las actividades");
        return String.join(",", codigos);
    }

    public EmpresaResponse registrar(RegistrarEmpresaRequest solicitud, String usuario) {
        if (solicitud.estado() == null || (!solicitud.estado().trim().equals("0") && !solicitud.estado().trim().equals("1"))) {
            throw new IllegalArgumentException("El estado de la empresa debe ser 1 o 0");
        }
        if (solicitud.modalidad() == null) {
            throw new IllegalArgumentException("La modalidad es obligatoria");
        }
        bandera(solicitud.enviarCorreo(), "enviarCorreo");
        bandera(solicitud.completarDatos(), "completarDatos");
        bandera(solicitud.generarPdf(), "generarPdf");
        bandera(solicitud.tokenEnabled(), "tokenEnabled");
        bandera(solicitud.correoIndividual(), "correoIndividual");
        bandera(solicitud.conciliacion(), "conciliacion");
        bandera(solicitud.funcionalida(), "funcionalida");
        String nit = solicitud.nit().trim();
        if (empresas.existeNit(nit)) {
            throw new NitRegistrado("Ya existe una empresa con el NIT " + nit);
        }
        String quien = usuario == null || usuario.isBlank() ? null : usuario.trim();
        if (quien == null) throw new IllegalArgumentException("Falta el usuario que registra la empresa");
        return empresas.insertar(solicitud, quien);
    }

    private static void bandera(Integer valor, String campo) {
        if (valor != null && valor != 0 && valor != 1) {
            throw new IllegalArgumentException(campo + " debe ser 0 o 1");
        }
    }

    public static final class NitRegistrado extends RuntimeException {
        public NitRegistrado(String message) { super(message); }
    }
}
