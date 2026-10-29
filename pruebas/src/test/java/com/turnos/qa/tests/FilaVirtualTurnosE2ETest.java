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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite E2E y Funcional para validar el reto "Fila Virtual de Turnos".
 *
 * Tecnologías:
 * - Java 17+
 * - Selenium WebDriver 4.x (ChromeDriver)
 * - JUnit 5 (@TestMethodOrder, @Order, @DisplayName, @BeforeEach, @AfterEach)
 * - Patrón Page Object Model (ClientePage y AsesorPage)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Suite E2E - Fila Virtual de Turnos (Angular + Java + Supabase PostgreSQL)")
public class FilaVirtualTurnosE2ETest {

    private static final String BASE_URL = System.getProperty("base.url", "http://localhost:4200");
    private static final Duration EXPLICIT_WAIT_TIMEOUT = Duration.ofSeconds(10);

    // Secuencia inicial precargada en base de datos (08:25:00 a 08:45:00)
    private static final List<Integer> SECUENCIA_FIFO_ESPERADA = List.of(6, 7, 8, 9, 10);
    private static final int ULTIMO_TURNO_HISTORICO_MATUTINO = 10;

    // Registro en memoria de turnos vistos/generados para validar unicidad estricta
    private static final Set<Integer> TURNOS_REGISTRADOS = new HashSet<>(SECUENCIA_FIFO_ESPERADA);

    private WebDriver driver;
    private WebDriverWait wait;
    private ClientePage clientePage;
    private AsesorPage asesorPage;

