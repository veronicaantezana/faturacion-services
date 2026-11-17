-- Esquema FACTURACION. Empresas, sucursales (F_CONFIG) y puntos de venta (F_POS).
-- En DBeaver, conexion DEV - FACTURACION.
-- El delimitador de script es la barra / en una linea sola.
-- Ejecutar con "Execute SQL Script" (Alt+X). No uses Ctrl+Enter.
-- El procedimiento no hace COMMIT. La transaccion la confirma quien lo llama.

CREATE OR REPLACE PACKAGE FACTURACION.PKG_REGISTRO AS

    PROCEDURE SPR_REGISTRAR_EMPRESA (
        pInNit                    IN VARCHAR2,
        pInNombre                 IN VARCHAR2,
        pInEstado                 IN VARCHAR2,
        pInModalidad              IN NUMBER,
        pInUsuario                IN VARCHAR2,
        pInCodigoAutorizacion     IN VARCHAR2 DEFAULT NULL,
        pInKeyName                IN VARCHAR2 DEFAULT NULL,
        pInCodigoSistema          IN VARCHAR2 DEFAULT NULL,
        pInAdminSequence          IN NUMBER DEFAULT NULL,
        pInAdminSeqNota           IN NUMBER DEFAULT NULL,
        pInEnviarCorreo           IN NUMBER DEFAULT NULL,
        pInCompletarDatos         IN NUMBER DEFAULT NULL,
        pInUrlDescarga            IN VARCHAR2 DEFAULT NULL,
        pInDestNotificacion       IN VARCHAR2 DEFAULT NULL,
        pInDocSectorValidate      IN VARCHAR2 DEFAULT NULL,
        pInGenerarPdf             IN NUMBER DEFAULT NULL,
        pInTokenEnabled           IN NUMBER DEFAULT NULL,
        pInCorreoIndividual       IN NUMBER DEFAULT NULL,
        pInPlanVigente            IN VARCHAR2 DEFAULT NULL,
        pInPeriodoPago            IN VARCHAR2 DEFAULT NULL,
        pInMaxFacturas            IN NUMBER DEFAULT NULL,
        pInConciliacion           IN NUMBER DEFAULT NULL,
        pInNombreComercial        IN VARCHAR2 DEFAULT NULL,
        pInFuncionalida           IN NUMBER DEFAULT NULL,
        pInGenerarPdfCodSistema   IN NUMBER DEFAULT NULL,
        pOutIdEmpresa             OUT NUMBER,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_LISTAR_EMPRESAS (
        pOutDatos                 OUT SYS_REFCURSOR,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_REGISTRAR_SUCURSAL (
        pInIdEmpresa              IN NUMBER,
        pInCodigo                 IN VARCHAR2,
        pInReferencia             IN VARCHAR2,
        pInSucursal               IN NUMBER,
        pInPos                    IN NUMBER,
        pInDireccion              IN VARCHAR2,
        pInNroDocSequence         IN NUMBER,
        pInNroNotaSequence        IN NUMBER,
        pInEnabled                IN NUMBER,
        pInUsuario                IN VARCHAR2,
        pInHoraCufd               IN NUMBER DEFAULT NULL,
        pInTelefono               IN VARCHAR2 DEFAULT NULL,
        pInMunicipio              IN VARCHAR2 DEFAULT NULL,
        pInKeyName                IN VARCHAR2 DEFAULT NULL,
        pOutIdConfig              OUT NUMBER,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_LISTAR_SUCURSALES (
        pOutDatos                 OUT SYS_REFCURSOR,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_REGISTRAR_PUNTO_VENTA (
        pInIdConfig               IN NUMBER,
        pInDescripcion            IN VARCHAR2,
        pInPos                    IN NUMBER,
        pInTipoPuntoVenta         IN NUMBER,
        pInNombrePuntoVenta       IN VARCHAR2,
        pInTipoPing               IN NUMBER,
        pInEnabled                IN NUMBER,
        pInUsuario                IN VARCHAR2,
        pInDireccionIp            IN VARCHAR2 DEFAULT NULL,
        pInNumeroContrato         IN VARCHAR2 DEFAULT NULL,
        pInNitComisionista        IN VARCHAR2 DEFAULT NULL,
        pInFechaInicio            IN DATE DEFAULT NULL,
        pInFechaFin               IN DATE DEFAULT NULL,
        pInObtenerCufd            IN NUMBER DEFAULT NULL,
        pInEstadoSiat             IN NUMBER DEFAULT NULL,
        pOutIdPos                 OUT NUMBER,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_LISTAR_PUNTOS_VENTA (
        pOutDatos                 OUT SYS_REFCURSOR,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_LISTAR_PRODUCTOS (
        pInIdEmpresa              IN NUMBER,
        pOutDatos                 OUT SYS_REFCURSOR,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_LISTAR_PRODUCTOS_SIN (
        pInIdEmpresa              IN NUMBER,
        pOutDatos                 OUT SYS_REFCURSOR,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

    PROCEDURE SPR_REGISTRAR_PRODUCTO (
        pInIdEmpresa              IN NUMBER,
        pInIdProductoSin          IN NUMBER,
        pInUsuario                IN VARCHAR2,
        pOutIdMapeo               OUT NUMBER,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    );

END PKG_REGISTRO;
/

CREATE OR REPLACE PACKAGE BODY FACTURACION.PKG_REGISTRO AS

    FUNCTION fn_texto (pInValor IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
        IF pInValor IS NULL OR TRIM(pInValor) IS NULL THEN
            RETURN NULL;
        END IF;
        RETURN TRIM(pInValor);
    END fn_texto;

    PROCEDURE spr_validar_bandera (
        pInValor IN NUMBER,
        pInCampo IN VARCHAR2
    ) IS
    BEGIN
        IF pInValor IS NOT NULL AND pInValor NOT IN (0, 1) THEN
            RAISE_APPLICATION_ERROR(-20001, pInCampo || ' debe ser 0 o 1');
        END IF;
    END spr_validar_bandera;

    FUNCTION fn_secuencia (
        pInPrimera IN VARCHAR2,
        pInSegunda IN VARCHAR2,
        pInTercera IN VARCHAR2
    ) RETURN VARCHAR2 IS
        vStrNombre VARCHAR2(128);
    BEGIN
        SELECT sequence_name
          INTO vStrNombre
          FROM (
                SELECT sequence_name
                  FROM all_sequences
                 WHERE sequence_owner = 'FACTURACION'
                   AND sequence_name IN (pInPrimera, pInSegunda, pInTercera)
                 ORDER BY CASE sequence_name
                              WHEN pInPrimera THEN 1
                              WHEN pInSegunda THEN 2
                              ELSE 3
                          END
               )
         WHERE ROWNUM = 1;
        RETURN vStrNombre;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'No existe la secuencia ' || pInPrimera);
    END fn_secuencia;

    PROCEDURE SPR_REGISTRAR_EMPRESA (
        pInNit                    IN VARCHAR2,
        pInNombre                 IN VARCHAR2,
        pInEstado                 IN VARCHAR2,
        pInModalidad              IN NUMBER,
        pInUsuario                IN VARCHAR2,
        pInCodigoAutorizacion     IN VARCHAR2,
        pInKeyName                IN VARCHAR2,
        pInCodigoSistema          IN VARCHAR2,
        pInAdminSequence          IN NUMBER,
        pInAdminSeqNota           IN NUMBER,
        pInEnviarCorreo           IN NUMBER,
        pInCompletarDatos         IN NUMBER,
        pInUrlDescarga            IN VARCHAR2,
        pInDestNotificacion       IN VARCHAR2,
        pInDocSectorValidate      IN VARCHAR2,
        pInGenerarPdf             IN NUMBER,
        pInTokenEnabled           IN NUMBER,
        pInCorreoIndividual       IN NUMBER,
        pInPlanVigente            IN VARCHAR2,
        pInPeriodoPago            IN VARCHAR2,
        pInMaxFacturas            IN NUMBER,
        pInConciliacion           IN NUMBER,
        pInNombreComercial        IN VARCHAR2,
        pInFuncionalida           IN NUMBER,
        pInGenerarPdfCodSistema   IN NUMBER,
        pOutIdEmpresa             OUT NUMBER,
        pOutCodError              OUT NUMBER,
        pOutDesError              OUT VARCHAR2
    ) IS
        vStrNit                 VARCHAR2(20);
        vStrNombre              VARCHAR2(200);
        vStrEstado              VARCHAR2(5);
        vStrUsuario             VARCHAR2(100);
        vStrCodigoAutorizacion  VARCHAR2(500);
        vStrKeyName             VARCHAR2(500);
        vStrCodigoSistema       VARCHAR2(500);
        vStrUrlDescarga         VARCHAR2(1000);
        vStrDestNotificacion    VARCHAR2(500);
        vStrDocSectorValidate   VARCHAR2(500);
        vStrPlanVigente         VARCHAR2(100);
        vStrPeriodoPago         VARCHAR2(100);
        vStrNombreComercial     VARCHAR2(500);
        vNumExiste              NUMBER;
    BEGIN
        pOutIdEmpresa := NULL;
        pOutCodError := 0;
        pOutDesError := NULL;

        vStrNit := fn_texto(pInNit);
        vStrNombre := fn_texto(pInNombre);
        vStrEstado := fn_texto(pInEstado);
        vStrUsuario := fn_texto(pInUsuario);
        vStrCodigoAutorizacion := fn_texto(pInCodigoAutorizacion);
        vStrKeyName := fn_texto(pInKeyName);
        vStrCodigoSistema := fn_texto(pInCodigoSistema);
        vStrUrlDescarga := fn_texto(pInUrlDescarga);
        vStrDestNotificacion := fn_texto(pInDestNotificacion);
        vStrDocSectorValidate := fn_texto(pInDocSectorValidate);
        vStrPlanVigente := fn_texto(pInPlanVigente);
        vStrPeriodoPago := fn_texto(pInPeriodoPago);
        vStrNombreComercial := fn_texto(pInNombreComercial);

        IF vStrNit IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El NIT es obligatorio');
        END IF;
        IF vStrNombre IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El nombre de la empresa es obligatorio');
        END IF;
        IF vStrEstado IS NULL OR vStrEstado NOT IN ('0', '1') THEN
            RAISE_APPLICATION_ERROR(-20001, 'El estado de la empresa debe ser 1 o 0');
        END IF;
        IF pInModalidad IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La modalidad es obligatoria');
        END IF;
        IF vStrUsuario IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Falta el usuario que registra la empresa');
        END IF;

        spr_validar_bandera(pInEnviarCorreo, 'enviarCorreo');
        spr_validar_bandera(pInCompletarDatos, 'completarDatos');
        spr_validar_bandera(pInGenerarPdf, 'generarPdf');
        spr_validar_bandera(pInTokenEnabled, 'tokenEnabled');
        spr_validar_bandera(pInCorreoIndividual, 'correoIndividual');
        spr_validar_bandera(pInConciliacion, 'conciliacion');
        spr_validar_bandera(pInFuncionalida, 'funcionalida');

        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_EMPRESA
         WHERE NIT = vStrNit
           AND ROWNUM = 1;
        IF vNumExiste > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Ya existe una empresa con el NIT ' || vStrNit);
        END IF;

        INSERT INTO FACTURACION.F_EMPRESA (
            ID_EMPRESA, NIT, NOMBRE, ESTADO, MODALIDAD, CODIGO_AUTORIZACION, KEY_NAME, CODIGO_SISTEMA,
            ADMIN_SEQUENCE, ADMIN_SEQ_NOTA, LAST_USER, LAST_STAMP, ENVIAR_CORREO, COMPLETAR_DATOS,
            URL_DESCARGA, DEST_NOTIFICACION, DOC_SECTOR_VALIDATE, GENERAR_PDF, TOKEN_ENABLED,
            CORREO_INDIVIDUAL, PLAN_VIGENTE, PERIODO_PAGO, MAX_FACTURAS, CONCILIACION, NOMBRECOMERCIAL,
            FUNCIONALIDA, GENERAR_PDF_COD_SISTEMA
        ) VALUES (
            FACTURACION.S_F_EMPRESA.NEXTVAL, vStrNit, vStrNombre, vStrEstado, pInModalidad,
            vStrCodigoAutorizacion, vStrKeyName, vStrCodigoSistema,
            pInAdminSequence, pInAdminSeqNota, vStrUsuario, SYSTIMESTAMP,
            pInEnviarCorreo, pInCompletarDatos, vStrUrlDescarga, vStrDestNotificacion,
            vStrDocSectorValidate, pInGenerarPdf, pInTokenEnabled, pInCorreoIndividual,
            vStrPlanVigente, vStrPeriodoPago, pInMaxFacturas, pInConciliacion,
            vStrNombreComercial, pInFuncionalida, pInGenerarPdfCodSistema
        ) RETURNING ID_EMPRESA INTO pOutIdEmpresa;

        pOutDesError := 'OK';
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
    END SPR_REGISTRAR_EMPRESA;

    PROCEDURE SPR_LISTAR_EMPRESAS (
        pOutDatos    OUT SYS_REFCURSOR,
        pOutCodError OUT NUMBER,
        pOutDesError OUT VARCHAR2
    ) IS
    BEGIN
        pOutCodError := 0;
        pOutDesError := 'OK';
        OPEN pOutDatos FOR
            SELECT ID_EMPRESA, NIT, NOMBRE, ESTADO, MODALIDAD, CODIGO_AUTORIZACION, KEY_NAME, CODIGO_SISTEMA,
                   ADMIN_SEQUENCE, ADMIN_SEQ_NOTA, LAST_USER, LAST_STAMP, ENVIAR_CORREO, COMPLETAR_DATOS,
                   URL_DESCARGA, DEST_NOTIFICACION, DOC_SECTOR_VALIDATE, GENERAR_PDF, TOKEN_ENABLED,
                   CORREO_INDIVIDUAL, PLAN_VIGENTE, PERIODO_PAGO, MAX_FACTURAS, CONCILIACION, NOMBRECOMERCIAL,
                   FUNCIONALIDA, GENERAR_PDF_COD_SISTEMA
              FROM FACTURACION.F_EMPRESA
             ORDER BY ID_EMPRESA;
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
            OPEN pOutDatos FOR SELECT NULL AS ID_EMPRESA FROM DUAL WHERE 1 = 0;
    END SPR_LISTAR_EMPRESAS;

    PROCEDURE SPR_REGISTRAR_SUCURSAL (
        pInIdEmpresa         IN NUMBER,
        pInCodigo            IN VARCHAR2,
        pInReferencia        IN VARCHAR2,
        pInSucursal          IN NUMBER,
        pInPos               IN NUMBER,
        pInDireccion         IN VARCHAR2,
        pInNroDocSequence    IN NUMBER,
        pInNroNotaSequence   IN NUMBER,
        pInEnabled           IN NUMBER,
        pInUsuario           IN VARCHAR2,
        pInHoraCufd          IN NUMBER,
        pInTelefono          IN VARCHAR2,
        pInMunicipio         IN VARCHAR2,
        pInKeyName           IN VARCHAR2,
        pOutIdConfig         OUT NUMBER,
        pOutCodError         OUT NUMBER,
        pOutDesError         OUT VARCHAR2
    ) IS
        vStrCodigo     VARCHAR2(20);
        vStrReferencia VARCHAR2(200);
        vStrDireccion  VARCHAR2(1000);
        vStrUsuario    VARCHAR2(100);
        vStrTelefono   VARCHAR2(100);
        vStrMunicipio  VARCHAR2(200);
        vStrKeyName    VARCHAR2(500);
        vStrSecuencia  VARCHAR2(128);
        vStrSql        VARCHAR2(4000);
        vNumExiste     NUMBER;
    BEGIN
        pOutIdConfig := NULL;
        pOutCodError := 0;
        pOutDesError := NULL;

        vStrCodigo := fn_texto(pInCodigo);
        vStrReferencia := fn_texto(pInReferencia);
        vStrDireccion := fn_texto(pInDireccion);
        vStrUsuario := fn_texto(pInUsuario);
        vStrTelefono := fn_texto(pInTelefono);
        vStrMunicipio := fn_texto(pInMunicipio);
        vStrKeyName := fn_texto(pInKeyName);

        IF pInIdEmpresa IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La empresa es obligatoria');
        END IF;
        IF vStrCodigo IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El codigo de la sucursal es obligatorio');
        END IF;
        IF vStrReferencia IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La referencia de la sucursal es obligatoria');
        END IF;
        IF vStrDireccion IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La direccion de la sucursal es obligatoria');
        END IF;
        IF pInSucursal IS NULL OR pInPos IS NULL OR pInSucursal < 0 OR pInPos < 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'La sucursal y el punto de venta no pueden ser negativos');
        END IF;
        IF pInEnabled IS NULL OR pInEnabled NOT IN (0, 1) THEN
            RAISE_APPLICATION_ERROR(-20001, 'enabled debe ser 0 o 1');
        END IF;
        IF pInNroDocSequence IS NULL OR pInNroNotaSequence IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Las secuencias de documento y nota son obligatorias');
        END IF;
        IF vStrUsuario IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Falta el usuario que registra la sucursal');
        END IF;
        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_EMPRESA
         WHERE ID_EMPRESA = pInIdEmpresa
           AND ROWNUM = 1;
        IF vNumExiste = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'No existe la empresa ' || pInIdEmpresa);
        END IF;
        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_CONFIG
         WHERE ID_EMPRESA = pInIdEmpresa
           AND SUCURSAL = pInSucursal
           AND POS = pInPos
           AND ROWNUM = 1;
        IF vNumExiste > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Esa empresa ya tiene la sucursal ' || pInSucursal
                || ' con punto de venta ' || pInPos);
        END IF;

        vStrSecuencia := fn_secuencia('S_F_CONFIG', 'SEQ_F_CONFIG', 'S_CONFIG');
        vStrSql := 'INSERT INTO FACTURACION.F_CONFIG ('
            || 'ID_CONFIG, ID_EMPRESA, CODIGO, REFERENCIA, SUCURSAL, POS, DIRECCION, '
            || 'NRO_DOC_SEQUENCE, NRO_NOTA_SEQUENCE, ENABLED, LAST_USER, LAST_STAMP, '
            || 'HORA_CUFD, TELEFONO, MUNICIPIO, KEYNAME'
            || ') VALUES (FACTURACION.' || vStrSecuencia || '.NEXTVAL, '
            || ':1, :2, :3, :4, :5, :6, :7, :8, :9, :10, SYSTIMESTAMP, :11, :12, :13, :14) '
            || 'RETURNING ID_CONFIG INTO :15';

        EXECUTE IMMEDIATE vStrSql
            USING pInIdEmpresa, vStrCodigo, vStrReferencia, pInSucursal, pInPos, vStrDireccion,
                  pInNroDocSequence, pInNroNotaSequence, pInEnabled, vStrUsuario,
                  pInHoraCufd, vStrTelefono, vStrMunicipio, vStrKeyName,
                  OUT pOutIdConfig;

        pOutDesError := 'OK';
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
    END SPR_REGISTRAR_SUCURSAL;

    PROCEDURE SPR_LISTAR_SUCURSALES (
        pOutDatos    OUT SYS_REFCURSOR,
        pOutCodError OUT NUMBER,
        pOutDesError OUT VARCHAR2
    ) IS
    BEGIN
        pOutCodError := 0;
        pOutDesError := 'OK';
        OPEN pOutDatos FOR
            SELECT c.ID_CONFIG, c.ID_EMPRESA, e.NOMBRE, e.NIT, c.CODIGO, c.REFERENCIA, c.SUCURSAL, c.POS,
                   c.DIRECCION, c.NRO_DOC_SEQUENCE, c.NRO_NOTA_SEQUENCE, c.ENABLED, c.LAST_USER, c.LAST_STAMP,
                   c.HORA_CUFD, c.TELEFONO, c.MUNICIPIO, c.KEYNAME
              FROM FACTURACION.F_CONFIG c
              LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
             ORDER BY c.ID_CONFIG DESC;
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
            OPEN pOutDatos FOR SELECT NULL AS ID_CONFIG FROM DUAL WHERE 1 = 0;
    END SPR_LISTAR_SUCURSALES;

    PROCEDURE SPR_REGISTRAR_PUNTO_VENTA (
        pInIdConfig          IN NUMBER,
        pInDescripcion       IN VARCHAR2,
        pInPos               IN NUMBER,
        pInTipoPuntoVenta    IN NUMBER,
        pInNombrePuntoVenta  IN VARCHAR2,
        pInTipoPing          IN NUMBER,
        pInEnabled           IN NUMBER,
        pInUsuario           IN VARCHAR2,
        pInDireccionIp       IN VARCHAR2,
        pInNumeroContrato    IN VARCHAR2,
        pInNitComisionista   IN VARCHAR2,
        pInFechaInicio       IN DATE,
        pInFechaFin          IN DATE,
        pInObtenerCufd       IN NUMBER,
        pInEstadoSiat        IN NUMBER,
        pOutIdPos            OUT NUMBER,
        pOutCodError         OUT NUMBER,
        pOutDesError         OUT VARCHAR2
    ) IS
        vStrDescripcion    VARCHAR2(200);
        vStrNombre         VARCHAR2(100);
        vStrUsuario        VARCHAR2(100);
        vStrDireccionIp    VARCHAR2(100);
        vStrNumeroContrato VARCHAR2(100);
        vStrNitComisionista VARCHAR2(30);
        vNumPosConfig      NUMBER;
        vNumExiste         NUMBER;
        vStrSecuencia      VARCHAR2(128);
        vStrSql            VARCHAR2(4000);
    BEGIN
        pOutIdPos := NULL;
        pOutCodError := 0;
        pOutDesError := NULL;

        vStrDescripcion := fn_texto(pInDescripcion);
        vStrNombre := fn_texto(pInNombrePuntoVenta);
        vStrUsuario := fn_texto(pInUsuario);
        vStrDireccionIp := fn_texto(pInDireccionIp);
        vStrNumeroContrato := fn_texto(pInNumeroContrato);
        vStrNitComisionista := fn_texto(pInNitComisionista);

        IF pInIdConfig IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La sucursal es obligatoria');
        END IF;
        IF vStrDescripcion IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La descripcion del punto de venta es obligatoria');
        END IF;
        IF vStrNombre IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El nombre del punto de venta es obligatorio');
        END IF;
        IF pInPos IS NULL OR pInPos < 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'El punto de venta no puede ser negativo');
        END IF;
        IF pInTipoPuntoVenta IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El tipo de punto de venta es obligatorio');
        END IF;
        IF pInTipoPing IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El tipo de ping es obligatorio');
        END IF;
        IF pInEnabled IS NULL OR pInEnabled NOT IN (0, 1) THEN
            RAISE_APPLICATION_ERROR(-20001, 'enabled debe ser 0 o 1');
        END IF;
        IF vStrUsuario IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Falta el usuario que registra el punto de venta');
        END IF;

        BEGIN
            SELECT POS INTO vNumPosConfig
              FROM FACTURACION.F_CONFIG
             WHERE ID_CONFIG = pInIdConfig;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20001, 'No existe la sucursal ' || pInIdConfig);
        END;

        IF vNumPosConfig <> pInPos THEN
            RAISE_APPLICATION_ERROR(-20001, 'La sucursal ' || pInIdConfig
                || ' tiene el punto de venta ' || vNumPosConfig
                || '. F_POS debe usar ese mismo numero');
        END IF;
        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_POS
         WHERE ID_CONFIG = pInIdConfig
           AND ROWNUM = 1;
        IF vNumExiste > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'La sucursal ' || pInIdConfig || ' ya tiene un punto de venta en F_POS');
        END IF;

        vStrSecuencia := fn_secuencia('S_F_POS', 'SEQ_F_POS', 'S_POS');
        vStrSql := 'INSERT INTO FACTURACION.F_POS ('
            || 'ID_POS, ID_CONFIG, DESCRIPCION, DIRECCION_IP, POS, TIPO_PUNTO_VENTA, NOMBRE_PUNTO_VENTA, '
            || 'TIPO_PING, ENABLED, LAST_USER, LAST_STAMP, NUMERO_CONTRATO, NIT_COMISIONISTA, '
            || 'FECHA_INICIO, FECHA_FIN, OBTENER_CUFD, ESTADO_SIAT'
            || ') VALUES (FACTURACION.' || vStrSecuencia || '.NEXTVAL, '
            || ':1, :2, :3, :4, :5, :6, :7, :8, :9, SYSTIMESTAMP, :10, :11, :12, :13, :14, :15) '
            || 'RETURNING ID_POS INTO :16';

        EXECUTE IMMEDIATE vStrSql
            USING pInIdConfig, vStrDescripcion, vStrDireccionIp, pInPos, pInTipoPuntoVenta, vStrNombre,
                  pInTipoPing, pInEnabled, vStrUsuario, vStrNumeroContrato, vStrNitComisionista,
                  pInFechaInicio, pInFechaFin, pInObtenerCufd, pInEstadoSiat,
                  OUT pOutIdPos;

        pOutDesError := 'OK';
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
    END SPR_REGISTRAR_PUNTO_VENTA;

    PROCEDURE SPR_LISTAR_PUNTOS_VENTA (
        pOutDatos    OUT SYS_REFCURSOR,
        pOutCodError OUT NUMBER,
        pOutDesError OUT VARCHAR2
    ) IS
    BEGIN
        pOutCodError := 0;
        pOutDesError := 'OK';
        OPEN pOutDatos FOR
            SELECT p.ID_POS, p.ID_CONFIG, c.ID_EMPRESA, e.NOMBRE, e.NIT, c.CODIGO, c.REFERENCIA, c.SUCURSAL,
                   p.DESCRIPCION, p.DIRECCION_IP, p.POS, p.TIPO_PUNTO_VENTA, p.NOMBRE_PUNTO_VENTA, p.TIPO_PING,
                   p.ENABLED, p.LAST_USER, p.LAST_STAMP, p.NUMERO_CONTRATO, p.NIT_COMISIONISTA,
                   p.FECHA_INICIO, p.FECHA_FIN, p.OBTENER_CUFD, p.ESTADO_SIAT
              FROM FACTURACION.F_POS p
              LEFT JOIN FACTURACION.F_CONFIG c ON c.ID_CONFIG = p.ID_CONFIG
              LEFT JOIN FACTURACION.F_EMPRESA e ON e.ID_EMPRESA = c.ID_EMPRESA
             ORDER BY p.ID_POS DESC;
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
            OPEN pOutDatos FOR SELECT NULL AS ID_POS FROM DUAL WHERE 1 = 0;
    END SPR_LISTAR_PUNTOS_VENTA;

    PROCEDURE SPR_LISTAR_PRODUCTOS (
        pInIdEmpresa IN NUMBER,
        pOutDatos    OUT SYS_REFCURSOR,
        pOutCodError OUT NUMBER,
        pOutDesError OUT VARCHAR2
    ) IS
        vNumExiste NUMBER;
    BEGIN
        pOutCodError := 0;
        pOutDesError := 'OK';

        IF pInIdEmpresa IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La empresa es obligatoria');
        END IF;

        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_EMPRESA
         WHERE ID_EMPRESA = pInIdEmpresa
           AND ROWNUM = 1;
        IF vNumExiste = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'No existe la empresa ' || pInIdEmpresa);
        END IF;

        OPEN pOutDatos FOR
            SELECT m.ID_MAPEO_PRODUCTO,
                   NVL(NULLIF(RTRIM(TRIM(m.CODIGO_PRODUCTO), ','), ''), TO_CHAR(p.CODIGO_PRODUCTO_SIN)) AS CODIGO_PRODUCTO,
                   p.DESCRIPCION,
                   p.CODIGO_ACTIVIDAD,
                   (SELECT MIN(a.DESCRIPTION)
                      FROM FACTURACION.F_ACT_ECONOMICA a
                     WHERE a.ID_EMPRESA = m.ID_EMPRESA
                       AND a.CODIGO = p.CODIGO_ACTIVIDAD
                       AND a.ENABLED = 1) AS DESCRIPCION_ACTIVIDAD,
                   p.CODIGO_PRODUCTO_SIN
              FROM FACTURACION.F_MAPEO_PRODUCTO m
              JOIN FACTURACION.F_PRODUCTO_SIN p
                ON p.ID_PRODUCTO_SIN = m.ID_PRODUCTO_SIN
               AND p.ID_EMPRESA = m.ID_EMPRESA
             WHERE m.ID_EMPRESA = pInIdEmpresa
             ORDER BY p.CODIGO_ACTIVIDAD, p.CODIGO_PRODUCTO_SIN, m.ID_MAPEO_PRODUCTO;
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
            OPEN pOutDatos FOR SELECT NULL AS ID_MAPEO_PRODUCTO FROM DUAL WHERE 1 = 0;
    END SPR_LISTAR_PRODUCTOS;

    PROCEDURE SPR_LISTAR_PRODUCTOS_SIN (
        pInIdEmpresa IN NUMBER,
        pOutDatos    OUT SYS_REFCURSOR,
        pOutCodError OUT NUMBER,
        pOutDesError OUT VARCHAR2
    ) IS
        vNumExiste NUMBER;
    BEGIN
        pOutCodError := 0;
        pOutDesError := 'OK';

        IF pInIdEmpresa IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La empresa es obligatoria');
        END IF;

        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_EMPRESA
         WHERE ID_EMPRESA = pInIdEmpresa
           AND ROWNUM = 1;
        IF vNumExiste = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'No existe la empresa ' || pInIdEmpresa);
        END IF;

        OPEN pOutDatos FOR
            SELECT p.ID_PRODUCTO_SIN,
                   p.CODIGO_ACTIVIDAD,
                   p.CODIGO_PRODUCTO_SIN,
                   p.DESCRIPCION
              FROM FACTURACION.F_PRODUCTO_SIN p
             WHERE p.ID_EMPRESA = pInIdEmpresa
               AND p.ENABLED = 1
               AND NOT EXISTS (
                    SELECT 1
                      FROM FACTURACION.F_MAPEO_PRODUCTO m
                     WHERE m.ID_EMPRESA = p.ID_EMPRESA
                       AND m.ID_PRODUCTO_SIN = p.ID_PRODUCTO_SIN
               )
             ORDER BY p.CODIGO_ACTIVIDAD, p.CODIGO_PRODUCTO_SIN, p.ID_PRODUCTO_SIN;
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
            OPEN pOutDatos FOR SELECT NULL AS ID_PRODUCTO_SIN FROM DUAL WHERE 1 = 0;
    END SPR_LISTAR_PRODUCTOS_SIN;

    PROCEDURE SPR_REGISTRAR_PRODUCTO (
        pInIdEmpresa     IN NUMBER,
        pInIdProductoSin IN NUMBER,
        pInUsuario       IN VARCHAR2,
        pOutIdMapeo      OUT NUMBER,
        pOutCodError     OUT NUMBER,
        pOutDesError     OUT VARCHAR2
    ) IS
        vNumExiste   NUMBER;
        vNumCodigo   NUMBER;
        vStrUsuario  VARCHAR2(100);
        vStrCodigo   VARCHAR2(100);
    BEGIN
        pOutIdMapeo := NULL;
        pOutCodError := 0;
        pOutDesError := NULL;

        vStrUsuario := fn_texto(pInUsuario);
        IF pInIdEmpresa IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'La empresa es obligatoria');
        END IF;
        IF pInIdProductoSin IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'El producto del SIN es obligatorio');
        END IF;
        IF vStrUsuario IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Falta el usuario que registra el producto');
        END IF;

        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_EMPRESA
         WHERE ID_EMPRESA = pInIdEmpresa
           AND ROWNUM = 1;
        IF vNumExiste = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'No existe la empresa ' || pInIdEmpresa);
        END IF;

        BEGIN
            SELECT TO_CHAR(p.CODIGO_PRODUCTO_SIN) || ','
              INTO vStrCodigo
              FROM FACTURACION.F_PRODUCTO_SIN p
             WHERE p.ID_PRODUCTO_SIN = pInIdProductoSin
               AND p.ID_EMPRESA = pInIdEmpresa
               AND p.ENABLED = 1;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20001, 'El producto del SIN no pertenece a la empresa o no esta habilitado');
        END;

        SELECT COUNT(*)
          INTO vNumExiste
          FROM FACTURACION.F_MAPEO_PRODUCTO
         WHERE ID_EMPRESA = pInIdEmpresa
           AND ID_PRODUCTO_SIN = pInIdProductoSin
           AND ROWNUM = 1;
        IF vNumExiste > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Ese producto del SIN ya esta mapeado para la empresa');
        END IF;

        SELECT COUNT(*)
          INTO vNumCodigo
          FROM FACTURACION.F_MAPEO_PRODUCTO
         WHERE ID_EMPRESA = pInIdEmpresa
           AND CODIGO_PRODUCTO = vStrCodigo
           AND ROWNUM = 1;
        IF vNumCodigo > 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'La empresa ya tiene el codigo ' || vStrCodigo);
        END IF;

        INSERT INTO FACTURACION.F_MAPEO_PRODUCTO (
            ID_MAPEO_PRODUCTO, ID_PRODUCTO_SIN, ID_EMPRESA, CODIGO_NANDINA,
            LAST_USER, LAST_STAMP, CODIGO_PRODUCTO
        ) VALUES (
            FACTURACION.S_F_MAPEO_PRODUCTO.NEXTVAL, pInIdProductoSin, pInIdEmpresa, NULL,
            vStrUsuario, SYSTIMESTAMP, vStrCodigo
        ) RETURNING ID_MAPEO_PRODUCTO INTO pOutIdMapeo;

        pOutDesError := 'OK';
    EXCEPTION
        WHEN OTHERS THEN
            pOutCodError := SQLCODE;
            pOutDesError := SQLERRM;
    END SPR_REGISTRAR_PRODUCTO;

END PKG_REGISTRO;
/
