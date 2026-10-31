# Despliegue en la nube (Oracle Cloud + Render)

| Pieza | Servicio | Nota |
|---|---|---|
| Base de datos | Oracle Cloud - Autonomous Database (Always Free) | scripts 02 a 09, 11 y 12 de `soluciones/` |
| Aplicacion | Render - Web Service con Docker | usa el `Dockerfile` de la raiz |

## Variables de entorno en Render

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:oracle:thin:@` + cadena **mTLS** del servicio `*_tp` (en una sola linea) |
| `DB_USUARIO` | `VETERINARIA` |
| `DB_PASSWORD` | clave del usuario VETERINARIA |
| `WALLET_PASSWORD` | clave que se puso al descargar el wallet |
| `WALLET_TRUSTSTORE_B64` | `truststore.jks` del wallet, en Base64 |
| `WALLET_KEYSTORE_B64` | `keystore.jks` del wallet, en Base64 |

`entrypoint.sh` reconstruye los dos `.jks` al arrancar y los entrega a la JVM (`javax.net.ssl.*`).
Las claves nunca se suben a GitHub: solo viven en las variables de Render.

## Base64 en Windows (PowerShell)
    [Convert]::ToBase64String([IO.File]::ReadAllBytes("truststore.jks")) | Set-Clipboard

## Hora
La base en la nube usa UTC. Se corrige con `soluciones/etapa3_logica/12_nube_hora_peru.sql` y `TZ=America/Lima` en la app.
