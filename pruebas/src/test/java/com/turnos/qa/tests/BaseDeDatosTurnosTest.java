package com.turnos.qa.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de Pruebas de Base de Datos (PostgreSQL / Supabase).
 *
 * Valida directamente sobre el motor relacional:
 * 1. Integridad y persistencia inicial de los registros históricos (turnos_hoy.csv).
 * 2. Cumplimiento de la regla de unicidad (UNIQUE constraint) incluso bajo concurrencia.
 * 3. Orden cronológico estricto (FIFO) mediante consultas ordenadas por hora_solicitud ASC.
 * 4. Consistencia de estados y marcas de tiempo (hora_atencion obligatoria y >= hora_solicitud).
 * 5. Precisión en funciones de agregación analíticas: COUNT(pendientes) y AVG(hora_atencion - hora_solicitud).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Suite BD - Persistencia, Integridad y Lógica Relacional (PostgreSQL / Supabase)")
public class BaseDeDatosTurnosTest {

    // Configuración de conexión JDBC hacia PostgreSQL / Supabase (configurable por parámetros Maven o variables de entorno)
    private static final String DB_URL = System.getProperty(
            "db.url",
            System.getenv().getOrDefault("SUPABASE_DB_URL", "jdbc:postgresql://localhost:5432/postgres")
    );
    private static final String DB_USER = System.getProperty(
            "db.user",
            System.getenv().getOrDefault("SUPABASE_DB_USER", "postgres")
    );
    private static final String DB_PASSWORD = System.getProperty(
            "db.password",
            System.getenv().getOrDefault("SUPABASE_DB_PASSWORD", "postgres")
    );

    private Connection connection;

    @BeforeEach
    void setUpDatabase() throws Exception {
        connection = obtenerNuevaConexion();
        inicializarEsquemaRelacional(connection);
        importarTurnosHistoricosDesdeCSV(connection);
    }

