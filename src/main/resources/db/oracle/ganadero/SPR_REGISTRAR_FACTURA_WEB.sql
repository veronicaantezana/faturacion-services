-- Procedimiento suelto del esquema GANADERO.
-- En DBeaver queda en Procedimientos, al mismo nivel que los paquetes.
-- No se pega dentro de PKG_CI_FACTURA.
-- Antes de ejecutar el script, cambia el delimitador del editor de ; a /.

CREATE OR REPLACE PROCEDURE GANADERO.SPR_REGISTRAR_FACTURA_WEB (
    pInNitEmisor                  IN NUMBER,
    pInRazonSocialEmisor          IN VARCHAR2,
    pInCodDocumentoSector         IN NUMBER,
    pInCodTipoFactura             IN NUMBER,
    pInSucursalSin                IN NUMBER,
    pInCodPuntoVenta              IN NUMBER,
    pInMunicipioSin               IN VARCHAR2,
    pInRazonSocialCliente         IN VARCHAR2,
    pInCodTipoDocumentoIdentidad  IN NUMBER,
    pInNumeroDocumento            IN VARCHAR2,
    pInComplemento                IN VARCHAR2,
    pInCodCliente                 IN VARCHAR2,
    pInCodMetodoPago              IN NUMBER,
    pInNumeroTarjeta              IN NUMBER,
    pInCodMonedaSin               IN NUMBER,
    pInTipoCambio                 IN NUMBER,
    pInTipoCambioOficial          IN NUMBER,
    pInMontoTotal                 IN NUMBER,
    pInMontoTotalMoneda           IN NUMBER,
    pInMontoTotalSujetoIva        IN NUMBER,
    pInCodTipoOperacion           IN NUMBER,
    pInIngresoDiferenciaCambio    IN NUMBER,
    pInEmail                      IN VARCHAR2,
    pInCelular                    IN VARCHAR2,
    pInCodExcepcion               IN NUMBER,
    pInCafc                       IN VARCHAR2,
    pInDetalleJson                IN CLOB,
    pInCodSistemaOrigen           IN VARCHAR2 DEFAULT 'FACTWEB',
    pInCodCanal                   IN NUMBER   DEFAULT 0,
    pInUsuario                    IN VARCHAR2 DEFAULT 'admin',
    pOutIdComplemento             OUT NUMBER,
    pOutNroSolicitud              OUT NUMBER,
    pOutCodError                  OUT VARCHAR2,
    pOutDesError                  OUT VARCHAR2
) AS
    lExcError              EXCEPTION;
    lIntCantidadDetalle    NUMBER(5);
    lIntIdComplemento      NUMBER(16);
    lIntNroSolicitud       NUMBER(16);
    lStrCodError           VARCHAR2(20);
    lStrMensaje            VARCHAR2(500);
    lDatFechaProceso       DATE;
