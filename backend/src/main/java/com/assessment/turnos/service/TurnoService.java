package com.assessment.turnos.service;

import com.assessment.turnos.model.Turno;
import com.assessment.turnos.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepository;

    @Autowired
    public TurnoService(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    public List<Turno> listarTodos() {
        return turnoRepository.findAllByOrderByHoraSolicitudAsc();
    }

    public Turno crearTurno() {
        int siguienteNumero = turnoRepository.findAll().stream()
                .mapToInt(Turno::getNumero)
                .max()
                .orElse(0) + 1;

        Turno turno = new Turno();
        turno.setNumero(siguienteNumero);
        turno.setHoraSolicitud(LocalTime.now());
        turno.setEstado("pendiente");

        return turnoRepository.save(turno);
    }

    public Turno atenderTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!"pendiente".equals(turno.getEstado())) {
            throw new RuntimeException("El turno no está pendiente");
        }

        turno.setEstado("atendido");
        turno.setHoraAtencion(LocalTime.now());

        return turnoRepository.save(turno);
    }

    public Turno cancelarTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!"pendiente".equals(turno.getEstado())) {
            throw new RuntimeException("El turno no está pendiente");
        }

        turno.setEstado("cancelado");

        return turnoRepository.save(turno);
    }

    public Turno siguientePendiente() {
        return turnoRepository.findFirstByEstadoOrderByHoraSolicitudAsc("pendiente");
    }

    public double calcularEsperaPromedio() {
        List<Turno> atendidos = turnoRepository.findByEstado("atendido");

        return atendidos.stream()
                .mapToDouble(t -> Duration.between(t.getHoraSolicitud(), t.getHoraAtencion()).toMinutes())
                .average()
                .orElse(0.0);
    }

    public long contarPendientes() {
        return turnoRepository.findByEstado("pendiente").size();
    }
}
