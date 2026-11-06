# Facturacion services

Backend del modulo de factura, inicialmente compra-venta. Java 17, Spring Boot 3.5.5, Maven, Bean Validation y JPA/H2. Se ajusto la base inicial (Java 21 / Boot 4.1.2) para compilar con el JDK 17 instalado.

## Ejecutar y probar

Desde la raiz:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Tambien puede usarse Maven instalado: `mvn spring-boot:run` / `mvn test`.
Si Maven toma una ruta incorrecta como repositorio local, puede indicarse explicitamente:
`mvn "-Dmaven.repo.local=C:\Users\Veronica\.m2\repository" test`.

API: http://localhost:8080. H2 guarda datos en `./data/facturacion`; los tests usan bases en memoria independientes.
No borrar la base si se desea conservar la numeracion y las claves de idempotencia.

## Probar con datos estaticos

```powershell
$facturaDemo = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/demo/facturas/compra-venta' -Headers @{ 'Idempotency-Key' = 'demo-compra-0001' }
$facturaDemo
```

El ejemplo contiene dos unidades de 50, descuento de linea 5 y descuento adicional 2: total 93 BOB.
Los codigos SIN, actividad, unidad y datos del emisor son ilustrativos, no catalogos homologados.

## Usar datos del frontend

La API siempre acepta datos del frontend; activar la demostracion no los reemplaza ni ignora.
El cuerpo de ejemplo esta en [compra-venta.json](src/main/resources/demo/compra-venta.json).

```powershell
$contenidoFactura = Get-Content -Raw 'src/main/resources/demo/compra-venta.json'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/facturas' -ContentType 'application/json' -Headers @{ 'Idempotency-Key' = 'venta-frontend-0001' } -Body $contenidoFactura
```

| Metodo | Ruta | Uso |
|---|---|---|
| POST | /api/v1/facturas | Validar, calcular, guardar y emitir |
| GET | /api/v1/facturas/{id} | Consultar factura y resultado persistido |
| GET | /api/v1/facturas/{id}/solicitud | Ver XML/JSON construido por el adaptador seleccionado |
| GET | /api/v1/demo/facturas/compra-venta | Obtener cuerpo de ejemplo |
| POST | /api/v1/demo/facturas/compra-venta | Emitir usando el ejemplo |

Los POST de emision exigen `Idempotency-Key`, de 8 a 100 caracteres alfanumericos, guion o guion bajo. Reutilizar la misma clave y cuerpo devuelve la misma factura sin reenviar; otro cuerpo con esa clave produce 409. En solicitudes concurrentes se puede recibir 409 mientras la primera termina: reintentar con la misma clave. Una factura en proceso o pendiente no se reenvia automaticamente.

Errores: 400 datos invalidos, 404 factura inexistente, 409 conflicto. Formato ProblemDetail, con errores de campos para Bean Validation.

## Donde cambiar cada cosa

Configuracion principal: [application.properties](src/main/resources/application.properties).

| Necesidad | Propiedad / archivo |
|---|---|
| Quitar endpoints y datos de demostracion | `facturacion.mock-datos=false` o `FACTURACION_MOCK_DATOS=false` |
| Elegir estructura de salida SIAT | `facturacion.proveedor=siat` o `FACTURACION_PROVEEDOR=siat` |
| Elegir estructura de salida del tercero | `facturacion.proveedor=tercero` o `FACTURACION_PROVEEDOR=tercero` |
| Simular respuesta | `facturacion.simulacion.escenario=aceptar\|rechazar\|timeout` / `FACTURACION_ESCENARIO` |
| Cambiar ejemplo | `src/main/resources/demo/compra-venta.json` |
| Configurar emisor | `facturacion.emisor.*` y sus variables de entorno |
| Adaptar contrato del tercero | `infrastructure/provider/TerceroAdapter.java` |
| Adaptar estructura SIAT | `infrastructure/provider/SiatAdapter.java` |
| Registrar o seleccionar integracion | `infrastructure/config/FacturacionConfiguration.java` |

Las variables de entorno se leen al arrancar. Reiniciar despues de cambiarlas.

Ejemplo: datos del frontend con adaptador del tercero, aun con transporte simulado:

```powershell
$env:FACTURACION_MOCK_DATOS = 'false'
$env:FACTURACION_PROVEEDOR = 'tercero'
$env:FACTURACION_ESCENARIO = 'aceptar'
.\mvnw.cmd spring-boot:run
```

## Arquitectura y extension

