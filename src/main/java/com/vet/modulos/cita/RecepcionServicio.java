package com.vet.modulos.cita;

import com.vet.modulos.mascota.MascotaRepositorio;
import com.vet.modulos.propietario.PropietarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Recepcion: agendar una cita o registrar una emergencia a partir del DNI del propietario.
 *
 * Si el propietario o la mascota todavia no existen, se registran en la MISMA transaccion que la cita:
 * si algo falla (horario ocupado, urgencia invalida, dato faltante...) Oracle deshace todo y no queda
 * ningun propietario ni mascota a medias.
 */
@Service
public class RecepcionServicio {

    private final PropietarioRepositorio propietarios;
    private final MascotaRepositorio mascotas;
    private final CitaRepositorio citas;

    public RecepcionServicio(PropietarioRepositorio propietarios, MascotaRepositorio mascotas, CitaRepositorio citas) {
        this.propietarios = propietarios;
        this.mascotas = mascotas;
        this.citas = citas;
    }

    /** Programa una cita. Devuelve el ID de la cita. */
    @Transactional
    public long agendar(Map<String, String> f, long idVeterinario, LocalDateTime fecha, int duracion, String motivo) {
        long idMascota = resolverMascota(f);
        return citas.programar(idMascota, idVeterinario, fecha, duracion, motivo);
    }

    /** Registra un paciente sin cita / emergencia en la cola. Devuelve el ID de la cita creada. */
    @Transactional
    public long registrarEmergencia(Map<String, String> f, String motivo, CitaRepositorio.Triaje triaje) {
        long idMascota = resolverMascota(f);
        return citas.registrarSinCita(idMascota, motivo, triaje);
    }

    // ------------------------------------------------------------------------------------------

    /** Si el formulario trae una mascota elegida la usa; si no, registra una nueva (y su propietario si hace falta). */
    private long resolverMascota(Map<String, String> f) {
        String elegida = f.get("mascota");
        if (esNumero(elegida)) {
            return Long.parseLong(elegida.trim());
        }
        long idPropietario = resolverPropietario(f);
        exigir(f, "nombre", "el nombre de la mascota");
        exigir(f, "id_raza", "la raza de la mascota");
        exigir(f, "sexo", "el sexo de la mascota");
        return mascotas.insertar(idPropietario, f);
    }

    private long resolverPropietario(Map<String, String> f) {
        String id = f.get("id_propietario");
        if (esNumero(id)) {
            return Long.parseLong(id.trim());
        }
        exigir(f, "dni", "el DNI del propietario");
        String dni = f.get("dni").trim();
        // Se vuelve a verificar en el servidor: otra persona pudo registrarlo mientras se llenaba el formulario.
        for (Map<String, Object> p : propietarios.buscar(dni)) {
            if (dni.equals(String.valueOf(p.get("DNI")))) {
                return ((Number) p.get("ID_PROPIETARIO")).longValue();
            }
        }
        exigir(f, "nombres", "los nombres del propietario");
        exigir(f, "apellidos", "los apellidos del propietario");
        exigir(f, "telefono", "el telefono del propietario");
        return propietarios.insertar(f);
    }

    private static boolean esNumero(String s) {
        return s != null && s.trim().matches("[0-9]+");
    }

    private static void exigir(Map<String, String> f, String campo, String descripcion) {
        String v = f.get(campo);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Falta indicar " + descripcion + ".");
        }
    }
}
