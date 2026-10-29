package com.assessment.turnos.controller;

import com.assessment.turnos.model.Turno;
import com.assessment.turnos.service.TurnoService;
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
public class TurnoController {

    private final TurnoService turnoService;

    @Autowired
    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @GetMapping
    public ResponseEntity<List<Turno>> listarTodos() {
        return ResponseEntity.ok(turnoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Turno> buscarPorId(@PathVariable Long id) {
        return turnoService.listarTodos().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Turno>> filtrarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(
                turnoService.listarTodos().stream()
                        .filter(t -> estado.equals(t.getEstado()))
                        .toList()
        );
    }

    @GetMapping("/siguiente")
    public ResponseEntity<Turno> siguiente() {
        Turno turno = turnoService.siguientePendiente();
        if (turno == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(turno);
    }

    @GetMapping("/espera-promedio")
    public ResponseEntity<Double> esperaPromedio() {
        return ResponseEntity.ok(turnoService.calcularEsperaPromedio());
    }

    @GetMapping("/pendientes/count")
    public ResponseEntity<Long> contarPendientes() {
        return ResponseEntity.ok(turnoService.contarPendientes());
    }

    @PostMapping
    public ResponseEntity<Turno> crear() {
        Turno turno = turnoService.crearTurno();
        return ResponseEntity.status(HttpStatus.CREATED).body(turno);
    }

    @PutMapping("/{id}/atender")
    public ResponseEntity<?> atender(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(turnoService.atenderTurno(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(turnoService.cancelarTurno(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
