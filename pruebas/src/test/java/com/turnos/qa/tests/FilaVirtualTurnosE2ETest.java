package com.turnos.qa.tests;

import com.turnos.qa.pages.AsesorPage;
import com.turnos.qa.pages.ClientePage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de Pruebas E2E basada en el Caso:
 * - Caminos Felices (Casos Exitosos): Flujo normal de pedir turno, atender en orden y ver métricas.
 * - Caminos Tristes (Casos de Bloqueo / Validación): Fila vacía, números duplicados y no saltarse el orden.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Pruebas E2E - Fila Virtual de Turnos (Caminos Felices y Caminos Tristes)")
public class FilaVirtualTurnosE2ETest {

    private static final String BASE_URL = resolveBaseUrl();
    private static final Set<Integer> TURNOS_USADOS = new HashSet<>(List.of(1, 2, 3, 4, 5));

    private WebDriver driver;
    private ClientePage clientePage;
    private AsesorPage asesorPage;

    private static String resolveBaseUrl() {
        String prop = System.getProperty("base.url");
        return (prop == null || prop.isBlank()) ? "http://localhost:4200" : prop.trim();
    }

    private static boolean isFrontendActivo() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .GET()
                    .build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void configurarDriver() {
        Assumptions.assumeTrue(
                isFrontendActivo(),
                "El Frontend no está encendido en " + BASE_URL + ". Se omiten las pruebas E2E de Chrome (enciende el frontend en el puerto 4200 para ejecutarlas)."
        );
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("start-maximized");
        if ("true".equalsIgnoreCase(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        clientePage = new ClientePage(driver, wait, BASE_URL);
        asesorPage = new AsesorPage(driver, wait, BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // =========================================================================
    // CAMINOS FELICES (HAPPY PATHS - CASOS EXITOSOS)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("[Camino Feliz] 1. El asesor atiende los turnos pendientes en estricto orden de llegada (FIFO)")
    void test01_verificarAtencionEstrictoOrdenLlegadaFIFO() {
        asesorPage.navegar();

        // Si la fila quedó vacía por pruebas previas, generamos 3 turnos desde la vista cliente
        if (asesorPage.obtenerTurnosEnEspera() < 3) {
            clientePage.navegar();
            for (int i = 0; i < 3; i++) {
                clientePage.pedirTurno();
                clientePage.obtenerNumeroTurnoAsignado();
            }
            asesorPage.navegar();
        }

        int cantidadPendientes = asesorPage.obtenerTurnosEnEspera();
        List<Integer> turnosLlamados = new ArrayList<>();
        for (int i = 0; i < cantidadPendientes; i++) {
            asesorPage.llamarSiguiente();
            int llamado = asesorPage.obtenerTurnoActualLlamado();
            turnosLlamados.add(llamado);
            TURNOS_USADOS.add(llamado);
        }

        for (int i = 1; i < turnosLlamados.size(); i++) {
            assertTrue(turnosLlamados.get(i) > turnosLlamados.get(i - 1),
                    "El orden de atención debe respetar el estricto orden de llegada (FIFO): " + turnosLlamados);
        }
    }

    @Test
    @Order(2)
    @DisplayName("[Camino Feliz] 2. El cliente pide un turno nuevo (>= 11) y aumenta la fila en espera")
    void test02_clientePideTurno_noSeRepiteNumero_ySeEncola() {
        asesorPage.navegar();
        int enEsperaAntes = asesorPage.obtenerTurnosEnEspera();

        clientePage.navegar();
        clientePage.pedirTurno();
        int nuevoTurno = clientePage.obtenerNumeroTurnoAsignado();

        assertTrue(nuevoTurno >= 11, "El turno asignado debe continuar la numeración de la mañana (>= 11)");
        assertFalse(TURNOS_USADOS.contains(nuevoTurno), "El número de turno no debe repetirse");
        TURNOS_USADOS.add(nuevoTurno);

        asesorPage.navegar();
        assertEquals(enEsperaAntes + 1, asesorPage.obtenerTurnosEnEspera(),
                "El contador de turnos en espera debe aumentar en 1");
    }

    @Test
    @Order(3)
    @DisplayName("[Camino Feliz] 3. La vista del asesor muestra correctamente los turnos en espera y la espera promedio")
    void test03_verificarMetricasAsesor() {
        asesorPage.navegar();

        int turnosEnEspera = asesorPage.obtenerTurnosEnEspera();
        double esperaPromedio = asesorPage.obtenerTiempoEsperaPromedio();

        assertTrue(turnosEnEspera >= 0, "La cantidad de turnos en espera debe ser un número válido (>= 0)");
        assertTrue(esperaPromedio >= 0.0, "El tiempo de espera promedio debe ser un número válido (>= 0)");
    }

    // =========================================================================
    // CAMINOS TRISTES (UNHAPPY PATHS - CASOS DE BLOQUEO Y REGLAS ESTRICTAS)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("[Camino Triste] 4. Bloqueo al intentar llamar al siguiente cuando la fila está vacía")
    void test04_bloqueoLlamarSiguiente_cuandoFilaEstaVacia() {
        asesorPage.navegar();

        // Vaciar todos los turnos pendientes en la fila
        while (asesorPage.obtenerTurnosEnEspera() > 0) {
            asesorPage.llamarSiguiente();
        }

        assertEquals(0, asesorPage.obtenerTurnosEnEspera(), "La fila debe estar vacía (0 en espera)");

        // Intentar llamar cuando no hay nadie en la fila
        if (!asesorPage.isBotonLlamarDeshabilitado()) {
            asesorPage.llamarSiguiente();
        }

        assertTrue(
                asesorPage.isBotonLlamarDeshabilitado() || asesorPage.isAlertaFilaVaciaVisible(),
                "Con la fila vacía no se debe permitir llamar al siguiente (botón deshabilitado o alerta visible)"
        );
    }

    @Test
    @Order(5)
    @DisplayName("[Camino Triste] 5. Solicitudes seguidas de clientes nunca generan números repetidos ni desordenados")
    void test05_noPermitirNumerosRepetidosNiSaltarseElOrden() {
        clientePage.navegar();

        // Dos clientes piden turno de forma consecutiva
        clientePage.pedirTurno();
        int turnoPrimero = clientePage.obtenerNumeroTurnoAsignado();

        clientePage.pedirTurno();
        int turnoSegundo = clientePage.obtenerNumeroTurnoAsignado();

        // Validar que no se repitan números
        assertNotEquals(turnoPrimero, turnoSegundo, "Dos solicitudes distintas no pueden recibir el mismo número de turno");
        assertFalse(TURNOS_USADOS.contains(turnoPrimero), "El primer turno no debe repetir un número histórico");
        assertFalse(TURNOS_USADOS.contains(turnoSegundo), "El segundo turno no debe repetir un número histórico");

        // Validar en el asesor que no se salte el orden (no debe llamar al segundo antes que al primero)
        asesorPage.navegar();
        asesorPage.llamarSiguiente();
        int turnoAtendido = asesorPage.obtenerTurnoActualLlamado();

        assertNotEquals(turnoSegundo, turnoAtendido,
                "El asesor no puede atender un turno más reciente saltándose al que llegó primero");
        assertEquals(turnoPrimero, turnoAtendido,
                "El orden debe respetarse en todos los casos: primero se atiende el turno que llegó antes");
    }
}
