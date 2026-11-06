package com.bga.facturacion_services.facturacion.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "factura", uniqueConstraints = @UniqueConstraint(columnNames = "clave"))
public class FacturaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long numero;
    @Column(nullable = false, unique = true, length = 36)
    String uuid;
    @Column(nullable = false, length = 100)
    String clave;
    @Lob @Column(nullable = false)
    String solicitud;
    @Lob
    String registro;
    protected FacturaEntity() {}
}