BEGIN
    /*========================================================================================
        Autor      : canal web
        Fecha      : 10/11/2026
        Descripcion: Alta de factura recibida desde Angular, sin asiento del core.
                     Inserta CI_FACTURA_COMPLEMENTO, CI_FACTURA_LINEA_COMPLEMENTO
                     y FAC_SOLICITUDES_ENVIO en estado P.
        Referencia : Proyecto de Facturacion Electronica
        ========================================================================================
    */
    lStrCodError     := 'COD000';
    lStrMensaje      := NULL;
    lDatFechaProceso := TRUNC(SYSDATE);

    IF pInNitEmisor IS NULL
       OR pInRazonSocialEmisor IS NULL
       OR pInCodDocumentoSector IS NULL
       OR pInCodTipoFactura IS NULL
       OR pInRazonSocialCliente IS NULL
       OR pInNumeroDocumento IS NULL
       OR pInCodMetodoPago IS NULL
       OR pInMontoTotal IS NULL
       OR pInDetalleJson IS NULL
       OR pInCodSistemaOrigen IS NULL
       OR pInUsuario IS NULL THEN
        lStrCodError := 'WEB001';
        lStrMensaje  := 'Faltan datos obligatorios de la cabecera';
        RAISE lExcError;
    END IF;

    IF pInCodDocumentoSector NOT IN (1, 9, 15) THEN
        lStrCodError := 'WEB002';
        lStrMensaje  := 'Codigo de documento sector no habilitado para el canal web';
        RAISE lExcError;
    END IF;

    IF pInMontoTotal <= 0 THEN
        lStrCodError := 'WEB003';
        lStrMensaje  := 'El monto total debe ser mayor a cero';
        RAISE lExcError;
    END IF;

    SELECT COUNT(*)
      INTO lIntCantidadDetalle
      FROM JSON_TABLE(pInDetalleJson, '$[*]'
             COLUMNS (descripcion VARCHAR2(500) PATH '$.descripcion')) jt;

    IF lIntCantidadDetalle <= 0 THEN
        lStrCodError := 'WEB004';
        lStrMensaje  := 'La factura no tiene detalle';
        RAISE lExcError;
    END IF;

    lIntIdComplemento := GANADERO.SEQ_CI_FACTURA_COMPLEMENTO.NEXTVAL;
    lIntNroSolicitud  := GANADERO.SEQ_FAC_SOLICITUDES_ENVIO.NEXTVAL;

    -- ASIENTO = identificador para no repetir la clave contable del core.
    INSERT INTO GANADERO.CI_FACTURA_COMPLEMENTO (
           IDENTIFICADOR, ASIENTO, SUCURSAL_ASIENTO, FECHA_PROCESO_ASIENTO,
           ORDINAL_MOVIMIENTO, TR_REAL, TR_VIRTUAL, TR_TIPO,
           COD_MOTIVO_ANULACION, COD_PUNTO_VENTA, COD_METODO_PAGO, NUMERO_TARJETA,
           MONTO_DESCUENTO, TIPO_CAMBIO, TIPO_CAMBIO_OFICIAL, INGRESO_DIFERENCIA_CAMBIO,
           NRO_FACTURA_SIN, NIT_EMISOR, CUF, CUFD, COD_DOCUMENTO_SECTOR, COD_CLIENTE,
           COD_TIPO_OPERACION, PERIODO_FACTURADO, NRO_CREDITO_DEBITO, MONTO_TOTAL_DEVUELTO,
           MONTO_EFECTIVO_CRE_DEB, COD_SISTEMA_ORIGEN, COD_TIPO_FACTURA, FECHA_EMISION,
           LEYENDA, TZ_LOCK, ID_DOC_FISCAL_FEEL, FECHA_EMISION_SIN,
           COD_ESTADO_SOLICITUD, COD_ESTADO_FACTURA, COD_ESTADO_OBSERVACION_SIN,
           MONTO_TOTAL_ARRENDAMIENTO_FINANCIERO, MONTO_TOTAL_SUJETO_IVA, COD_CANAL,
           SUCURSAL_SIN, MUNICIPIO_SIN, DIRECCION_SUCURSAL_SIN, RAZON_SOCIAL_EMISOR,
           FACTURA_XML_FIRMADO, EMAIL, CELULAR, CONTINGENCIA, COD_EVENTO_SIGNIF, COD_CAFC,
           ES_LOTE, NRO_LOTE, ULT_FACTURA_LOTE, COD_MONEDA_SIN, MONTO_TOTAL, MONTO_TOTAL_MONEDA,
           COD_SUCURSAL_CONTA, RAZON_SOCIAL_CLIENTE, COD_TIPO_DOCUMENTO_IDENTIDAD,
           NUMERO_DOCUMENTO, COMPLEMENTO, MONEDA_TRANSACCION, CANT_ENVIO_CORREO, PRIORITARIO,
           NRO_OPERACION, COD_FACTURA_COMPLEMENTO_ORIGEN, USUARIO, FECHA_CREACION, HORA_CREACION,
           COD_EXCEPCION
    ) VALUES (
           lIntIdComplemento, lIntIdComplemento, 0, lDatFechaProceso,
           0, 0, 0, 0,
           NULL, NVL(pInCodPuntoVenta, 0), pInCodMetodoPago, NVL(pInNumeroTarjeta, 0),
           0, NVL(pInTipoCambio, 1), NVL(pInTipoCambioOficial, 1), NVL(pInIngresoDiferenciaCambio, 0),
           0, pInNitEmisor, NULL, NULL, pInCodDocumentoSector, pInCodCliente,
           NVL(pInCodTipoOperacion, 0), NULL, 0, 0,
           0, pInCodSistemaOrigen, pInCodTipoFactura, NULL,
           NULL, 0, 0, TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') || '000',
           'P', 'P', NULL,
           0, NVL(pInMontoTotalSujetoIva, pInMontoTotal), NVL(pInCodCanal, 0),
           NVL(pInSucursalSin, 0), pInMunicipioSin, NULL, pInRazonSocialEmisor,
           NULL, pInEmail, pInCelular, 0, 0, pInCafc,
           0, 0, 0, NVL(pInCodMonedaSin, 1), pInMontoTotal, NVL(pInMontoTotalMoneda, pInMontoTotal),
           0, pInRazonSocialCliente, pInCodTipoDocumentoIdentidad,
           pInNumeroDocumento, pInComplemento, NVL(pInCodMonedaSin, 1), 0, 'N',
           0, NULL, pInUsuario, TRUNC(SYSDATE), TO_CHAR(SYSDATE, 'HH24:MI:SS'),
           NVL(pInCodExcepcion, 0)
    );

    INSERT INTO GANADERO.CI_FACTURA_LINEA_COMPLEMENTO (
           IDENTIFICADOR, COD_CI_FACTURA_COM, ASIENTO, SUCURSAL_ASIENTO, FECHA_PROCESO_ASIENTO,
           ORDINAL_MOVIMIENTO, TR_REAL, TR_VIRTUAL, TR_TIPO, ORDINAL,
           COD_PRODUCTO_SIN, MONTO_FACTURABLE, COD_UNIDAD_MEDIDA, PRECIO_UNITARIO,
           MONTO_DETALLE_DESCUENTO, CANTIDAD, COD_ACT_ECONOMICA, DESCRIPCION, COD_PRODUCTO_BG
    )
    SELECT GANADERO.SEQ_CI_FACTURA_LINEA_COMPLEMENTO.NEXTVAL,
           lIntIdComplemento,
           lIntIdComplemento,
           0,
           lDatFechaProceso,
           0, 0, 0, 0,
           jt.ordinal,
           jt.codigo_producto_sin,
           jt.subtotal,
           jt.unidad_medida,
           jt.precio_unitario,
           NVL(jt.monto_descuento, 0),
           jt.cantidad,
           jt.actividad_economica,
           jt.descripcion,
           NVL(jt.codigo_producto, jt.codigo_producto_sin)
      FROM JSON_TABLE(pInDetalleJson, '$[*]'
             COLUMNS (
                 ordinal              FOR ORDINALITY,
                 actividad_economica  VARCHAR2(30)  PATH '$.actividadEconomica',
                 codigo_producto_sin  VARCHAR2(30)  PATH '$.codigoProductoSin',
                 codigo_producto      VARCHAR2(50)  PATH '$.codigoProducto',
                 descripcion          VARCHAR2(500) PATH '$.descripcion',
                 cantidad             NUMBER        PATH '$.cantidad',
                 unidad_medida        NUMBER        PATH '$.unidadMedida',
                 precio_unitario      NUMBER        PATH '$.precioUnitario',
                 monto_descuento      NUMBER        PATH '$.montoDescuento',
                 subtotal             NUMBER        PATH '$.subTotal'
             )) jt;

    INSERT INTO GANADERO.FAC_SOLICITUDES_ENVIO (
           IDENTIFICADOR, COD_ERROR, COD_ESTADO, FECHA_CREACION, HORA_CREACION,
           TZ_LOCK, COD_SOLICITUDES_CONFIRMACION, COD_CI_FACTURA_COMPLEMENTO,
           JSON_DATO_ENVIO, JSON_DATO_RESPUESTA
    ) VALUES (
           lIntNroSolicitud, NULL, 'P', TRUNC(SYSDATE), TO_CHAR(SYSDATE, 'HH24:MI:SS'),
           0, 0, lIntIdComplemento,
           NULL, NULL
    );

    pOutIdComplemento := lIntIdComplemento;
    pOutNroSolicitud  := lIntNroSolicitud;
    pOutCodError      := lStrCodError;
    pOutDesError      := NULL;

EXCEPTION
    WHEN lExcError THEN
        pOutIdComplemento := NULL;
        pOutNroSolicitud  := NULL;
        pOutCodError      := lStrCodError;
        pOutDesError      := lStrMensaje;
    WHEN OTHERS THEN
        pOutIdComplemento := NULL;
        pOutNroSolicitud  := NULL;
        pOutCodError      := TO_CHAR(SQLCODE);
        pOutDesError      := SQLERRM;
END SPR_REGISTRAR_FACTURA_WEB;
/
