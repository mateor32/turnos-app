package com.assessment.turnos.controller;

import com.assessment.turnos.model.Turno;
import com.assessment.turnos.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Turnos", description = "Gestión de la fila de turnos: creación, atención, cancelación y métricas")
public class TurnoController {

    private final TurnoService turnoService;

    @Autowired
    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @Operation(summary = "Lista todos los turnos ordenados por hora de solicitud ascendente")
    @GetMapping
    public ResponseEntity<List<Turno>> listarTodos() {
        return ResponseEntity.ok(turnoService.listarTodos());
    }

    @Operation(summary = "Busca un turno por su id. 404 si no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<Turno> buscarPorId(@PathVariable Long id) {
        return turnoService.listarTodos().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Filtra turnos por estado (pendiente, atendido, cancelado)")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Turno>> filtrarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(
                turnoService.listarTodos().stream()
                        .filter(t -> estado.equals(t.getEstado()))
                        .toList()
        );
    }

    @Operation(summary = "Devuelve el próximo turno pendiente (el más antiguo). 404 si no hay.")
    @GetMapping("/siguiente")
    public ResponseEntity<Turno> siguiente() {
        Turno turno = turnoService.siguientePendiente();
        if (turno == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(turno);
    }

    @Operation(summary = "Promedio de espera, en minutos, entre horaSolicitud y horaAtencion de los turnos atendidos")
    @GetMapping("/espera-promedio")
    public ResponseEntity<Double> esperaPromedio() {
        return ResponseEntity.ok(turnoService.calcularEsperaPromedio());
    }

    @Operation(summary = "Cantidad de turnos en estado pendiente")
    @GetMapping("/pendientes/count")
    public ResponseEntity<Long> contarPendientes() {
        return ResponseEntity.ok(turnoService.contarPendientes());
    }

    @Operation(summary = "Crea un turno nuevo. No requiere body: asigna número y horaSolicitud automáticamente.")
    @PostMapping
    public ResponseEntity<Turno> crear() {
        Turno turno = turnoService.crearTurno();
        return ResponseEntity.status(HttpStatus.CREATED).body(turno);
    }

    @Operation(summary = "Marca un turno como atendido y setea horaAtencion. 400 si el turno no está pendiente.")
    @PutMapping("/{id}/atender")
    public ResponseEntity<?> atender(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(turnoService.atenderTurno(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Marca un turno como cancelado. 400 si el turno no está pendiente.")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(turnoService.cancelarTurno(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
