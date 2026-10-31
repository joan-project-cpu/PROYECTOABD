package com.vet.modulos.boleta;

import com.vet.db.Db;
import org.springframework.stereotype.Repository;

/**
 * Boletas en texto plano. El texto lo arma Oracle (funciones FN_BOLETA_* de soluciones/etapa3_logica/11_boletas.sql);
 * aqui solo se consulta. Devuelve null si el ID no existe.
 */
@Repository
public class BoletaRepositorio {

    private static final String MASCOTA = "SELECT FN_BOLETA_MASCOTA(:id) FROM DUAL";
    private static final String MEDICAMENTO = "SELECT FN_BOLETA_MEDICAMENTO(:id) FROM DUAL";
    private static final String VACUNACION = "SELECT FN_BOLETA_VACUNACION(:id) FROM DUAL";

    private final Db db;

    public BoletaRepositorio(Db db) {
        this.db = db;
    }

    public String mascota(long idMascota) {
        return db.valor(MASCOTA, String.class, "id", idMascota);
    }

    public String medicamento(long idMedicamento) {
        return db.valor(MEDICAMENTO, String.class, "id", idMedicamento);
    }

    public String vacunacion(long idVacunacion) {
        return db.valor(VACUNACION, String.class, "id", idVacunacion);
    }
}
