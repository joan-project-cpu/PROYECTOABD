# Despliegue en la nube (Oracle Cloud + Render)

| Pieza | Servicio | Nota |
|---|---|---|
| Base de datos | Oracle Cloud - Autonomous Database (Always Free) | scripts: `soluciones/etapa2_creacion` y `etapa3_logica` (02 a 09, 11 y 12) |
| Aplicacion | Render - Web Service con Docker | usa el `Dockerfile` de la raiz |

## Variables de entorno en Render

| Variable | Valor |
|---|---|
| `DB_URL` | cadena JDBC TLS de la base (ver abajo) |
| `DB_USUARIO` | `VETERINARIA` |
| `DB_PASSWORD` | la clave del usuario VETERINARIA (**no subirla a GitHub**) |

Las demas (`PORT`, `TZ`, modo solucion, etc.) ya vienen en el `Dockerfile` o las pone Render.

## Cadena JDBC (conexion TLS, sin wallet)

En la consola de Oracle Cloud: la base -> **Database connection** -> *Connection strings* -> **TLS** -> copiar el servicio `*_tp`.
Se arma asi:

```
jdbc:oracle:thin:@(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=HOST))(connect_data=(service_name=SERVICIO))(security=(ssl_server_dn_match=yes)))
```

Para que funcione sin wallet, en la base: **Network -> Mutual TLS (mTLS) authentication -> Edit** y desmarcar "Require mutual TLS".

## Hora
La base en la nube usa UTC. Se corrige con `soluciones/etapa3_logica/12_nube_hora_peru.sql` y `TZ=America/Lima` en la app.
