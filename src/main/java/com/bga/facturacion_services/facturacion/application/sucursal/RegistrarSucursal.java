package com.bga.facturacion_services.facturacion.application.sucursal;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegistrarSucursal {
    private final SucursalRepositorio sucursales;

    public RegistrarSucursal(SucursalRepositorio sucursales) {
        this.sucursales = sucursales;
    }

    public List<Sucursal> listar() {
        return sucursales.listar();
    }

    public Sucursal registrar(NuevaSucursal sucursal, String usuario) {
        if (sucursal.codigo() == null || sucursal.codigo().isBlank())
            throw new IllegalArgumentException("El codigo de la sucursal es obligatorio");
        if (sucursal.referencia() == null || sucursal.referencia().isBlank())
            throw new IllegalArgumentException("La referencia de la sucursal es obligatoria");
        if (sucursal.direccion() == null || sucursal.direccion().isBlank())
            throw new IllegalArgumentException("La direccion de la sucursal es obligatoria");
        if (sucursal.sucursal() < 0 || sucursal.pos() < 0)
            throw new IllegalArgumentException("La sucursal y el punto de venta no pueden ser negativos");
        if (sucursal.enabled() != 0 && sucursal.enabled() != 1)
            throw new IllegalArgumentException("enabled debe ser 0 o 1");
        if (sucursal.nroDocSequence() == null || sucursal.nroNotaSequence() == null)
            throw new IllegalArgumentException("Las secuencias de documento y nota son obligatorias");
        String quien = usuario == null ? "" : usuario.trim();
        if (quien.isBlank()) throw new IllegalArgumentException("Falta el usuario que registra la sucursal");
        if (!sucursales.existeEmpresa(sucursal.idEmpresa()))
            throw new IllegalArgumentException("No existe la empresa " + sucursal.idEmpresa());
        if (sucursales.existePunto(sucursal.idEmpresa(), sucursal.sucursal(), sucursal.pos()))
            throw new PuntoRegistrado("Esa empresa ya tiene la sucursal " + sucursal.sucursal()
                    + " con punto de venta " + sucursal.pos());
        return sucursales.insertar(sucursal, quien);
    }

    public static final class PuntoRegistrado extends RuntimeException {
        public PuntoRegistrado(String message) { super(message); }
    }
}
