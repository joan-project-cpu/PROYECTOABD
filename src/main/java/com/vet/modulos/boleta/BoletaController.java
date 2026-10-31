package com.vet.modulos.boleta;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Boletas imprimibles: ficha de mascota, ficha de insumo y comprobante de vacunacion. */
@Controller
@RequestMapping("/boletas")
public class BoletaController {

    private final BoletaRepositorio repo;

    public BoletaController(BoletaRepositorio repo) {
        this.repo = repo;
    }

    @GetMapping("/mascota/{id}")
    public String mascota(@PathVariable long id, Model model) {
        return mostrar("Ficha de la mascota", repo.mascota(id), "/mascotas/" + id, model);
    }

    /** Solo ADMIN y VETERINARIO (ver SesionInterceptor). */
    @GetMapping("/medicamento/{id}")
    public String medicamento(@PathVariable long id, Model model) {
        return mostrar("Ficha del insumo", repo.medicamento(id), "/inventario", model);
    }

    @GetMapping("/vacunacion/{id}")
    public String vacunacion(@PathVariable long id, @RequestParam(required = false) Long mascota, Model model) {
        String volver = mascota == null ? "/mascotas" : "/mascotas/" + mascota;
        return mostrar("Comprobante de vacunacion", repo.vacunacion(id), volver, model);
    }

    private String mostrar(String titulo, String texto, String volver, Model model) {
        model.addAttribute("tituloBoleta", titulo);
        model.addAttribute("textoBoleta", texto);   // null = el registro no existe
        model.addAttribute("volver", volver);
        return "boleta";
    }
}
