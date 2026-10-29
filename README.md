# TurnoFácil

Aplicación web para gestionar la fila virtual de una oficina de atención al cliente. Los clientes solicitan y consultan su turno desde el celular, y el asesor administra la fila y llama al siguiente turno desde su computador.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `client/` | Frontend en Angular: landing, panel del asesor y vista del cliente |
| `backend/` | API REST en Spring Boot 3.2 (Java 17) con PostgreSQL (Supabase) |
| `pruebas/` | Suite de 10 pruebas automatizadas (5 API Backend + 5 E2E Frontend en Google Chrome con Selenium 4 y JUnit 5) |

## Ejecución

- **Frontend:** desde `client/`, instalar dependencias con `npm install` y levantar con `npm start`. Queda en `http://localhost:4200`.
- **Backend:** desde `backend/`, ejecutar `mvn spring-boot:run` (requiere Java 17 y Maven 3.9+). Queda en `http://localhost:8080`.

La conexión a la base de datos se configura en `backend/src/main/resources/application.properties`. No se deben subir credenciales reales al repositorio.

## API de turnos

Base: `http://localhost:8080/api/turnos`. Intercambio en JSON, CORS habilitado para `http://localhost:4200`, sin autenticación por ahora.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista todos los turnos, ordenados por hora de solicitud |
| GET | `/{id}` | Busca un turno por id (404 si no existe) |
| GET | `/estado/{estado}` | Filtra por estado |
| GET | `/siguiente` | Devuelve el próximo turno pendiente (404 si no hay) |
| GET | `/espera-promedio` | Promedio de espera, en minutos, de los turnos atendidos |
| GET | `/pendientes/count` | Cantidad de turnos pendientes |
| POST | `/` | Crea un turno (sin cuerpo) |
| PUT | `/{id}/atender` | Marca el turno como atendido |
| PUT | `/{id}/cancelar` | Marca el turno como cancelado |

La colección de Postman está en `backend/postman/TurnosAPI.postman_collection.json`.

## Modelo de turno

Cada turno tiene `id`, `numero`, `horaSolicitud`, `horaAtencion` (vacía hasta que se atiende) y `estado`, que puede ser `pendiente`, `atendido` o `cancelado`.

## Reglas de negocio

- Un turno nace como `pendiente` y solo desde ahí puede pasar a `atendido` o `cancelado`.
- Atender o cancelar un turno que ya no está pendiente devuelve `400` con un mensaje en texto plano.
- El número de turno lo asigna el backend de forma secuencial; el cliente no lo envía.
- El asesor nunca debe llamar un turno cancelado: `/siguiente` solo considera turnos pendientes.
- No se puede llamar al siguiente si la fila está vacía (`/siguiente` devuelve `404` y el panel del asesor bloquea el botón).

---

## Pruebas automatizadas (`pruebas/`)

En la carpeta [`pruebas/`](pruebas/README.md) se encuentran **10 pruebas automatizadas** con **JUnit 5** y **Selenium WebDriver 4** que validan tanto el Backend como el Frontend bajo caminos felices y caminos tristes:

1. **[`BackendTurnosApiTest`](pruebas/src/test/java/com/turnos/qa/tests/BackendTurnosApiTest.java) (5 pruebas API REST)**:
   - **[Camino Feliz 1]** Atención en estricto orden de llegada (FIFO).
   - **[Camino Feliz 2]** Creación de turno nuevo con numeración consecutiva y aumento de la fila.
   - **[Camino Feliz 3]** Cálculo de turnos pendientes y espera promedio en minutos.
   - **[Camino Triste 4]** Bloqueo `404 Not Found` al pedir siguiente con fila vacía y `400 Bad Request` al intentar re-atender un turno o ID inexistente.
   - **[Camino Triste 5]** Solicitudes seguidas nunca repiten número ni se saltan el orden.
2. **[`FilaVirtualTurnosE2ETest`](pruebas/src/test/java/com/turnos/qa/tests/FilaVirtualTurnosE2ETest.java) (5 pruebas E2E en Google Chrome)**:
   - Abre Google Chrome automáticamente y mantiene **cada vista y paso en pantalla como mínimo 3 segundos (`3000 ms`)** para poder observar todo el recorrido visualmente.
   - Valida en la interfaz real (`/clientes` → `/clientes/turno` y `/administradores/panel`) los mismos 3 caminos felices y 2 caminos tristes.

### Cómo ejecutar las pruebas paso a paso

1. **Encender el Backend (Terminal 1):**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. **Encender el Frontend (Terminal 2):**
   ```bash
   cd client
   npm install
   npm start
   ```
3. **Ejecutar las pruebas (Terminal 3):**
   ```bash
   cd pruebas
   # Ejecuta las 10 pruebas (5 del Backend + 5 del Frontend abriendo Google Chrome)
   mvn test
   ```
   También puedes ejecutar cada suite por separado:
   ```bash
   mvn test -Dtest=BackendTurnosApiTest
   mvn test -Dtest=FilaVirtualTurnosE2ETest
   ```

> Consulta el detalle completo de cada prueba y los selectores HTML en [`pruebas/README.md`](pruebas/README.md).
