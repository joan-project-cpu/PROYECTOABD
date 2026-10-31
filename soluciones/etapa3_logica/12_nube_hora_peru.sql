-- =====================================================================
-- 12_nube_hora_peru.sql  -  SOLO para Autonomous Database (Oracle Cloud)
--
-- La base en la nube trabaja en UTC (5 horas adelantada respecto a Peru).
-- SYSDATE se usa en las reglas de las citas ("fecha futura", "cita de hoy"...),
-- asi que se configura la hora de Peru.
--
-- PARTE 1: ejecutar con la conexion ADMIN, y luego REINICIAR la base de datos
--          (consola de Oracle Cloud -> More actions -> Restart).
-- PARTE 2: ejecutar con la conexion VETERINARIA.
-- =====================================================================

-- ------------------------------ PARTE 1 (ADMIN) ------------------------------
ALTER DATABASE SET TIME_ZONE = '-05:00';

-- ------------------------------ PARTE 2 (VETERINARIA) ------------------------
-- Cada sesion del esquema pide que SYSDATE use la zona horaria de la base.
-- El bloque EXCEPTION evita que un error aqui impida iniciar sesion.
CREATE OR REPLACE TRIGGER TRG_LOGON_HORA_LOCAL
AFTER LOGON ON SCHEMA
BEGIN
    EXECUTE IMMEDIATE 'ALTER SESSION SET SYSDATE_AT_DBTIMEZONE = TRUE';
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END;
/

-- Comprobacion (abrir una conexion NUEVA como VETERINARIA): debe mostrar la hora de Peru
-- SELECT TO_CHAR(SYSDATE, 'DD/MM/YYYY HH24:MI') AS hora_bd, DBTIMEZONE AS zona FROM DUAL;
