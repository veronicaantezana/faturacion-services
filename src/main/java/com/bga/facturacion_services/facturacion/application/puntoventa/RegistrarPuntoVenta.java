package com.bga.facturacion_services.facturacion.application.puntoventa;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegistrarPuntoVenta {
    private final PuntoVentaRepositorio puntos;

    public RegistrarPuntoVenta(PuntoVentaRepositorio puntos) {
        this.puntos = puntos;
    }

    public List<PuntoVenta> listar() {
        return puntos.listar();
    }

    public PuntoVenta registrar(NuevoPuntoVenta punto, String usuario) {
        if (punto.descripcion() == null || punto.descripcion().isBlank())
            throw new IllegalArgumentException("La descripcion del punto de venta es obligatoria");
        if (punto.nombrePuntoVenta() == null || punto.nombrePuntoVenta().isBlank())
            throw new IllegalArgumentException("El nombre del punto de venta es obligatorio");
        if (punto.pos() < 0)
            throw new IllegalArgumentException("El punto de venta no puede ser negativo");
        if (punto.enabled() != 0 && punto.enabled() != 1)
            throw new IllegalArgumentException("enabled debe ser 0 o 1");
        String quien = usuario == null ? "" : usuario.trim();
        if (quien.isBlank()) throw new IllegalArgumentException("Falta el usuario que registra el punto de venta");
        PuntoVentaRepositorio.SucursalVinculo sucursal = puntos.buscarSucursal(punto.idConfig());
        if (sucursal == null)
            throw new IllegalArgumentException("No existe la sucursal " + punto.idConfig());
        if (sucursal.pos() != punto.pos())
            throw new IllegalArgumentException("La sucursal " + punto.idConfig()
                    + " tiene el punto de venta " + sucursal.pos()
                    + ". F_POS debe usar ese mismo numero");
        if (puntos.existeParaConfig(punto.idConfig()))
            throw new PuntoExistente("La sucursal " + punto.idConfig() + " ya tiene un punto de venta en F_POS");
        return puntos.insertar(punto, quien);
    }

    public static final class PuntoExistente extends RuntimeException {
        public PuntoExistente(String message) { super(message); }
    }
}