```text
REST DTO -> caso de uso EmitirFactura -> estrategia ReglaFactura
                                  | -> puerto FacturaStore -> JPA/H2
                                  | -> puerto ProveedorFacturacion
                                         | -> SiatAdapter -> TransporteSimulado
                                         | -> TerceroAdapter -> TransporteSimulado
```

- Dominio sin dependencias de Spring, JPA ni estructuras externas.
- Estrategia por tipo de factura: `ReglaFactura` / `CompraVenta`.
- Puertos y adaptadores para cambiar proveedor y almacenamiento.
- Inyeccion por constructor y seleccion de un solo proveedor al arrancar.
- DTO REST independiente del modelo interno; cada adaptador transforma desde ese modelo.
- Importes BigDecimal, precios y descuentos con hasta dos decimales, cantidades hasta cinco, redondeo HALF_UP por linea. El frontend no determina los totales.
- Datos del emisor, numero, fecha e identificador se generan en backend.
- Numero unico asignado por la base para este modulo/emisor. No es una secuencia fiscal por sucursal/punto de venta; esa politica se debe acordar al integrar la emision real.
- La clave idempotente se reserva antes de llamar al proveedor; no hay transaccion de base abierta durante el envio. Un reinicio entre reserva y guardado requiere conciliacion manual, no un reenvio a ciegas.

Para agregar los otros tres tipos:

1. Implementar una estrategia `ReglaFactura` con un codigo de tipo propio y registrarla como bean.
2. Si requiere campos propios, ampliar el contrato con un DTO especifico por tipo y su modelo, manteniendo los datos comunes; no reutilizar campos de compra-venta con otro significado.
3. Agregar el mapeo y validaciones especificos en los adaptadores que soporten ese tipo.
4. Agregar pruebas de calculo y contrato para ese documento.

El registro de estrategias selecciona por codigo, sin agregar condiciones al caso de uso de emision. La API rechaza tipos no registrados.

## Alcance de las integraciones actuales

Ambos adaptadores existen y se pueden seleccionar, pero **el transporte de ambos es simulado**. No se realizan llamadas de red.

- SIAT: construye una vista previa XML compra-venta basada en la pagina suministrada. CUF y CUFD llevan marcas de simulacion; no son autorizaciones fiscales. No incluye firma, validacion XSD oficial, solicitud SOAP, compresion ni hash. Esas etapas se incorporaran dentro de la integracion SIAT, sin convertirlas en requisitos del tercero.
- Tercero: construye un JSON de ejemplo con contrato propio y provisional. No representa una API real conocida; DTOs, autenticacion y transporte se reemplazan cuando exista su documentacion.
- `facturacion.transporte=real` falla explicitamente al arrancar. No basta cambiar un booleano para habilitar una integracion aun no implementada.
- La simulacion produce ACEPTADA_SIMULADA, RECHAZADA_SIMULADA o PENDIENTE_CONFIRMACION. Los documentos y respuestas carecen de validez fiscal.
- Primera version: BOB, efectivo (metodoPago 1), sin Gift Card. Otros metodos se rechazan para no generar documentos incompletos.
- Aun no incluye anulacion, contingencias, paquetes, inventario, PDF, catalogos sincronizados ni autenticacion. Es un backend de desarrollo local; antes de exponerlo hay que incorporar acceso autenticado y definir autorizaciones del emisor.
- H2 y `ddl-auto=update` son para esta etapa. La migracion a produccion requiere base definitiva y migraciones versionadas.

Para conectar al tercero: implementar su transporte, reemplazar sus DTO y mapeo, normalizar su respuesta a `ResultadoEmision`, declarar `simulado=false` y habilitar la seleccion en configuracion. Para SIAT directo: completar ademas autorizaciones, codigos, CUF, firma segun modalidad, XSD vigente y protocolo oficial. La estructura interna y el caso de uso no deben depender de esos DTO externos.

Referencias consultadas:
- [Factura de Compra y Venta SIAT](https://siatinfo.impuestos.gob.bo/index.php/facturacion-en-linea/archivos-xml-xsd-de-facturas-electronicas/factura-de-compra-y-venta)
- [Validaciones de documentos sector](https://siatinfo.impuestos.gob.bo/index.php/facturacion-en-linea/archivos-xml-xsd-de-facturas-electronicas/validaciones-documentos-sector/validaciones)

Se sigue la estructura del enlace indicado. Antes de integracion real debe confirmarse el anexo/XSD aplicable; esta vista previa no certifica cumplimiento fiscal.
