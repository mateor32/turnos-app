# Suite de Pruebas Automatizadas (E2E Selenium + Base de Datos PostgreSQL/Supabase)

Este directorio contiene la suite completa de aseguramiento de calidad (**QA Automation**) para el reto **"Fila Virtual de Turnos"**, dividida en dos niveles complementarios:

1. **Pruebas de Persistencia y Lógica Relacional en Base de Datos (`BaseDeDatosTurnosTest`)**: Validan directamente sobre el motor **PostgreSQL / Supabase** la creación de tablas, la importación del archivo histórico `turnos_hoy.csv`, las restricciones de unicidad bajo concurrencia, el ordenamiento FIFO, las reglas de transición de estados/tiempos y la exactitud de las funciones analíticas (`COUNT` y `AVG`).
2. **Pruebas Funcionales End-to-End en Navegador (`FilaVirtualTurnosE2ETest`)**: Validan con **Selenium WebDriver 4 + ChromeDriver** la interacción real sobre el frontend en Angular (`http://localhost:4200`) para las vistas `/cliente` y `/asesor`.

---

## 1. Contexto Funcional y Datos Iniciales (`turnos_hoy.csv`)

El sistema gestiona una fila virtual de atención con dos vistas principales (`/cliente` y `/asesor`) respaldadas por un backend en Java y una base de datos PostgreSQL en Supabase.

El archivo histórico matutino ([`src/test/resources/data/turnos_hoy.csv`](src/test/resources/data/turnos_hoy.csv)) contiene **10 registros** iniciales:

| Turno (`numero_turno`) | `hora_solicitud` | `hora_atencion` | `estado` | Descripción |
| :---: | :---: | :---: | :---: | :--- |
| **1** | `08:00:00` | `08:05:00` | `atendido` | Espera de 5 min |
| **2** | `08:04:00` | `08:12:00` | `atendido` | Espera de 8 min |
| **3** | `08:10:00` | *(NULL)* | `cancelado` | Turno cancelado |
| **4** | `08:15:00` | `08:22:00` | `atendido` | Espera de 7 min |
| **5** | `08:20:00` | `08:28:00` | `atendido` | Espera de 8 min |
| **6** | `08:25:00` | *(NULL)* | `pendiente` | 1° en espera por atender |
| **7** | `08:34:00` | *(NULL)* | `pendiente` | 2° en espera por atender |
| **8** | `08:38:00` | *(NULL)* | `pendiente` | 3° en espera por atender |
| **9** | `08:41:00` | *(NULL)* | `pendiente` | 4° en espera por atender |
| **10** | `08:45:00` | *(NULL)* | `pendiente` | 5° en espera por atender (cierre matutino) |

---

## 2. ¿Qué prueban las Pruebas de Base de Datos? (`BaseDeDatosTurnosTest`)

Ubicadas en [`BaseDeDatosTurnosTest.java`](src/test/java/com/turnos/qa/tests/BaseDeDatosTurnosTest.java) junto con el esquema DDL [`schema.sql`](src/test/resources/sql/schema.sql), garantizan que las reglas de negocio se cumplan en el motor PostgreSQL/Supabase independientemente de la capa de aplicación:

1. **`DB01` - Integridad y persistencia inicial (`testDB01_integridadYPersistenciaInicial_importacionCSV`)**:
   - Crea la tabla `turnos` y carga los registros de `turnos_hoy.csv`.
   - Comprueba los tipos de datos (`SERIAL`, `INT`, `TIME`, `VARCHAR`), la presencia de claves primarias técnicas y la distribución exacta de estados (`4 atendidos`, `1 cancelado`, `5 pendientes`).
2. **`DB02` - Cumplimiento de la regla de unicidad (`testDB02_cumplimientoReglaUnicidad_yOperacionesConcurrentes`)**:
   - Verifica que la restricción `UNIQUE (numero_turno)` rechace un número de turno repetido con código SQLState `23505`.
   - Ejecuta una prueba de concurrencia con **8 hilos simultáneos** intentando insertar el turno `11` al mismo instante, comprobando que exactamente **1 hilo** logra insertarlo y los **7 restantes** son bloqueados por el motor relacional.
3. **`DB03` - Orden cronológico estricto FIFO (`testDB03_ordenCronologicoEstrictoFIFO_consultaSeleccion`)**:
   - Evalúa que las consultas de selección sobre registros en estado `pendiente` ordenen prioritariamente por `hora_solicitud ASC`, recuperando siempre el turno más antiguo (`Turno 6 - 08:25:00`) y la secuencia exacta `[6, 7, 8, 9, 10]`.
4. **`DB04` - Consistencia de estados y marcas de tiempo (`testDB04_consistenciaEstadosYMarcasDeTiempo`)**:
   - Verifica que al pasar un registro de `pendiente` a `atendido`, la base de datos exija obligatoriamente `hora_atencion IS NOT NULL` (`chk_atendido_requiere_hora`).
   - Verifica que la base de datos rechace cualquier actualización donde `hora_atencion < hora_solicitud` (`chk_orden_temporal_atencion`, código SQLState `23514`).
