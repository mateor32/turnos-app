package com.assessment.turnos.service;

import com.assessment.turnos.model.Turno;
import com.assessment.turnos.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoService - pruebas unitarias")
class TurnoServiceTest {

    @Mock
    private TurnoRepository turnoRepository;

    @InjectMocks
    private TurnoService turnoService;

    private Turno turnoPendiente;
    private Turno turnoAtendido;

    @BeforeEach
    void setUp() {
        turnoPendiente = new Turno();
        turnoPendiente.setId(1L);
        turnoPendiente.setNumero(1);
        turnoPendiente.setHoraSolicitud(LocalTime.of(8, 0));
        turnoPendiente.setEstado("pendiente");

        turnoAtendido = new Turno();
        turnoAtendido.setId(2L);
        turnoAtendido.setNumero(2);
        turnoAtendido.setHoraSolicitud(LocalTime.of(8, 5));
        turnoAtendido.setHoraAtencion(LocalTime.of(8, 15));
        turnoAtendido.setEstado("atendido");
    }

    @Test
    @DisplayName("listarTodos devuelve lo que entrega el repositorio ordenado")
    void listarTodos_devuelveListaOrdenada() {
        when(turnoRepository.findAllByOrderByHoraSolicitudAsc())
                .thenReturn(List.of(turnoPendiente, turnoAtendido));

        List<Turno> resultado = turnoService.listarTodos();

        assertThat(resultado).containsExactly(turnoPendiente, turnoAtendido);
    }

    @Test
    @DisplayName("crearTurno con tabla vacía asigna numero=1 y estado=pendiente")
    void crearTurno_tablaVacia_asignaNumeroUno() {
        when(turnoRepository.findAll()).thenReturn(Collections.emptyList());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno creado = turnoService.crearTurno();

        assertThat(creado.getNumero()).isEqualTo(1);
        assertThat(creado.getEstado()).isEqualTo("pendiente");
        assertThat(creado.getHoraSolicitud()).isNotNull();
        assertThat(creado.getHoraAtencion()).isNull();
    }

    @Test
    @DisplayName("crearTurno con turnos existentes asigna max(numero) + 1")
    void crearTurno_conExistentes_asignaMaximoMasUno() {
        Turno t1 = new Turno(); t1.setNumero(7);
        Turno t2 = new Turno(); t2.setNumero(3);
        Turno t3 = new Turno(); t3.setNumero(5);

        when(turnoRepository.findAll()).thenReturn(List.of(t1, t2, t3));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno creado = turnoService.crearTurno();

        assertThat(creado.getNumero()).isEqualTo(8);
    }

    @Test
    @DisplayName("atenderTurno marca estado=atendido y setea horaAtencion")
    void atenderTurno_pendiente_actualizaEstadoYHora() {
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPendiente));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(inv -> inv.getArgument(0));

        Turno atendido = turnoService.atenderTurno(1L);

        assertThat(atendido.getEstado()).isEqualTo("atendido");
        assertThat(atendido.getHoraAtencion()).isNotNull();
    }

    @Test
    @DisplayName("atenderTurno lanza RuntimeException si el id no existe")
    void atenderTurno_idInexistente_lanzaExcepcion() {
        when(turnoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.atenderTurno(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");

        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("atenderTurno lanza RuntimeException si el turno no está pendiente")
    void atenderTurno_noPendiente_lanzaExcepcion() {
        when(turnoRepository.findById(2L)).thenReturn(Optional.of(turnoAtendido));

        assertThatThrownBy(() -> turnoService.atenderTurno(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está pendiente");

        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarTurno marca estado=cancelado y no setea horaAtencion")
    void cancelarTurno_pendiente_marcaCancelado() {
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPendiente));
        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        when(turnoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Turno cancelado = turnoService.cancelarTurno(1L);

        assertThat(cancelado.getEstado()).isEqualTo("cancelado");
        assertThat(captor.getValue().getHoraAtencion()).isNull();
    }

    @Test
    @DisplayName("cancelarTurno lanza RuntimeException si el id no existe")
    void cancelarTurno_idInexistente_lanzaExcepcion() {
        when(turnoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnoService.cancelarTurno(99L))
                .isInstanceOf(RuntimeException.class);

        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarTurno lanza RuntimeException si el turno no está pendiente")
    void cancelarTurno_noPendiente_lanzaExcepcion() {
        when(turnoRepository.findById(2L)).thenReturn(Optional.of(turnoAtendido));

        assertThatThrownBy(() -> turnoService.cancelarTurno(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está pendiente");

        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("siguientePendiente delega en el repositorio")
    void siguientePendiente_delegaEnRepo() {
        when(turnoRepository.findFirstByEstadoOrderByHoraSolicitudAsc("pendiente"))
                .thenReturn(turnoPendiente);

        Turno siguiente = turnoService.siguientePendiente();

        assertThat(siguiente).isSameAs(turnoPendiente);
    }

    @Test
    @DisplayName("siguientePendiente devuelve null si no hay pendientes")
    void siguientePendiente_sinPendientes_devuelveNull() {
        when(turnoRepository.findFirstByEstadoOrderByHoraSolicitudAsc("pendiente"))
                .thenReturn(null);

        assertThat(turnoService.siguientePendiente()).isNull();
    }

    @Test
    @DisplayName("calcularEsperaPromedio devuelve 0.0 si no hay atendidos")
    void calcularEsperaPromedio_sinAtendidos_devuelveCero() {
        when(turnoRepository.findByEstado("atendido")).thenReturn(Collections.emptyList());

        assertThat(turnoService.calcularEsperaPromedio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calcularEsperaPromedio promedia los minutos de espera")
    void calcularEsperaPromedio_variosAtendidos_promedia() {
        Turno a = new Turno();
        a.setHoraSolicitud(LocalTime.of(8, 0));
        a.setHoraAtencion(LocalTime.of(8, 10)); // 10 min

        Turno b = new Turno();
        b.setHoraSolicitud(LocalTime.of(9, 0));
        b.setHoraAtencion(LocalTime.of(9, 20)); // 20 min

        when(turnoRepository.findByEstado("atendido")).thenReturn(Arrays.asList(a, b));

        assertThat(turnoService.calcularEsperaPromedio()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("contarPendientes devuelve la cantidad de pendientes")
    void contarPendientes_devuelveCantidad() {
        when(turnoRepository.findByEstado("pendiente"))
                .thenReturn(List.of(turnoPendiente, new Turno(), new Turno()));

        assertThat(turnoService.contarPendientes()).isEqualTo(3L);
    }
}
