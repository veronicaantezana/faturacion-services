-- Vuelco de la respuesta de FEEL sobre la fila que creo SPR_REGISTRAR_FACTURA_WEB.
-- No usa PKG_CI_FACTURA.ACTUALIZAR_DATOS_FACTURA_ENVIADO: ese cierre es del worker del core.
-- No hace COMMIT. La transaccion la confirma Java.
-- En DBeaver, conexion del usuario GANADERO. Delimitador /.
-- Selecciona este CREATE completo y ejecuta el script. No incluyas solo el cuerpo.

CREATE OR REPLACE PROCEDURE GANADERO.SPR_ACTUALIZAR_RESPUESTA_FEEL (
    pInIdComplemento   IN NUMBER,
    pInNroSolicitud    IN NUMBER,
    pInEnviado         IN NUMBER,
    pInMensaje         IN VARCHAR2,
    pInJsonRespuesta   IN CLOB,
    pOutCodError       OUT VARCHAR2,
    pOutDesError       OUT VARCHAR2
) AS
    lExcError            EXCEPTION;
    lIntExiste           NUMBER(5);
    lStrCuf              VARCHAR2(100);
    lStrCufd             VARCHAR2(200);
    lStrCufCabecera      VARCHAR2(100);
    lStrCufdCabecera     VARCHAR2(200);
    lStrCufdProceso      VARCHAR2(200);
    lStrFecha            VARCHAR2(50);
    lStrLeyenda          VARCHAR2(400);
    lStrDireccion        VARCHAR2(200);
    lStrFueraLinea       VARCHAR2(10);
    lIntIdFeel           NUMBER(15);
    lIntNroFactura       NUMBER(15);
    lIntContingencia     NUMBER(1);
BEGIN
    pOutCodError := 'COD000';
    pOutDesError := NULL;

    IF pInIdComplemento IS NULL OR pInNroSolicitud IS NULL OR pInEnviado IS NULL THEN
        pOutCodError := 'WEB010';
        pOutDesError := 'Faltan el complemento, la solicitud o el resultado de FEEL';
        RAISE lExcError;
    END IF;

    SELECT COUNT(*)
      INTO lIntExiste
      FROM GANADERO.CI_FACTURA_COMPLEMENTO
     WHERE IDENTIFICADOR = pInIdComplemento
       AND TZ_LOCK = 0;

    IF lIntExiste <> 1 THEN
        pOutCodError := 'WEB011';
        pOutDesError := 'No esta la factura del canal web para actualizar';
        RAISE lExcError;
    END IF;

    IF pInJsonRespuesta IS NOT NULL THEN
        BEGIN
            SELECT JSON_VALUE(pInJsonRespuesta, '$.proceso.cuf' RETURNING VARCHAR2(100)),
                   JSON_VALUE(pInJsonRespuesta, '$.proceso.cufd' RETURNING VARCHAR2(200)),
                   JSON_VALUE(pInJsonRespuesta, '$.proceso.idDocFiscalFEEL' RETURNING NUMBER(15)),
                   JSON_VALUE(pInJsonRespuesta, '$.proceso.fueraLinea' RETURNING VARCHAR2(10)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.cuf' RETURNING VARCHAR2(100)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.cufd' RETURNING VARCHAR2(200)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.numeroFactura' RETURNING NUMBER(15)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.leyenda' RETURNING VARCHAR2(400)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.fechaEmision' RETURNING VARCHAR2(50)),
                   JSON_VALUE(pInJsonRespuesta, '$.facturaCompraVenta.cabecera.direccion' RETURNING VARCHAR2(200))
              INTO lStrCuf, lStrCufdProceso, lIntIdFeel, lStrFueraLinea,
                   lStrCufCabecera, lStrCufdCabecera, lIntNroFactura, lStrLeyenda, lStrFecha, lStrDireccion
              FROM DUAL;

            IF lIntNroFactura IS NULL AND lStrCufCabecera IS NULL AND lStrLeyenda IS NULL AND lStrFecha IS NULL THEN
                lStrCufCabecera := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.cuf' RETURNING VARCHAR2(100));
                lStrCufdCabecera := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.cufd' RETURNING VARCHAR2(200));
                lIntNroFactura := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.numeroFactura' RETURNING NUMBER(15));
                lStrLeyenda := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.leyenda' RETURNING VARCHAR2(400));
                lStrFecha := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.fechaEmision' RETURNING VARCHAR2(50));
                lStrDireccion := JSON_VALUE(pInJsonRespuesta, '$.notaMonedaExtranjera.cabecera.direccion' RETURNING VARCHAR2(200));
            END IF;
        EXCEPTION
            WHEN OTHERS THEN
                lStrCuf := NULL;
        END;
    END IF;

    IF lStrCuf IS NULL THEN
        lStrCuf := lStrCufCabecera;
    END IF;
    lStrCufd := CASE WHEN lStrCufdProceso IS NOT NULL THEN lStrCufdProceso ELSE lStrCufdCabecera END;
    lIntContingencia := CASE WHEN LOWER(lStrFueraLinea) = 'true' THEN 1 ELSE 0 END;

    IF pInEnviado = 1 THEN
        UPDATE GANADERO.CI_FACTURA_COMPLEMENTO
           SET CUF = NVL(lStrCuf, CUF),
               CUFD = NVL(lStrCufd, CUFD),
               ID_DOC_FISCAL_FEEL = NVL(lIntIdFeel, ID_DOC_FISCAL_FEEL),
               NRO_FACTURA_SIN = NVL(lIntNroFactura, NRO_FACTURA_SIN),
               LEYENDA = NVL(lStrLeyenda, LEYENDA),
               FECHA_EMISION_SIN = NVL(lStrFecha, FECHA_EMISION_SIN),
               DIRECCION_SUCURSAL_SIN = NVL(lStrDireccion, DIRECCION_SUCURSAL_SIN),
               FECHA_EMISION = CASE
                   WHEN lStrFecha IS NOT NULL AND LENGTH(lStrFecha) >= 14
                       THEN TO_DATE(SUBSTR(lStrFecha, 1, 14), 'YYYYMMDDHH24MISS')
                   ELSE FECHA_EMISION
               END,
               CONTINGENCIA = lIntContingencia,
               COD_ESTADO_SOLICITUD = 'E',
               FECHA_MODIFICACION = TRUNC(SYSDATE),
               HORA_MODIFICACION = TO_CHAR(SYSDATE, 'HH24:MI:SS')
         WHERE IDENTIFICADOR = pInIdComplemento
           AND TZ_LOCK = 0;

        UPDATE GANADERO.FAC_SOLICITUDES_ENVIO
           SET COD_ESTADO = 'E',
               JSON_DATO_RESPUESTA = pInJsonRespuesta
         WHERE IDENTIFICADOR = pInNroSolicitud
           AND COD_CI_FACTURA_COMPLEMENTO = pInIdComplemento;
    ELSE
        UPDATE GANADERO.FAC_SOLICITUDES_ENVIO
           SET COD_ESTADO = 'O',
               JSON_DATO_RESPUESTA = NVL(pInJsonRespuesta, pInMensaje)
         WHERE IDENTIFICADOR = pInNroSolicitud
           AND COD_CI_FACTURA_COMPLEMENTO = pInIdComplemento;
    END IF;

EXCEPTION
    WHEN lExcError THEN
        NULL;
    WHEN OTHERS THEN
        pOutCodError := TO_CHAR(SQLCODE);
        pOutDesError := SQLERRM;
END SPR_ACTUALIZAR_RESPUESTA_FEEL;
/