    @AfterEach
    void tearDownDatabase() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * 1. Integridad y persistencia inicial:
     * Verifica la correcta importación del archivo turnos_hoy.csv con sus tipos de datos,
     * marcas de tiempo y estados correspondientes (atendido, pendiente, cancelado).
     */
    @Test
    @Order(1)
    @DisplayName("DB01 - Integridad y persistencia inicial: importación de turnos_hoy.csv con tipos, horas y estados")
    void testDB01_integridadYPersistenciaInicial_importacionCSV() throws SQLException {
        String sql = "SELECT id, numero_turno, hora_solicitud, hora_atencion, estado FROM turnos ORDER BY numero_turno ASC";

        int totalRegistros = 0;
        int atendidos = 0;
        int pendientes = 0;
        int cancelados = 0;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                totalRegistros++;
                int id = rs.getInt("id");
                int numeroTurno = rs.getInt("numero_turno");
                Time horaSolicitud = rs.getTime("hora_solicitud");
                Time horaAtencion = rs.getTime("hora_atencion");
                String estado = rs.getString("estado");

                assertTrue(id > 0, "Cada registro debe poseer una clave primaria técnica autogenerada > 0.");
                assertEquals(totalRegistros, numeroTurno, "El número de turno importado debe coincidir con la secuencia 1..10.");
                assertNotNull(horaSolicitud, "El campo hora_solicitud nunca debe ser NULL.");
                assertNotNull(estado, "El campo estado nunca debe ser NULL.");

                switch (estado) {
                    case "atendido" -> {
                        atendidos++;
                        assertNotNull(horaAtencion, "Los turnos en estado 'atendido' deben tener hora_atencion registrada.");
                        assertTrue(
                                !horaAtencion.before(horaSolicitud),
                                "En turnos atendidos, hora_atencion debe ser >= hora_solicitud."
                        );
                    }
                    case "pendiente" -> {
                        pendientes++;
                        assertNull(horaAtencion, "Los turnos en estado 'pendiente' deben tener hora_atencion en NULL.");
                    }
                    case "cancelado" -> {
                        cancelados++;
                        assertNull(horaAtencion, "Los turnos en estado 'cancelado' no deben registrar hora_atencion.");
                    }
                    default -> throw new AssertionError("Estado desconocido encontrado en BD: " + estado);
                }
            }
        }

        assertEquals(10, totalRegistros, "Se deben haber importado exactamente los 10 registros históricos de turnos_hoy.csv.");
        assertEquals(4, atendidos, "Deben existir exactamente 4 turnos en estado 'atendido' (turnos 1, 2, 4 y 5).");
        assertEquals(1, cancelados, "Debe existir exactamente 1 turno en estado 'cancelado' (turno 3).");
        assertEquals(5, pendientes, "Deben existir exactamente 5 turnos en estado 'pendiente' (turnos 6 al 10).");
    }

    /**
     * 2. Cumplimiento de la regla de unicidad:
     * Comprueba mediante restricciones (UNIQUE / clave primaria técnica) que el motor PostgreSQL
     * rechace la inserción de números de turno duplicados tanto en ejecución simple como concurrente.
     */
    @Test
    @Order(2)
    @DisplayName("DB02 - Cumplimiento de unicidad: rechazo de números de turno duplicados bajo concurrencia")
    void testDB02_cumplimientoReglaUnicidad_yOperacionesConcurrentes() throws InterruptedException {
        String insertSql = "INSERT INTO turnos (numero_turno, hora_solicitud, estado) VALUES (?, ?, 'pendiente')";

        // 2.1 Validación directa: intentar insertar nuevamente el turno 10 (ya existente en turnos_hoy.csv)
        SQLException excepcionDuplicado = assertThrows(
                SQLException.class,
                () -> {
                    try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                        ps.setInt(1, 10);
                        ps.setTime(2, Time.valueOf("08:50:00"));
                        ps.executeUpdate();
                    }
                },
                "El motor de base de datos debe rechazar la inserción de un número de turno duplicado (10)."
        );

        // Código SQLState 23505 corresponde a unique_violation en PostgreSQL
        assertEquals(
                "23505",
                excepcionDuplicado.getSQLState(),
                "La excepción lanzada debe corresponder a una violación de restricción UNIQUE (SQLState 23505)."
        );

        // 2.2 Validación bajo concurrencia: 8 hilos intentan insertar simultáneamente el turno 11
        int numeroHilos = 8;
        int turnoConcurrente = 11;
        ExecutorService pool = Executors.newFixedThreadPool(numeroHilos);
        CountDownLatch listoParaArrancar = new CountDownLatch(1);
        CountDownLatch finalizado = new CountDownLatch(numeroHilos);

        AtomicInteger insercionesExitosas = new AtomicInteger(0);
        AtomicInteger rechazosPorUnicidad = new AtomicInteger(0);

        for (int i = 0; i < numeroHilos; i++) {
            pool.submit(() -> {
                try (Connection connHilo = obtenerNuevaConexion();
                     PreparedStatement ps = connHilo.prepareStatement(insertSql)) {
                    listoParaArrancar.await();
                    ps.setInt(1, turnoConcurrente);
                    ps.setTime(2, Time.valueOf("08:55:00"));
                    ps.executeUpdate();
                    insercionesExitosas.incrementAndGet();
                } catch (SQLException e) {
                    if ("23505".equals(e.getSQLState())) {
                        rechazosPorUnicidad.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finalizado.countDown();
                }
            });
        }

        // Liberar todos los hilos al mismo instante
        listoParaArrancar.countDown();
        assertTrue(finalizado.await(15, TimeUnit.SECONDS), "Las operaciones concurrentes deben finalizar a tiempo.");
        pool.shutdownNow();

        assertEquals(1, insercionesExitosas.get(), "Exactamente UN hilo debe lograr insertar el turno 11.");
        assertEquals(
                numeroHilos - 1,
                rechazosPorUnicidad.get(),
                "Los demás hilos concurrentes deben ser rechazados por la restricción UNIQUE de la base de datos."
        );
    }

    /**
     * 3. Orden cronológico estricto (FIFO):
     * Evalúa que las consultas de selección ordenen prioritariamente por hora_solicitud ASC
     * sobre los registros en estado 'pendiente', garantizando recuperar siempre el turno más antiguo.
     */
    @Test
    @Order(3)
    @DisplayName("DB03 - Orden cronológico estricto (FIFO): selección por hora_solicitud ASC en estado pendiente")
    void testDB03_ordenCronologicoEstrictoFIFO_consultaSeleccion() throws SQLException {
        String sqlColaFifo = """
                SELECT numero_turno, hora_solicitud
                FROM turnos
                WHERE estado = 'pendiente'
                ORDER BY hora_solicitud ASC, numero_turno ASC
                """;

        List<Integer> secuenciaTurnosPendientes = new ArrayList<>();
        List<Time> secuenciaHorasSolicitud = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sqlColaFifo);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                secuenciaTurnosPendientes.add(rs.getInt("numero_turno"));
                secuenciaHorasSolicitud.add(rs.getTime("hora_solicitud"));
            }
        }

        // Verificar que la secuencia recuperada sea exactamente 6 -> 7 -> 8 -> 9 -> 10
        assertEquals(
                List.of(6, 7, 8, 9, 10),
                secuenciaTurnosPendientes,
                "La consulta FIFO sobre registros pendientes debe recuperar estrictamente la secuencia 6 -> 7 -> 8 -> 9 -> 10."
        );

        // Verificar que el primer turno en salir sea el más antiguo (Turno 6 solicitado a las 08:25:00)
        assertEquals(
                Time.valueOf("08:25:00"),
                secuenciaHorasSolicitud.get(0),
                "El primer turno recuperado para atención debe ser el de las 08:25:00 (Turno 6)."
        );

        // Comprobar que cada hora_solicitud sea menor o igual a la siguiente en la lista
        for (int i = 0; i < secuenciaHorasSolicitud.size() - 1; i++) {
            assertTrue(
                    !secuenciaHorasSolicitud.get(i).after(secuenciaHorasSolicitud.get(i + 1)),
                    "El orden cronológico ascendente se rompió entre la posición " + i + " y " + (i + 1)
            );
        }
    }

    /**
     * 4. Consistencia de estados y marcas de tiempo:
     * Valida que al actualizar un registro de 'pendiente' a 'atendido', el campo hora_atencion
     * se registre obligatoriamente y cumpla la condición temporal hora_atencion >= hora_solicitud.
     */
    @Test
    @Order(4)
    @DisplayName("DB04 - Consistencia de estados y tiempos: hora_atencion obligatoria y >= hora_solicitud al atender")
    void testDB04_consistenciaEstadosYMarcasDeTiempo() throws SQLException {
        // Caso 4.1: Intentar pasar el Turno 6 (08:25:00) a 'atendido' dejando hora_atencion en NULL -> Debe fallar
        String sqlSinHoraAtencion = "UPDATE turnos SET estado = 'atendido', hora_atencion = NULL WHERE numero_turno = 6";
        SQLException errorSinHora = assertThrows(
                SQLException.class,
                () -> {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate(sqlSinHoraAtencion);
                    }
                },
                "La BD debe impedir que un turno pase a 'atendido' sin registrar hora_atencion."
        );
        // SQLState 23514 corresponde a check_violation en PostgreSQL
        assertEquals("23514", errorSinHora.getSQLState(), "Se esperaba violación de restricción CHECK (23514).");

        // Caso 4.2: Intentar pasar el Turno 6 (solicitado 08:25:00) a 'atendido' con hora_atencion = 08:20:00 (< hora_solicitud) -> Debe fallar
        String sqlHoraIncoherente = "UPDATE turnos SET estado = 'atendido', hora_atencion = '08:20:00' WHERE numero_turno = 6";
        SQLException errorOrdenTemporal = assertThrows(
                SQLException.class,
                () -> {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate(sqlHoraIncoherente);
                    }
                },
                "La BD debe rechazar una hora_atencion anterior a hora_solicitud (08:20:00 < 08:25:00)."
        );
        assertEquals("23514", errorOrdenTemporal.getSQLState(), "Se esperaba violación de restricción CHECK temporal (23514).");

        // Caso 4.3: Actualizar el Turno 6 correctamente a 'atendido' con hora_atencion = 08:30:00 (>= 08:25:00) -> Debe tener éxito
        String sqlActualizacionValida = "UPDATE turnos SET estado = 'atendido', hora_atencion = '08:30:00' WHERE numero_turno = 6";
        try (Statement st = connection.createStatement()) {
            int filasActualizadas = st.executeUpdate(sqlActualizacionValida);
            assertEquals(1, filasActualizadas, "El turno 6 debe actualizarse correctamente cuando hora_atencion >= hora_solicitud.");
        }
    }

    /**
     * 5. Precisión en funciones de agregación:
     * Verifica que las consultas analíticas calculen con exactitud la cantidad de turnos en espera (COUNT)
     * y la espera promedio en minutos (AVG(hora_atencion - hora_solicitud)).
     */
    @Test
    @Order(5)
    @DisplayName("DB05 - Precisión en funciones de agregación: COUNT de espera y AVG(hora_atencion - hora_solicitud)")
    void testDB05_precisionFuncionesAgregacion_countYAvgEsperaMinutos() throws SQLException {
        // 5.1 Verificar COUNT exacto de turnos en espera ('pendiente')
        String sqlCountPendientes = "SELECT COUNT(*) AS total_en_espera FROM turnos WHERE estado = 'pendiente'";
        try (PreparedStatement ps = connection.prepareStatement(sqlCountPendientes);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            int totalEnEspera = rs.getInt("total_en_espera");
            assertEquals(5, totalEnEspera, "El COUNT de turnos en estado 'pendiente' debe ser exactamente 5 (turnos 6 al 10).");
        }

        // 5.2 Verificar AVG(hora_atencion - hora_solicitud) en minutos sobre los turnos atendidos de turnos_hoy.csv:
        // Turno 1: 08:05:00 - 08:00:00 = 5.0 min
        // Turno 2: 08:12:00 - 08:04:00 = 8.0 min
        // Turno 4: 08:22:00 - 08:15:00 = 7.0 min
        // Turno 5: 08:28:00 - 08:20:00 = 8.0 min
        // Promedio exacto esperado = (5 + 8 + 7 + 8) / 4 = 28 / 4 = 7.0 minutos
        String sqlPromedioMinutos = """
                SELECT AVG(EXTRACT(EPOCH FROM (hora_atencion - hora_solicitud)) / 60.0) AS promedio_espera_min
                FROM turnos
                WHERE estado = 'atendido'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sqlPromedioMinutos);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            double promedioCalculado = rs.getDouble("promedio_espera_min");
            assertEquals(
                    7.0,
                    promedioCalculado,
                    0.001,
                    "El promedio de espera en minutos calculado con AVG(hora_atencion - hora_solicitud) debe ser exactamente 7.0 min."
            );
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE CONEXIÓN, CREACIÓN DE ESQUEMA E IMPORTACIÓN DE CSV
    // =========================================================================

    private Connection obtenerNuevaConexion() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void inicializarEsquemaRelacional(Connection conn) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sql/schema.sql")) {
            assertNotNull(is, "No se encontró el archivo sql/schema.sql en el classpath de pruebas.");
            String scriptSql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            try (Statement st = conn.createStatement()) {
                st.execute(scriptSql);
            }
        }
    }

    private void importarTurnosHistoricosDesdeCSV(Connection conn) throws Exception {
        String insertSql = "INSERT INTO turnos (numero_turno, hora_solicitud, hora_atencion, estado) VALUES (?, ?, ?, ?)";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/turnos_hoy.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            String linea;
            boolean esEncabezado = true;

            while ((linea = reader.readLine()) != null) {
                if (esEncabezado) {
                    esEncabezado = false;
                    continue;
                }
                if (linea.isBlank()) {
                    continue;
                }

                String[] columnas = linea.split(",", -1);
                int numeroTurno = Integer.parseInt(columnas[0].trim());
                Time horaSolicitud = Time.valueOf(columnas[1].trim());
                String horaAtencionStr = columnas[2].trim();
                String estado = columnas[3].trim();

                ps.setInt(1, numeroTurno);
                ps.setTime(2, horaSolicitud);
                if (horaAtencionStr.isEmpty()) {
                    ps.setNull(3, Types.TIME);
                } else {
                    ps.setTime(3, Time.valueOf(horaAtencionStr));
                }
                ps.setString(4, estado);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
