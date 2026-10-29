package com.assessment.turnos.controller;

import com.assessment.turnos.model.Turno;
import com.assessment.turnos.service.TurnoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TurnoController.class)
@DisplayName("TurnoController - pruebas de integración web (MockMvc)")
class TurnoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
    @DisplayName("GET /api/turnos devuelve 200 con la lista")
    void listarTodos_devuelve200ConLista() throws Exception {
        when(turnoService.listarTodos()).thenReturn(List.of(turnoPendiente, turnoAtendido));

        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("pendiente"))
                .andExpect(jsonPath("$[1].estado").value("atendido"));
    }

    @Test
    @DisplayName("GET /api/turnos/{id} con id existente devuelve 200")
    void buscarPorId_existente_devuelve200() throws Exception {
        when(turnoService.listarTodos()).thenReturn(List.of(turnoPendiente, turnoAtendido));

        mockMvc.perform(get("/api/turnos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numero").value(1));
    }

    @Test
    @DisplayName("GET /api/turnos/{id} con id inexistente devuelve 404")
    void buscarPorId_inexistente_devuelve404() throws Exception {
        when(turnoService.listarTodos()).thenReturn(List.of(turnoPendiente));

        mockMvc.perform(get("/api/turnos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/turnos/estado/{estado} filtra correctamente")
    void filtrarPorEstado_devuelveSoloEsosEstados() throws Exception {
        when(turnoService.listarTodos()).thenReturn(List.of(turnoPendiente, turnoAtendido));

        mockMvc.perform(get("/api/turnos/estado/pendiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("pendiente"));
    }

    @Test
    @DisplayName("GET /api/turnos/siguiente con pendientes devuelve 200")
    void siguiente_conPendientes_devuelve200() throws Exception {
        when(turnoService.siguientePendiente()).thenReturn(turnoPendiente);

        mockMvc.perform(get("/api/turnos/siguiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("pendiente"));
    }

    @Test
    @DisplayName("GET /api/turnos/siguiente sin pendientes devuelve 404")
    void siguiente_sinPendientes_devuelve404() throws Exception {
        when(turnoService.siguientePendiente()).thenReturn(null);

        mockMvc.perform(get("/api/turnos/siguiente"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/turnos/espera-promedio devuelve 200 con el valor")
    void esperaPromedio_devuelve200ConValor() throws Exception {
        when(turnoService.calcularEsperaPromedio()).thenReturn(12.5);

        mockMvc.perform(get("/api/turnos/espera-promedio"))
                .andExpect(status().isOk())
                .andExpect(content().string("12.5"));
    }

    @Test
    @DisplayName("GET /api/turnos/pendientes/count devuelve 200 con el conteo")
    void contarPendientes_devuelve200ConConteo() throws Exception {
        when(turnoService.contarPendientes()).thenReturn(7L);

        mockMvc.perform(get("/api/turnos/pendientes/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @DisplayName("POST /api/turnos devuelve 201 con el turno creado")
    void crearTurno_devuelve201() throws Exception {
        when(turnoService.crearTurno()).thenReturn(turnoPendiente);

        mockMvc.perform(post("/api/turnos"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("pendiente"));
    }

    @Test
    @DisplayName("PUT /api/turnos/{id}/atender exitoso devuelve 200")
    void atender_exitoso_devuelve200() throws Exception {
        turnoPendiente.setEstado("atendido");
        turnoPendiente.setHoraAtencion(LocalTime.of(8, 10));
        when(turnoService.atenderTurno(1L)).thenReturn(turnoPendiente);

        mockMvc.perform(put("/api/turnos/1/atender"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("atendido"))
                .andExpect(jsonPath("$.horaAtencion").exists());
    }

    @Test
    @DisplayName("PUT /api/turnos/{id}/atender inválido devuelve 400 con mensaje")
    void atender_invalido_devuelve400() throws Exception {
        when(turnoService.atenderTurno(anyLong()))
                .thenThrow(new RuntimeException("El turno no está pendiente"));

        mockMvc.perform(put("/api/turnos/1/atender"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no está pendiente")));
    }

    @Test
    @DisplayName("PUT /api/turnos/{id}/cancelar exitoso devuelve 200")
    void cancelar_exitoso_devuelve200() throws Exception {
        turnoPendiente.setEstado("cancelado");
        when(turnoService.cancelarTurno(1L)).thenReturn(turnoPendiente);

        mockMvc.perform(put("/api/turnos/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cancelado"));
    }

    @Test
    @DisplayName("PUT /api/turnos/{id}/cancelar inválido devuelve 400 con mensaje")
    void cancelar_invalido_devuelve400() throws Exception {
        when(turnoService.cancelarTurno(anyLong()))
                .thenThrow(new RuntimeException("Turno no encontrado"));

        mockMvc.perform(put("/api/turnos/999/cancelar"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Turno no encontrado")));
    }
}