    @BeforeAll
    static void configurarDriverBinario() {
        // Resuelve automáticamente la versión de ChromeDriver compatible con el navegador instalado
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        // Opciones requeridas para entorno local y pipelines CI/CD
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");

        // Soporte opcional para ejecución Headless en servidores CI/CD (ej. GitHub Actions / GitLab CI)
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
        }

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, EXPLICIT_WAIT_TIMEOUT);

        // Inicialización de Page Objects
        clientePage = new ClientePage(driver, wait, BASE_URL);
        asesorPage = new AsesorPage(driver, wait, BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("TC01 - Verificar atención de turnos en estricto orden de llegada (FIFO: 6 -> 7 -> 8 -> 9 -> 10)")
    void test01_verificarAtencionEstrictoOrdenLlegadaFIFO() {
        asesorPage.navegar();

        // Validar que inicialmente existen los 5 turnos precargados en espera
        int enEsperaInicial = asesorPage.obtenerContadorTurnosEnEspera();
        assertEquals(
                SECUENCIA_FIFO_ESPERADA.size(),
                enEsperaInicial,
                "La fila inicial debe contener exactamente los 5 turnos precargados en BD (6 al 10)."
        );

        List<Integer> turnosLlamados = new ArrayList<>();

        // El asesor llama 5 veces seguidas
        for (int i = 0; i < SECUENCIA_FIFO_ESPERADA.size(); i++) {
            asesorPage.llamarSiguienteTurno();
            int turnoActual = asesorPage.obtenerNumeroTurnoActualLlamado();
            turnosLlamados.add(turnoActual);
        }

        // Comprobar que la secuencia llamada sea exactamente 6 -> 7 -> 8 -> 9 -> 10
        assertEquals(
                SECUENCIA_FIFO_ESPERADA,
                turnosLlamados,
                "Los turnos llamados por el asesor no respetaron el estricto orden FIFO (6 -> 7 -> 8 -> 9 -> 10)."
        );
    }

    @Test
    @Order(2)
    @DisplayName("TC02 - Cliente solicita turno (>= 11, sin duplicados) y aumenta el contador de espera en Asesor")
    void test02_clientePideTurno_noSeRepiteNumero_ySeEncola() {
        // 1. Consultar el contador actual de turnos en espera desde la vista del asesor
        asesorPage.navegar();
        int contadorAntesDeSolicitar = asesorPage.obtenerContadorTurnosEnEspera();

        // 2. Navegar a /cliente y solicitar un nuevo turno
        clientePage.navegar();
        clientePage.solicitarNuevoTurno();
        int primerTurnoGenerado = clientePage.obtenerNumeroTurnoAsignado();

        // Validar que continúa la numeración histórica (>= 11) y que no está duplicado
        assertTrue(
                primerTurnoGenerado > ULTIMO_TURNO_HISTORICO_MATUTINO,
                "El turno asignado (" + primerTurnoGenerado + ") debe ser mayor o igual a 11 (continuando el cierre matutino en 10)."
        );
        assertFalse(
                TURNOS_REGISTRADOS.contains(primerTurnoGenerado),
                "El número de turno " + primerTurnoGenerado + " ya fue asignado previamente (no se permiten duplicados)."
        );
        TURNOS_REGISTRADOS.add(primerTurnoGenerado);

        // 3. Solicitar un segundo turno consecutivo para confirmar incremento estricto sin repetición
        clientePage.solicitarNuevoTurno();
        int segundoTurnoGenerado = clientePage.obtenerNumeroTurnoAsignado();

        assertTrue(
                segundoTurnoGenerado > primerTurnoGenerado,
                "El segundo turno generado (" + segundoTurnoGenerado + ") debe ser consecutivo y mayor al anterior (" + primerTurnoGenerado + ")."
        );
        assertFalse(
                TURNOS_REGISTRADOS.contains(segundoTurnoGenerado),
                "El número de turno " + segundoTurnoGenerado + " no debe repetirse."
        );
        TURNOS_REGISTRADOS.add(segundoTurnoGenerado);

        // 4. Regresar a /asesor y validar que el contador de turnos en espera aumentó en 2
        asesorPage.navegar();
        int contadorEsperado = contadorAntesDeSolicitar + 2;
        asesorPage.esperarContadorTurnosEnEsperaIgualA(contadorEsperado);

        int contadorActual = asesorPage.obtenerContadorTurnosEnEspera();
        assertEquals(
                contadorEsperado,
                contadorActual,
                "El contador de turnos en espera en /asesor debe reflejar los nuevos turnos encolados desde /cliente."
        );
    }

    @Test
    @Order(3)
    @DisplayName("TC03 - Verificar métricas del Asesor (contador de espera y tiempo de espera promedio coherentes)")
    void test03_verificarMetricasAsesor() {
        asesorPage.navegar();

        // Asegurar que exista al menos un turno en espera para evaluar las métricas activas
        if (asesorPage.obtenerContadorTurnosEnEspera() == 0) {
            clientePage.navegar();
            clientePage.solicitarNuevoTurno();
            asesorPage.navegar();
        }

        int turnosEnEspera = asesorPage.obtenerContadorTurnosEnEspera();
        double tiempoPromedio = asesorPage.obtenerTiempoEsperaPromedio();
        String textoTiempoPromedio = asesorPage.obtenerTextoTiempoEsperaPromedio();

        // Validar coherencia numérica del contador de turnos en espera
        assertTrue(
                turnosEnEspera >= 0 && turnosEnEspera < 10_000,
                "El contador de personas en espera debe ser un entero no negativo coherente. Valor actual: " + turnosEnEspera
        );

        // Validar coherencia numérica del tiempo de espera promedio calculado por el backend
        assertFalse(
                Double.isNaN(tiempoPromedio) || Double.isInfinite(tiempoPromedio),
                "El tiempo de espera promedio no debe ser NaN ni infinito. Texto en UI: " + textoTiempoPromedio
        );
        assertTrue(
                tiempoPromedio >= 0.0 && tiempoPromedio <= 1440.0,
                "El tiempo de espera promedio debe encontrarse en un rango coherente (0 a 1440 min). Valor actual: " + tiempoPromedio
        );
    }

    @Test
    @Order(4)
    @DisplayName("TC04 - Bloqueo al llamar siguiente turno cuando la fila está vacía (botón deshabilitado o alerta visible)")
    void test04_bloqueoLlamarSiguiente_cuandoFilaEstaVacia() {
        asesorPage.navegar();

        // Consumir todos los turnos pendientes en la cola hasta llegar a 0 en espera
        int pendientes = asesorPage.obtenerContadorTurnosEnEspera();
        int salvaguardaIteraciones = 50; // Evita bucles infinitos si el backend presenta un bug al descontar

        while (pendientes > 0 && salvaguardaIteraciones > 0) {
            asesorPage.llamarSiguienteTurno();
            pendientes = asesorPage.obtenerContadorTurnosEnEspera();
            salvaguardaIteraciones--;
        }

        assertEquals(
                0,
                asesorPage.obtenerContadorTurnosEnEspera(),
                "El contador de turnos en espera debe llegar exactamente a 0 tras atender todos los turnos pendientes."
        );

        // Verificar regla de negocio: Con 0 turnos en espera, el botón está inhabilitado
        // O si permite clic, despliega el mensaje/alerta 'alerta-fila-vacia'
        boolean botonDeshabilitadoAntesDeClick = asesorPage.isBotonLlamarSiguienteDeshabilitado();

        if (!botonDeshabilitadoAntesDeClick) {
            asesorPage.intentarClickLlamarSiguiente();
        }

        boolean botonDeshabilitado = asesorPage.isBotonLlamarSiguienteDeshabilitado();
        boolean alertaFilaVaciaVisible = asesorPage.isAlertaFilaVaciaVisible();

        assertTrue(
                botonDeshabilitado || alertaFilaVaciaVisible,
                "Cuando la fila está vacía (0 en espera), el botón 'btn-llamar-siguiente' debe estar deshabilitado "
                        + "o debe mostrarse la alerta 'alerta-fila-vacia'."
        );
    }
}

