# TurnoFácil

Aplicación web para gestionar la fila virtual de una oficina de atención al cliente. Los clientes solicitan y consultan su turno desde el celular, y el asesor administra la fila y llama al siguiente turno desde su computador.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `client/` | Frontend en Angular: landing, panel del asesor y vista del cliente |
| `backend/` | API REST en Spring Boot 3.2 (Java 17) con PostgreSQL (Supabase) |
| `pruebas/` | Pruebas automatizadas (QA) |

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