5. **`DB05` - Precisión en funciones de agregación (`testDB05_precisionFuncionesAgregacion_countYAvgEsperaMinutos`)**:
   - Comprueba con exactitud el cálculo analítico de turnos en espera (`COUNT(*) WHERE estado = 'pendiente'` = `5`) y el promedio de espera en minutos (`AVG(EXTRACT(EPOCH FROM (hora_atencion - hora_solicitud)) / 60.0) WHERE estado = 'atendido'` = `7.0 minutos`).

---

## 3. ¿Qué prueban las Pruebas E2E con Selenium? (`FilaVirtualTurnosE2ETest`)

Ubicadas en [`FilaVirtualTurnosE2ETest.java`](src/test/java/com/turnos/qa/tests/FilaVirtualTurnosE2ETest.java), utilizan el patrón **Page Object Model (POM)** y **esperas explícitas** (`WebDriverWait` + `ExpectedConditions`, sin `Thread.sleep()`):

1. **`TC01` - Atención en estricto orden de llegada (`test01_verificarAtencionEstrictoOrdenLlegadaFIFO`)**:
   - El asesor llama 5 veces seguidas y se valida que los turnos llamados en pantalla sigan exactamente la secuencia **`6 -> 7 -> 8 -> 9 -> 10`**.
2. **`TC02` - Solicitud de turno consecutivo sin duplicados y encolado (`test02_clientePideTurno_noSeRepiteNumero_ySeEncola`)**:
   - Desde `/cliente` se solicitan nuevos turnos comprobando que sean **`>= 11`** y sin repetirse, y luego en `/asesor` se verifica que aumente el contador de turnos en espera.
3. **`TC03` - Métricas del panel de asesor (`test03_verificarMetricasAsesor`)**:
   - Valida que `contador-turnos-espera` y `tiempo-espera-promedio` contengan valores numéricos coherentes calculados por el backend.
4. **`TC04` - Bloqueo al llamar siguiente con fila vacía (`test04_bloqueoLlamarSiguiente_cuandoFilaEstaVacia`)**:
   - Consume todos los turnos hasta llegar a `0` en espera y comprueba que el botón `btn-llamar-siguiente` quede deshabilitado o se muestre la alerta `alerta-fila-vacia`.

---

## 4. Contrato de Selectores HTML (Para el equipo de Frontend)

| Vista | Elemento | Selector requerido (`id`) |
| :--- | :--- | :--- |
| **`/cliente`** | Botón para pedir turno | `id="btn-pedir-turno"` |
| **`/cliente`** | Número de turno generado | `id="ticket-numero-asignado"` |
| **`/asesor`** | Botón llamar siguiente | `id="btn-llamar-siguiente"` |
| **`/asesor`** | Turno mostrado en pantalla | `id="turno-actual-llamado"` |
| **`/asesor`** | Contador de personas en espera | `id="contador-turnos-espera"` |
| **`/asesor`** | Tiempo de espera promedio | `id="tiempo-espera-promedio"` |
| **`/asesor`** | Alerta/Mensaje de fila vacía | `id="alerta-fila-vacia"` |

---

## 5. Estructura del Directorio `pruebas/`

```text
pruebas/
├── pom.xml                                                 # Configuración Maven (Selenium 4, PostgreSQL JDBC, JUnit 5)
├── README.md                                               # Guía general de las pruebas E2E y de Base de Datos
└── src/test/
    ├── java/com/turnos/qa/
    │   ├── pages/
    │   │   ├── ClientePage.java                            # Page Object de la vista /cliente
    │   │   └── AsesorPage.java                             # Page Object de la vista /asesor
    │   └── tests/
    │       ├── BaseDeDatosTurnosTest.java                  # Suite de pruebas relacionales en PostgreSQL/Supabase (DB01-DB05)
    │       └── FilaVirtualTurnosE2ETest.java               # Suite de pruebas E2E con Selenium WebDriver (TC01-TC04)
    └── resources/
        ├── data/
        │   └── turnos_hoy.csv                              # Datos históricos iniciales (turnos 1 al 10)
        └── sql/
            └── schema.sql                                  # Creación de tabla, índices y restricciones (UNIQUE y CHECK)
```

---

## 6. Cómo ejecutar las pruebas

Ubicarse dentro de la carpeta `pruebas`:

```bash
cd pruebas
```

### A) Ejecutar únicamente las pruebas de Base de Datos (PostgreSQL / Supabase)
```bash
mvn test -Dtest=BaseDeDatosTurnosTest \
  -Ddb.url="jdbc:postgresql://db.<TU-PROYECTO>.supabase.co:5432/postgres" \
  -Ddb.user="postgres" \
  -Ddb.password="<TU-PASSWORD>"
```

### B) Ejecutar únicamente las pruebas E2E de Interfaz con Selenium
```bash
# Modo visual local
mvn test -Dtest=FilaVirtualTurnosE2ETest -Dbase.url=http://localhost:4200

# Modo Headless para CI/CD
mvn test -Dtest=FilaVirtualTurnosE2ETest -Dheadless=true -Dbase.url=http://localhost:4200
```

### C) Ejecutar la suite completa (Base de Datos + E2E)
```bash
mvn clean test \
  -Ddb.url="jdbc:postgresql://db.<TU-PROYECTO>.supabase.co:5432/postgres" \
  -Ddb.user="postgres" \
  -Ddb.password="<TU-PASSWORD>" \
  -Dbase.url=http://localhost:4200
```
