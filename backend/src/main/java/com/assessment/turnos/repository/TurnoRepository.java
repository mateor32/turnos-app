package com.assessment.turnos.repository;

import com.assessment.turnos.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByEstado(String estado);

    List<Turno> findAllByOrderByHoraSolicitudAsc();

    Turno findFirstByEstadoOrderByHoraSolicitudAsc(String estado);
}
