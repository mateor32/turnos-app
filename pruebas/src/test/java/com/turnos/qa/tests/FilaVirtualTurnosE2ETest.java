package com.turnos.qa.tests;

import com.turnos.qa.pages.AsesorPage;
import com.turnos.qa.pages.ClientePage;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
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
 * Suite de Pruebas End-to-End (E2E) en Google Chrome con Selenium WebDriver 4 y JUnit 5.
 *
 * <p>Valida sobre el Frontend Angular (http://localhost:4200) y el Backend Spring Boot
 * (http://localhost:8080) el cumplimiento estricto de la regla de negocio del caso:</p>
 * <blockquote>
 * "Los turnos se atienden en orden de llegada, los números no se repiten y no se puede
 * llamar al siguiente si la fila está vacía."
 * </blockquote>
 *
 * <p>Cada acción y vista clave permanece en pantalla como mínimo 3 segundos (3000 ms)
 * para permitir observar con claridad la ejecución automatizada en el navegador.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Pruebas E2E Frontend - Fila Virtual de Turnos (Caminos Felices y Caminos Tristes)")
public class FilaVirtualTurnosE2ETest {

    /** Tiempo mínimo de visualización en pantalla por paso (3 segundos = 3000 ms). */
    private static final long MINIMO_PAUSA_VISUAL_MS = 3000L;

    private static final String BASE_URL = resolveBaseUrl();
    private static final long PAUSA_VISUAL_MS = resolvePausaVisualMs();
    private static final Set<Integer> TURNOS_USADOS = new HashSet<>(List.of(1, 2, 3, 4, 5));

    private WebDriver driver;
    private ClientePage clientePage;
    private AsesorPage asesorPage;

    private static String resolveBaseUrl() {
        String prop = System.getProperty("base.url");
        return (prop == null || prop.isBlank()) ? "http://localhost:4200" : prop.trim();
    }

    private static long resolvePausaVisualMs() {
        String prop = System.getProperty("pausa.ms");
        if (prop != null && !prop.isBlank()) {
            try {
                return Math.max(MINIMO_PAUSA_VISUAL_MS, Long.parseLong(prop.trim()));
            } catch (NumberFormatException ignored) {
                // Usa el mínimo de 3 segundos si el valor no es numérico
            }
        }
        return MINIMO_PAUSA_VISUAL_MS;
    }

    @BeforeAll
    static void configurarDriver() {
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        clientePage = new ClientePage(driver, wait, BASE_URL, PAUSA_VISUAL_MS);
        asesorPage = new AsesorPage(driver, wait, BASE_URL, PAUSA_VISUAL_MS);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                Thread.sleep(PAUSA_VISUAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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

        // Si la fila quedó vacía por pruebas previas del backend, generamos 3 turnos desde la vista cliente
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
        TURNOS_USADOS.add(turnoPrimero);
        TURNOS_USADOS.add(turnoSegundo);

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
