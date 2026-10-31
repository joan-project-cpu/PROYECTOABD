package com.vet.modulos.propietario;

import com.vet.modulos.mascota.MascotaRepositorio;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/propietarios")
public class PropietarioController {

    private final PropietarioRepositorio repo;
    private final MascotaRepositorio mascotas;

    public PropietarioController(PropietarioRepositorio repo, MascotaRepositorio mascotas) {
        this.repo = repo;
        this.mascotas = mascotas;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("propietarios", repo.buscar(q));
        return "propietarios/lista";
    }

    /**
     * Verifica por DNI si un propietario ya esta registrado (lo usan Nueva cita y Emergencia).
     * Devuelve JSON: { valido, encontrado, propietario{...}, mascotas[...] } con sus mascotas ACTIVAS.
     */
    @GetMapping("/buscar-dni")
    @ResponseBody
    public Map<String, Object> buscarDni(@RequestParam String dni) {
        String d = dni == null ? "" : dni.trim();
        Map<String, Object> r = new LinkedHashMap<>();
        if (!d.matches("[0-9]{8,12}")) {
            r.put("valido", false);
            r.put("encontrado", false);
            return r;
        }
        r.put("valido", true);
        r.put("encontrado", false);
        for (Map<String, Object> p : repo.buscar(d)) {
            if (!d.equals(String.valueOf(p.get("DNI")))) {
                continue;
            }
            long id = ((Number) p.get("ID_PROPIETARIO")).longValue();
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("id", id);
            prop.put("dni", p.get("DNI"));
            prop.put("nombres", p.get("NOMBRES"));
            prop.put("apellidos", p.get("APELLIDOS"));
            prop.put("telefono", p.get("TELEFONO"));
            List<Map<String, Object>> activas = new ArrayList<>();
            for (Map<String, Object> m : mascotas.dePropietario(id)) {
                if ("ACTIVO".equals(m.get("ESTADO"))) {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", ((Number) m.get("ID_MASCOTA")).longValue());
                    x.put("nombre", m.get("NOMBRE"));
                    x.put("especie", m.get("ESPECIE"));
                    x.put("raza", m.get("RAZA"));
                    activas.add(x);
                }
            }
            r.put("encontrado", true);
            r.put("propietario", prop);
            r.put("mascotas", activas);
            break;
        }
        return r;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("p", Map.of());
        return "propietarios/form";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("p", repo.obtener(id));
        model.addAttribute("mascotas", mascotas.dePropietario(id));
        return "propietarios/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        model.addAttribute("p", repo.obtener(id));
        return "propietarios/form";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Map<String, String> f, RedirectAttributes ra) {
        String id = f.get("id");
        long[] nuevo = new long[1];
        boolean ok = Mensajes.intentar(ra, "Propietario guardado", () -> {
            if (id == null || id.isBlank()) {
                nuevo[0] = repo.insertar(f);
            } else {
                repo.actualizar(Long.parseLong(id), f);
            }
        });
        if (!ok) {
            ra.addFlashAttribute("form", f);
            return id == null || id.isBlank() ? "redirect:/propietarios/nuevo" : "redirect:/propietarios/" + id + "/editar";
        }
        return "redirect:/propietarios/" + (id == null || id.isBlank() ? nuevo[0] : id);
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        if (Mensajes.intentar(ra, "Propietario eliminado", () -> repo.eliminar(id))) {
            return "redirect:/propietarios";
        }
        return "redirect:/propietarios/" + id;
    }
}
