package com.turnos.qa.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas directas sobre los endpoints del Backend (http://localhost:8080/api/turnos).
 * Permiten validar toda la lógica del servicio sin depender de que el Frontend esté listo.
 *
 * Regla del Caso:
 * "Los turnos se atienden en orden de llegada, los números no se repiten
 * y no se puede llamar al siguiente si la fila está vacía."
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Pruebas API Backend - Servicio de Turnos (Caminos Felices y Caminos Tristes)")
public class BackendTurnosApiTest {

    private static final String API_URL = System.getProperty("api.url", "http://localhost:8080/api/turnos");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final Pattern NUMERO_PATTERN = Pattern.compile("\"numero\"\\s*:\\s*(\\d+)");

    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // =========================================================================
    // CAMINOS FELICES (HAPPY PATHS - CASOS EXITOSOS)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("[Camino Feliz] 1. Los turnos pendientes se atienden en estricto orden de llegada (FIFO)")
    void test01_atencionEnOrdenDeLlegadaFIFO() throws Exception {
        // Asegurar que haya al menos 3 turnos pendientes en la fila para probar el orden
        while (contarPendientes() < 3) {
            crearTurno();
        }

        // Consultar la lista de turnos pendientes ordenados por hora de solicitud
        HttpResponse<String> resPendientes = get("/estado/pendiente");
        assertEquals(200, resPendientes.statusCode());
        List<Integer> ordenEsperado = extraerTodosLosNumeros(resPendientes.body());

        // Llamar al siguiente y atenderlo uno por uno verificando el orden FIFO
        List<Integer> ordenAtendido = new ArrayList<>();
        for (int i = 0; i < ordenEsperado.size(); i++) {
            HttpResponse<String> resSiguiente = get("/siguiente");
            assertEquals(200, resSiguiente.statusCode(), "Debe devolver el siguiente turno pendiente");

            int idSiguiente = extraerId(resSiguiente.body());
            int numeroSiguiente = extraerNumero(resSiguiente.body());
            ordenAtendido.add(numeroSiguiente);

            HttpResponse<String> resAtender = put("/" + idSiguiente + "/atender");
            assertEquals(200, resAtender.statusCode(), "El turno pendiente debe marcarse como atendido (200 OK)");
        }

        assertEquals(ordenEsperado, ordenAtendido,
                "El servicio debe entregar y atender los turnos exactamente en su orden de llegada (FIFO)");
    }

    @Test
    @Order(2)
    @DisplayName("[Camino Feliz] 2. Crear un nuevo turno asigna un número consecutivo y aumenta los pendientes")
    void test02_crearTurno_asignaNumeroConsecutivo_yAumentaFila() throws Exception {
        int pendientesAntes = contarPendientes();

        HttpResponse<String> resCrear = post("");
        assertEquals(201, resCrear.statusCode(), "POST /api/turnos debe responder 201 Created");

        int numeroAsignado = extraerNumero(resCrear.body());
        assertTrue(numeroAsignado >= 1, "El número de turno asignado debe ser positivo y válido");
        assertTrue(resCrear.body().contains("\"estado\":\"pendiente\""),
                "Todo turno nuevo debe nacer en estado 'pendiente'");

        int pendientesDespues = contarPendientes();
        assertEquals(pendientesAntes + 1, pendientesDespues,
                "El contador de pendientes (GET /pendientes/count) debe aumentar en 1");
    }

    @Test
    @Order(3)
    @DisplayName("[Camino Feliz] 3. El backend calcula correctamente los turnos en espera y la espera promedio")
    void test03_verificarMetricasBackend() throws Exception {
        HttpResponse<String> resCount = get("/pendientes/count");
        assertEquals(200, resCount.statusCode());
        int pendientes = Integer.parseInt(resCount.body().trim());

        HttpResponse<String> resPromedio = get("/espera-promedio");
        assertEquals(200, resPromedio.statusCode());
        double esperaPromedio = Double.parseDouble(resPromedio.body().trim());

        assertTrue(pendientes >= 0, "La cantidad de turnos pendientes debe ser >= 0");
        assertTrue(esperaPromedio >= 0.0, "El tiempo de espera promedio debe ser >= 0.0 minutos");
    }

    // =========================================================================
    // CAMINOS TRISTES (UNHAPPY PATHS - CASOS FALLIDOS Y DE BLOQUEO)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("[Camino Triste] 4. No se puede llamar al siguiente ni atender cuando la fila está vacía (404 / 400)")
    void test04_bloqueoCuandoFilaEstaVacia_yTurnoNoPendiente() throws Exception {
        int ultimoIdAtendido = -1;

        // Vaciar todos los turnos pendientes
        while (contarPendientes() > 0) {
            HttpResponse<String> resSig = get("/siguiente");
            ultimoIdAtendido = extraerId(resSig.body());
            put("/" + ultimoIdAtendido + "/atender");
        }

        assertEquals(0, contarPendientes(), "La fila debe tener 0 turnos pendientes");

        // 1. Intentar pedir el siguiente turno cuando la fila está vacía -> Debe responder 404 Not Found
        HttpResponse<String> resFilaVacia = get("/siguiente");
        assertEquals(404, resFilaVacia.statusCode(),
                "Cuando la fila está vacía, GET /api/turnos/siguiente debe bloquearse devolviendo 404 Not Found");

        // 2. Intentar volver a atender un turno que ya fue atendido -> Debe responder 400 Bad Request
        if (ultimoIdAtendido != -1) {
            HttpResponse<String> resDobleAtencion = put("/" + ultimoIdAtendido + "/atender");
            assertEquals(400, resDobleAtencion.statusCode(),
                    "Intentar atender un turno que ya no está pendiente debe fallar con 400 Bad Request");
        }

        // 3. Intentar atender un turno inexistente -> Debe responder 400 Bad Request
        HttpResponse<String> resInexistente = put("/999999/atender");
        assertEquals(400, resInexistente.statusCode(),
                "Intentar atender un ID inexistente debe fallar con 400 Bad Request");
    }

    @Test
    @Order(5)
    @DisplayName("[Camino Triste] 5. Solicitudes seguidas nunca repiten número ni se saltan el orden de la fila")
    void test05_noRepetirNumeros_niSaltarseElOrden() throws Exception {
        // Crear dos turnos seguidos
        HttpResponse<String> resPrimero = post("");
        int idPrimero = extraerId(resPrimero.body());
        int numeroPrimero = extraerNumero(resPrimero.body());

        HttpResponse<String> resSegundo = post("");
        int idSegundo = extraerId(resSegundo.body());
        int numeroSegundo = extraerNumero(resSegundo.body());

        // Validar que los números no se repitan y sean consecutivos
        assertNotEquals(numeroPrimero, numeroSegundo, "Dos turnos distintos no pueden tener el mismo número");
        assertEquals(numeroPrimero + 1, numeroSegundo, "El segundo turno debe recibir el número consecutivo siguiente");

        // Validar que el próximo en ser llamado sea el primero que llegó y NO el segundo
        HttpResponse<String> resSiguiente = get("/siguiente");
        assertEquals(200, resSiguiente.statusCode());
        int numeroLlamado = extraerNumero(resSiguiente.body());

        assertNotEquals(numeroSegundo, numeroLlamado,
                "El sistema nunca debe llamar al turno más reciente saltándose al que llegó antes");
        assertEquals(numeroPrimero, numeroLlamado,
                "El orden debe respetarse siempre: primero se atiende el turno que llegó primero");

        // Limpieza: dejar atendidos ambos turnos creados en la prueba
        put("/" + idPrimero + "/atender");
        put("/" + idSegundo + "/atender");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES HTTP SIMPLES
    // =========================================================================

    private int contarPendientes() throws Exception {
        HttpResponse<String> res = get("/pendientes/count");
        return Integer.parseInt(res.body().trim());
    }

    private void crearTurno() throws Exception {
        post("");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + path))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private int extraerId(String json) {
        Matcher m = ID_PATTERN.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new IllegalStateException("No se encontró 'id' en la respuesta JSON: " + json);
    }

    private int extraerNumero(String json) {
        Matcher m = NUMERO_PATTERN.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new IllegalStateException("No se encontró 'numero' en la respuesta JSON: " + json);
    }

    private List<Integer> extraerTodosLosNumeros(String jsonArray) {
        List<Integer> numeros = new ArrayList<>();
        Matcher m = NUMERO_PATTERN.matcher(jsonArray);
        while (m.find()) {
            numeros.add(Integer.parseInt(m.group(1)));
        }
        return numeros;
    }
}
