/*
 * Bloque "Propietario y mascota" de Nueva cita y de Emergencia.
 * El usuario escribe el DNI; el sistema consulta /propietarios/buscar-dni y muestra solo lo necesario:
 *   - propietario registrado     -> elegir su mascota (o registrar otra)
 *   - propietario NO registrado  -> pedir sus datos y los de la mascota
 * Los campos de las secciones ocultas se deshabilitan para que no se envien ni se validen.
 */
(function () {
    'use strict';

    const dni = document.getElementById('pDni');
    if (!dni) { return; }

    const $ = id => document.getElementById(id);
    const formulario = dni.form;
    const estado = $('pEstado');
    const idPropietario = $('pId');
    const boton = $('pBuscar');
    const titulo = $('pMascotaTitulo');
    const propietarioNuevo = $('pNuevo');
    const mascotaExistente = $('pMascotaExistente');
    const mascotaNueva = $('pMascotaNueva');
    const avisoSinMascotas = $('pSinMascotas');
    const selMascota = $('pMascota');

    let verificado = null;      // DNI que ya se consulto
    let temporizador = null;
    let turno = 0;              // para ignorar respuestas que llegan tarde

    function activar(seccion, encendida) {
        seccion.classList.toggle('d-none', !encendida);
        seccion.querySelectorAll('input, select, textarea').forEach(c => { c.disabled = !encendida; });
    }

    function mensaje(clase, icono, ...partes) {
        estado.className = 'small ' + clase;
        estado.replaceChildren();
        const i = document.createElement('i');
        i.className = 'bi ' + icono + ' me-1';
        estado.append(i, ...partes);
    }

    function limpiar() {
        verificado = null;
        idPropietario.value = '';
        titulo.classList.add('d-none');
        avisoSinMascotas.classList.add('d-none');
        activar(propietarioNuevo, false);
        activar(mascotaExistente, false);
        activar(mascotaNueva, false);
        estado.replaceChildren();
    }

    function alternarMascotaNueva() {
        activar(mascotaNueva, selMascota.value === 'nueva');
    }

    function mostrarEncontrado(d) {
        const p = d.propietario;
        idPropietario.value = p.id;
        const nombre = document.createElement('b');
        nombre.textContent = p.nombres + ' ' + p.apellidos;
        mensaje('text-success', 'bi-check-circle-fill', 'Propietario registrado: ', nombre,
            p.telefono ? ' · Tel. ' + p.telefono : '');

        titulo.classList.remove('d-none');
        activar(propietarioNuevo, false);

        if (d.mascotas.length > 0) {
            const preseleccion = selMascota.dataset.preseleccion;
            selMascota.replaceChildren(new Option('Seleccione la mascota...', ''));
            d.mascotas.forEach(m => {
                selMascota.add(new Option(m.nombre + ' (' + m.especie + ' · ' + m.raza + ')', m.id));
            });
            selMascota.add(new Option('+ Registrar otra mascota', 'nueva'));
            activar(mascotaExistente, true);
            avisoSinMascotas.classList.add('d-none');
            if (preseleccion && Array.from(selMascota.options).some(o => o.value === preseleccion)) {
                selMascota.value = preseleccion;
            }
            alternarMascotaNueva();
        } else {
            activar(mascotaExistente, false);
            avisoSinMascotas.classList.remove('d-none');
            activar(mascotaNueva, true);
        }
    }

    function mostrarNoEncontrado() {
        idPropietario.value = '';
        mensaje('text-warning-emphasis', 'bi-person-plus-fill',
            'No esta registrado. Complete sus datos y los de su mascota para registrarlos.');
        titulo.classList.remove('d-none');
        avisoSinMascotas.classList.add('d-none');
        activar(propietarioNuevo, true);
        activar(mascotaExistente, false);
        activar(mascotaNueva, true);
    }

    async function buscar() {
        const valor = dni.value.trim();
        if (!/^[0-9]{8,12}$/.test(valor)) {
            limpiar();
            if (valor) {
                mensaje('text-danger', 'bi-exclamation-circle', 'El DNI debe tener entre 8 y 12 digitos.');
            }
            return;
        }
        const miTurno = ++turno;
        mensaje('text-muted', 'bi-hourglass-split', 'Buscando...');
        try {
            const r = await fetch(dni.dataset.url + '?dni=' + encodeURIComponent(valor),
                {headers: {'Accept': 'application/json'}});
            if (!r.ok) { throw new Error('HTTP ' + r.status); }
            const d = await r.json();
            if (miTurno !== turno || dni.value.trim() !== valor) { return; }   // llego tarde
            verificado = valor;
            if (d.encontrado) { mostrarEncontrado(d); } else { mostrarNoEncontrado(); }
        } catch (e) {
            if (miTurno !== turno) { return; }
            limpiar();
            mensaje('text-danger', 'bi-wifi-off', 'No se pudo verificar el DNI. Pulse "Buscar" para intentar de nuevo.');
        }
    }

    dni.addEventListener('input', () => {
        dni.value = dni.value.replace(/\D/g, '');
        clearTimeout(temporizador);
        if (dni.value !== verificado) { limpiar(); }
        if (/^[0-9]{8,12}$/.test(dni.value)) { temporizador = setTimeout(buscar, 500); }
    });
    dni.addEventListener('keydown', e => {
        if (e.key === 'Enter') { e.preventDefault(); clearTimeout(temporizador); buscar(); }
    });
    boton.addEventListener('click', () => { clearTimeout(temporizador); buscar(); });
    selMascota.addEventListener('change', alternarMascotaNueva);

    // Si intenta enviar sin haber verificado el DNI, se verifica primero.
    formulario.addEventListener('submit', e => {
        if (verificado === null || dni.value.trim() !== verificado) {
            e.preventDefault();
            clearTimeout(temporizador);
            buscar();
        }
    });

    // Al volver a cargar con datos (error al guardar, o desde la ficha de una mascota) se verifica solo.
    if (dni.value) { buscar(); }
})();
