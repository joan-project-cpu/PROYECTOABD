package com.vet;

import com.vet.db.Sql;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Sistema Veterinario - servidor web.
 * Ejecutar y abrir http://localhost:8080  (los compañeros: http://IP-DE-ESTA-PC:8080)
 */
@SpringBootApplication
public class VeterinariaApplication {

    public static void main(String[] args) throws Exception {
        completarConSolucionSiSePide();
        SpringApplication.run(VeterinariaApplication.class, args);
    }

    /**
     * Modo solucion: las sentencias pendientes se toman de soluciones/etapa3_logica/consultas_app.sql.
     * Se activa con app.completar_con_solucion=true en config/db.properties
     * o con la variable de entorno APP_COMPLETAR_CON_SOLUCION=true (despliegue en la nube).
     */
    private static void completarConSolucionSiSePide() throws Exception {
        boolean pedido = Boolean.parseBoolean(String.valueOf(System.getenv("APP_COMPLETAR_CON_SOLUCION")).trim());

        Path config = Path.of("config/db.properties");
        if (Files.exists(config)) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(config.toFile())) {
                p.load(in);
            }
            pedido = pedido || Boolean.parseBoolean(p.getProperty("app.completar_con_solucion", "false").trim());
        }
        if (!pedido) {
            return;
        }
        Path soluciones = Path.of("soluciones/etapa3_logica/consultas_app.sql");
        if (!Files.exists(soluciones)) {
            System.out.println(">>> AVISO: se pidio el modo solucion pero no existe " + soluciones
                    + ". Se arranca en modo plantilla: las consultas no escritas mostraran 'Falta implementar'.");
            return;
        }
        Sql.cargarSoluciones(soluciones);
        System.out.println(">>> MODO SOLUCION: " + Sql.cantidadSoluciones()
                + " sentencias disponibles para completar las que falten (" + soluciones + ")");
    }
}
