package com.vet.modulos.cita;

import com.vet.modulos.mascota.MascotaRepositorio;
import com.vet.modulos.veterinario.VeterinarioRepositorio;
import com.vet.web.Mensajes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/citas")
public class CitaController {

    /** Una cita esta "pendiente" mientras no termine: aun no llego, esta esperando o esta siendo atendida. */
    private static final Set<String> PENDIENTES = Set.of("PROGRAMADA", "EN_ESPERA", "EN_ATENCION");

    private final CitaRepositorio repo;
    private final VeterinarioRepositorio veterinarios;
    private final MascotaRepositorio mascotas;
    private final RecepcionServicio recepcion;

    public CitaController(CitaRepositorio repo, VeterinarioRepositorio veterinarios,
                          MascotaRepositorio mascotas, RecepcionServicio recepcion) {
        this.repo = repo;
        this.veterinarios = veterinarios;
        this.mascotas = mascotas;
        this.recepcion = recepcion;
    }

    // ------------------------------------------------------------------ agenda

    @GetMapping
    public String agenda(@RequestParam(required = false) LocalDate fecha,
                         @RequestParam(required = false) Long veterinario,
                         @RequestParam(defaultValue = "pendientes") String ver, Model model) {
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        List<Map<String, Object>> todas = repo.agenda(dia, veterinario);
        List<Map<String, Object>> pendientes = todas.stream()
                .filter(c -> PENDIENTES.contains(String.valueOf(c.get("ESTADO"))))
                .collect(Collectors.toList());
        boolean soloPendientes = !"todas".equals(ver);

        model.addAttribute("fecha", dia);
        model.addAttribute("veterinario", veterinario);
        model.addAttribute("veterinarios", veterinarios.activos());
        model.addAttribute("ver", soloPendientes ? "pendientes" : "todas");
        model.addAttribute("citas", soloPendientes ? pendientes : todas);
        model.addAttribute("totalPendientes", pendientes.size());
        model.addAttribute("totalTodas", todas.size());
        model.addAttribute("esHoy", dia.equals(LocalDate.now()));
        return "citas/agenda";
    }

    // ------------------------------------------------------------------ nueva cita

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) Long mascota, @RequestParam(required = false) Long veterinario,
                        Model model) {
        model.addAttribute("veterinarios", veterinarios.activos());
        model.addAttribute("razas", mascotas.razas());
        model.addAttribute("veterinario", veterinario);
        if (mascota != null) {
            // Viene desde la ficha de una mascota: se pre-llena el DNI de su propietario y se elige la mascota.
            Map<String, Object> m = mascotas.obtener(mascota);
            if (m != null) {
                model.addAttribute("prefillDni", m.get("DNI"));
                model.addAttribute("prefillMascota", mascota);
            }
        }
        model.addAttribute("sugerida", LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
        return "citas/nueva";
    }

    /** Horario de un veterinario (JSON) para mostrarlo al elegirlo, sin recargar la pagina. */
    @GetMapping("/nueva/horario")
    @ResponseBody
    public List<Map<String, Object>> horario(@RequestParam long veterinario) {
        return veterinarios.horarios(veterinario);
    }

    @PostMapping("/nueva")
    public String programar(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        LocalDateTime[] cuando = new LocalDateTime[1];
        long[] vet = new long[1];
        boolean ok = Mensajes.intentar(ra, null, () -> {
            if (f.get("veterinario") == null || f.get("veterinario").isBlank()) {
                throw new IllegalArgumentException("Elija un veterinario.");
            }
            if (f.get("fecha") == null || f.get("fecha").isBlank()) {
                throw new IllegalArgumentException("Indique la fecha y la hora de la cita.");
            }
            if (f.get("motivo") == null || f.get("motivo").isBlank()) {
                throw new IllegalArgumentException("Indique el motivo de la cita.");
            }
            vet[0] = Long.parseLong(f.get("veterinario").trim());
            cuando[0] = LocalDateTime.parse(f.get("fecha").trim());
            int duracion = Integer.parseInt(f.getOrDefault("duracion", "30").trim());
            long id = recepcion.agendar(f, vet[0], cuando[0], duracion, f.get("motivo").trim());
            Mensajes.ok(ra, "Cita #" + id + " programada para el " + cuando[0].toLocalDate()
                    + " a las " + cuando[0].toLocalTime());
        });
        if (!ok) {
            ra.addFlashAttribute("form", f);
            return "redirect:/citas/nueva";
        }
        return "redirect:/citas?fecha=" + cuando[0].toLocalDate() + "&veterinario=" + vet[0] + "&ver=pendientes";
    }

    // ------------------------------------------------------------------ llegada / emergencia

    /** Llegada de un paciente con cita: formulario de triaje. */
    @GetMapping("/llegada/{id}")
    public String llegada(@PathVariable long id, Model model) {
        model.addAttribute("cita", repo.obtener(id));
        return "citas/llegada";
    }

    /** Paciente sin cita / emergencia: se pide el DNI del propietario y el sistema verifica si esta registrado. */
    @GetMapping("/llegada")
    public String sinCita(Model model) {
        model.addAttribute("razas", mascotas.razas());
        return "citas/llegada";
    }

    @PostMapping("/llegada")
    public String registrarLlegada(@RequestParam(required = false) Long cita,
                                   @RequestParam(required = false) String motivo,
                                   @RequestParam BigDecimal peso, @RequestParam BigDecimal temperatura,
                                   @RequestParam(required = false) Integer fc, @RequestParam(required = false) Integer fr,
                                   @RequestParam int urgencia, @RequestParam(required = false) String obs,
                                   @RequestParam Map<String, String> f, RedirectAttributes ra) {
        CitaRepositorio.Triaje t = new CitaRepositorio.Triaje(peso, temperatura, fc, fr, urgencia, obs);
        boolean ok = Mensajes.intentar(ra, "Paciente registrado en la cola de atencion", () -> {
            if (cita != null) {
                repo.registrarLlegada(cita, t);
            } else {
                if (motivo == null || motivo.isBlank()) {
                    throw new IllegalArgumentException("Indique el motivo de la emergencia.");
                }
                recepcion.registrarEmergencia(f, motivo.trim(), t);
            }
        });
        if (!ok) {
            if (cita != null) {
                return "redirect:/citas/llegada/" + cita;
            }
            ra.addFlashAttribute("form", f);
            return "redirect:/citas/llegada";
        }
        return "redirect:/citas";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable long id, @RequestParam(required = false) String volver, RedirectAttributes ra) {
        Mensajes.intentar(ra, "Cita cancelada", () -> repo.cancelar(id));
        return "redirect:" + (volver == null || volver.isBlank() ? "/citas" : volver);
    }
}
