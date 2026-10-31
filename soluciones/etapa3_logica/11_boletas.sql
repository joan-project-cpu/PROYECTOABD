-- =============================================================================================
-- 11_boletas.sql  -  Boletas / comprobantes en texto plano
-- Ejecutar con la conexion VETERINARIA (despues del 07_paquetes.sql).
--
--   FN_BOLETA_MASCOTA     (ID_MASCOTA)      ficha imprimible de la mascota
--   FN_BOLETA_MEDICAMENTO (ID_MEDICAMENTO)  ficha de stock del insumo
--   FN_BOLETA_VACUNACION  (ID_VACUNACION)   comprobante de una vacuna aplicada
--
-- Cada funcion devuelve el texto ya armado (listo para mostrarse en un <pre>) o NULL si el ID no existe.
-- Las funciones auxiliares FN_BOLETA_TITULO / _LINEA / _PIE solo dan formato.
-- =============================================================================================

-- ---------------------------------------------------------------- auxiliares de formato
CREATE OR REPLACE FUNCTION FN_BOLETA_TITULO (p_titulo IN VARCHAR2)
    RETURN VARCHAR2
IS
    c_ancho CONSTANT PLS_INTEGER := 50;
    v_marca CONSTANT VARCHAR2(40) := 'VETCARE - CLINICA VETERINARIA';
BEGIN
    RETURN RPAD('=', c_ancho, '=') || CHR(10)
        || LPAD(v_marca,  TRUNC((c_ancho + LENGTH(v_marca))  / 2)) || CHR(10)
        || LPAD(p_titulo, TRUNC((c_ancho + LENGTH(p_titulo)) / 2)) || CHR(10)
        || RPAD('=', c_ancho, '=') || CHR(10);
END FN_BOLETA_TITULO;
/

CREATE OR REPLACE FUNCTION FN_BOLETA_LINEA (p_etiqueta IN VARCHAR2, p_valor IN VARCHAR2)
    RETURN VARCHAR2
IS
BEGIN
    RETURN RPAD(p_etiqueta, 16, '.') || ': ' || NVL(p_valor, '-') || CHR(10);
END FN_BOLETA_LINEA;
/

CREATE OR REPLACE FUNCTION FN_BOLETA_PIE
    RETURN VARCHAR2
IS
BEGIN
    RETURN RPAD('-', 50, '-') || CHR(10)
        || 'Emitido: ' || TO_CHAR(SYSDATE, 'DD/MM/YYYY HH24:MI') || CHR(10)
        || RPAD('=', 50, '=') || CHR(10);
END FN_BOLETA_PIE;
/

-- ---------------------------------------------------------------- ficha de la mascota
CREATE OR REPLACE FUNCTION FN_BOLETA_MASCOTA (p_id_mascota IN NUMBER)
    RETURN VARCHAR2
IS
    v_m          VW_MASCOTA_DETALLE%ROWTYPE;
    v_texto      VARCHAR2(4000);
    v_atenciones NUMBER;
    v_vacunas    NUMBER;
BEGIN
    SELECT * INTO v_m FROM VW_MASCOTA_DETALLE WHERE ID_MASCOTA = p_id_mascota;

    SELECT COUNT(*) INTO v_atenciones
      FROM ATENCION a
      JOIN CITA c ON c.ID_CITA = a.ID_CITA
     WHERE c.ID_MASCOTA = p_id_mascota;

    SELECT COUNT(*) INTO v_vacunas FROM VACUNACION WHERE ID_MASCOTA = p_id_mascota;

    v_texto := FN_BOLETA_TITULO('FICHA DE LA MASCOTA')
            || FN_BOLETA_LINEA('Codigo',        LPAD(v_m.ID_MASCOTA, 6, '0'))
            || FN_BOLETA_LINEA('Nombre',        v_m.NOMBRE)
            || FN_BOLETA_LINEA('Especie',       v_m.ESPECIE)
            || FN_BOLETA_LINEA('Raza',          v_m.RAZA)
            || FN_BOLETA_LINEA('Sexo',          CASE v_m.SEXO WHEN 'M' THEN 'Macho' ELSE 'Hembra' END)
            || FN_BOLETA_LINEA('Edad',          v_m.EDAD)
            || FN_BOLETA_LINEA('Color',         v_m.COLOR)
            || FN_BOLETA_LINEA('Esterilizado',  CASE v_m.ESTERILIZADO WHEN 'S' THEN 'Si' ELSE 'No' END)
            || FN_BOLETA_LINEA('Estado',        v_m.ESTADO)
            || RPAD('-', 50, '-') || CHR(10)
            || 'PROPIETARIO' || CHR(10)
            || FN_BOLETA_LINEA('Nombre',        v_m.PROPIETARIO)
            || FN_BOLETA_LINEA('DNI',           v_m.DNI)
            || FN_BOLETA_LINEA('Telefono',      v_m.TELEFONO)
            || RPAD('-', 50, '-') || CHR(10)
            || 'HISTORIAL' || CHR(10)
            || FN_BOLETA_LINEA('Atenciones',    TO_CHAR(v_atenciones))
            || FN_BOLETA_LINEA('Vacunas',       TO_CHAR(v_vacunas))
            || 'Ultimas vacunas:' || CHR(10);

    FOR r IN (SELECT vc.NOMBRE AS VACUNA, va.FECHA_APLICACION
                FROM VACUNACION va
                JOIN VACUNA vc ON vc.ID_VACUNA = va.ID_VACUNA
               WHERE va.ID_MASCOTA = p_id_mascota
               ORDER BY va.FECHA_APLICACION DESC, va.ID_VACUNACION DESC
               FETCH FIRST 3 ROWS ONLY) LOOP
        v_texto := v_texto || '  - ' || RPAD(SUBSTR(r.VACUNA, 1, 28), 28) || ' '
                           || TO_CHAR(r.FECHA_APLICACION, 'DD/MM/YYYY') || CHR(10);
    END LOOP;

    IF v_vacunas = 0 THEN
        v_texto := v_texto || '  (sin vacunas registradas)' || CHR(10);
    END IF;

    RETURN v_texto || FN_BOLETA_PIE;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END FN_BOLETA_MASCOTA;
/

-- ---------------------------------------------------------------- ficha del medicamento
CREATE OR REPLACE FUNCTION FN_BOLETA_MEDICAMENTO (p_id_medicamento IN NUMBER)
    RETURN VARCHAR2
IS
    v_med   MEDICAMENTO%ROWTYPE;
    v_texto VARCHAR2(4000);
    v_hay   BOOLEAN := FALSE;
BEGIN
    SELECT * INTO v_med FROM MEDICAMENTO WHERE ID_MEDICAMENTO = p_id_medicamento;

    v_texto := FN_BOLETA_TITULO('FICHA DE INSUMO')
            || FN_BOLETA_LINEA('Codigo',        LPAD(v_med.ID_MEDICAMENTO, 6, '0'))
            || FN_BOLETA_LINEA('Nombre',        v_med.NOMBRE)
            || FN_BOLETA_LINEA('Presentacion',  v_med.PRESENTACION)
            || FN_BOLETA_LINEA('Unidad',        v_med.UNIDAD)
            || FN_BOLETA_LINEA('Stock actual',  TO_CHAR(v_med.STOCK_ACTUAL, 'FM999990.00'))
            || FN_BOLETA_LINEA('Stock minimo',  TO_CHAR(v_med.STOCK_MINIMO, 'FM999990.00'))
            || FN_BOLETA_LINEA('Situacion',
                   CASE WHEN v_med.ACTIVO = 'N' THEN 'INACTIVO'
                        WHEN v_med.STOCK_ACTUAL <= v_med.STOCK_MINIMO THEN 'STOCK BAJO - REPONER'
                        ELSE 'STOCK NORMAL' END)
            || RPAD('-', 50, '-') || CHR(10)
            || 'ULTIMOS MOVIMIENTOS' || CHR(10);

    FOR r IN (SELECT TIPO, CANTIDAD, FECHA
                FROM MOVIMIENTO_STOCK
               WHERE ID_MEDICAMENTO = p_id_medicamento
               ORDER BY FECHA DESC, ID_MOVIMIENTO DESC
               FETCH FIRST 5 ROWS ONLY) LOOP
        v_hay := TRUE;
        v_texto := v_texto || '  ' || TO_CHAR(r.FECHA, 'DD/MM/YYYY HH24:MI') || '  '
                           || RPAD(r.TIPO, 8)
                           || LPAD(TO_CHAR(r.CANTIDAD, 'FM999990.00'), 10) || CHR(10);
    END LOOP;

    IF NOT v_hay THEN
        v_texto := v_texto || '  (sin movimientos registrados)' || CHR(10);
    END IF;

    RETURN v_texto || FN_BOLETA_PIE;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END FN_BOLETA_MEDICAMENTO;
/

-- ---------------------------------------------------------------- comprobante de vacunacion
CREATE OR REPLACE FUNCTION FN_BOLETA_VACUNACION (p_id_vacunacion IN NUMBER)
    RETURN VARCHAR2
IS
    v_texto VARCHAR2(4000);
BEGIN
    FOR r IN (SELECT va.ID_VACUNACION, va.FECHA_APLICACION, va.PROXIMA_DOSIS, va.LOTE,
                     vc.NOMBRE AS VACUNA,
                     m.NOMBRE AS MASCOTA, m.ESPECIE, m.RAZA, m.PROPIETARIO, m.DNI,
                     vt.NOMBRES || ' ' || vt.APELLIDOS AS VETERINARIO, vt.CMVP
                FROM VACUNACION va
                JOIN VACUNA vc            ON vc.ID_VACUNA = va.ID_VACUNA
                JOIN VW_MASCOTA_DETALLE m ON m.ID_MASCOTA = va.ID_MASCOTA
                JOIN VETERINARIO vt       ON vt.ID_VETERINARIO = va.ID_VETERINARIO
               WHERE va.ID_VACUNACION = p_id_vacunacion) LOOP
        v_texto := FN_BOLETA_TITULO('COMPROBANTE DE VACUNACION')
                || FN_BOLETA_LINEA('N. registro',    LPAD(r.ID_VACUNACION, 6, '0'))
                || FN_BOLETA_LINEA('Vacuna',         r.VACUNA)
                || FN_BOLETA_LINEA('Fecha aplicada', TO_CHAR(r.FECHA_APLICACION, 'DD/MM/YYYY'))
                || FN_BOLETA_LINEA('Lote',           r.LOTE)
                || FN_BOLETA_LINEA('Proxima dosis',
                       NVL(TO_CHAR(r.PROXIMA_DOSIS, 'DD/MM/YYYY'), 'Dosis unica (sin refuerzo)'))
                || RPAD('-', 50, '-') || CHR(10)
                || FN_BOLETA_LINEA('Mascota',        r.MASCOTA)
                || FN_BOLETA_LINEA('Especie / raza', r.ESPECIE || ' / ' || r.RAZA)
                || FN_BOLETA_LINEA('Propietario',    r.PROPIETARIO)
                || FN_BOLETA_LINEA('DNI',            r.DNI)
                || RPAD('-', 50, '-') || CHR(10)
                || FN_BOLETA_LINEA('Veterinario',    r.VETERINARIO)
                || FN_BOLETA_LINEA('CMVP',           r.CMVP)
                || CHR(10) || CHR(10)
                || '  ______________________________' || CHR(10)
                || '  Firma y sello del veterinario' || CHR(10)
                || FN_BOLETA_PIE;
    END LOOP;

    RETURN v_texto;   -- NULL si el ID no existe
END FN_BOLETA_VACUNACION;
/

-- ---------------------------------------------------------------- comprobacion
-- No debe devolver filas (si devuelve algo, hay un error de compilacion):
SELECT name, type, line, text FROM user_errors WHERE name LIKE 'FN_BOLETA%';

-- Prueba rapida (cambie los IDs por otros que existan):
-- SELECT FN_BOLETA_MASCOTA(1)     FROM DUAL;
-- SELECT FN_BOLETA_MEDICAMENTO(1) FROM DUAL;
-- SELECT FN_BOLETA_VACUNACION(1)  FROM DUAL;
