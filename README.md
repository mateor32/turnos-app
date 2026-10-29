# turnos-app

Sistema de gestión de turnos. Repositorio con backend Spring Boot 3.2 (Java 17 + PostgreSQL/Supabase) y frontend Angular.

> Este README es la **guía de integración** para el equipo frontend. Contiene toda la información necesaria para consumir el backend sin tener que leer el código Java.

---

## 1. Arranque rápido del backend

Requisitos: **Java 17** y **Maven 3.9+**.

```bash
cd backend
mvn spring-boot:run
```

Logs esperados al arrancar:
```
HikariPool-1 - Start completed.
Tomcat started on port 8080
Started TurnosApplication in X seconds
```

El backend queda escuchando en **`http://localhost:8080`**.

---

## 2. Datos de conexión

| Dato | Valor |
|---|---|
| **Base URL (desarrollo)** | `http://localhost:8080` |
| **Prefijo de endpoints** | `/api/turnos` |
| **CORS habilitado para** | `http://localhost:4200` (Angular dev server) |
| **Formato de intercambio** | JSON |
| **Autenticación** | No (por ahora) |

### Documentación interactiva (Swagger)

Con el backend corriendo:

- **Swagger UI** → http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** → http://localhost:8080/v3/api-docs

El Swagger permite probar cada endpoint desde el navegador.

### Colección Postman

En el repo: [`backend/postman/TurnosAPI.postman_collection.json`](backend/postman/TurnosAPI.postman_collection.json)

En Postman: *Import → Upload Files → selecciona el `.json`*. Trae las variables `{{baseUrl}}` y `{{turnoId}}` ya configuradas, y el request "Crear turno" guarda automáticamente el id del turno recién creado para los PUT siguientes.

---

## 3. Modelo de datos

### Turno

```ts
export interface Turno {
  id: number;
  numero: number;
  horaSolicitud: string;        // formato "HH:mm:ss" (java.time.LocalTime)
  horaAtencion: string | null;  // null hasta que el turno sea atendido
  estado: 'pendiente' | 'atendido' | 'cancelado';
}
```

**Notas**
- `id` y `numero` los asigna el backend al crear.
- `horaSolicitud` se setea automáticamente al crear el turno.
- `horaAtencion` queda en `null` hasta que se marca como atendido.
- Los estados son strings en minúscula.

---

## 4. Endpoints

Base: `http://localhost:8080/api/turnos`

| Método | Path | Descripción | Respuesta |
|---|---|---|---|
| `GET` | `/` | Listar todos (orden ASC por `horaSolicitud`) | `200 Turno[]` |
| `GET` | `/{id}` | Buscar por id | `200 Turno` · `404` si no existe |
| `GET` | `/estado/{estado}` | Filtrar por estado | `200 Turno[]` |
| `GET` | `/siguiente` | Próximo turno pendiente | `200 Turno` · `404` si no hay |
| `GET` | `/espera-promedio` | Promedio de espera en minutos de atendidos | `200 number` |
| `GET` | `/pendientes/count` | Cantidad de pendientes | `200 number` |
| `POST` | `/` | Crear turno (sin body) | `201 Turno` |
| `PUT` | `/{id}/atender` | Marcar como atendido | `200 Turno` · `400` si no estaba pendiente |
| `PUT` | `/{id}/cancelar` | Marcar como cancelado | `200 Turno` · `400` si no estaba pendiente |

### Ejemplos de respuesta

**`GET /api/turnos`** → `200 OK`
```json
[
  {
    "id": 1,
    "numero": 1,
    "horaSolicitud": "08:02:00",
    "horaAtencion": "08:05:00",
    "estado": "atendido"
  },
  {
    "id": 6,
    "numero": 6,
    "horaSolicitud": "08:25:00",
    "horaAtencion": null,
    "estado": "pendiente"
  }
]
```

**`POST /api/turnos`** → `201 Created`
```json
{
  "id": 12,
  "numero": 11,
  "horaSolicitud": "14:32:17",
  "horaAtencion": null,
  "estado": "pendiente"
}
```

**`GET /api/turnos/espera-promedio`** → `200 OK`
```json
8.2
```

**`GET /api/turnos/pendientes/count`** → `200 OK`
```json
5
```

**`PUT /api/turnos/99/atender`** (turno inexistente o no pendiente) → `400 Bad Request`
```
Turno no encontrado
```
> El body del `400` es **texto plano, no JSON**. En el frontend hay que leer la respuesta como texto en los errores.

---

## 5. Servicio Angular (listo para pegar)

```ts
// src/app/services/turno.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Turno {
  id: number;
  numero: number;
  horaSolicitud: string;
  horaAtencion: string | null;
  estado: 'pendiente' | 'atendido' | 'cancelado';
}

@Injectable({ providedIn: 'root' })
export class TurnoService {
  private readonly baseUrl = 'http://localhost:8080/api/turnos';

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<Turno[]> {
    return this.http.get<Turno[]>(this.baseUrl);
  }

  buscarPorId(id: number): Observable<Turno> {
    return this.http.get<Turno>(`${this.baseUrl}/${id}`);
  }

  filtrarPorEstado(estado: 'pendiente' | 'atendido' | 'cancelado'): Observable<Turno[]> {
    return this.http.get<Turno[]>(`${this.baseUrl}/estado/${estado}`);
  }

  siguientePendiente(): Observable<Turno> {
    return this.http.get<Turno>(`${this.baseUrl}/siguiente`);
  }

  esperaPromedio(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/espera-promedio`);
  }

  contarPendientes(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/pendientes/count`);
  }

  crear(): Observable<Turno> {
    return this.http.post<Turno>(this.baseUrl, null);
  }

  atender(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.baseUrl}/${id}/atender`, null);
  }

  cancelar(id: number): Observable<Turno> {
    return this.http.put<Turno>(`${this.baseUrl}/${id}/cancelar`, null);
  }
}
```

Recuerda registrar `provideHttpClient()` (Angular 17+ standalone) o `HttpClientModule` (classic) en la configuración del `AppModule` / `app.config.ts`.

### Ejemplo de uso en un componente

```ts
import { Component, OnInit, inject } from '@angular/core';
import { TurnoService, Turno } from './services/turno.service';

@Component({
  selector: 'app-turnos',
  template: `
    <button (click)="crear()">Nuevo turno</button>
    <ul>
      <li *ngFor="let t of turnos">
        #{{ t.numero }} — {{ t.estado }} ({{ t.horaSolicitud }})
      </li>
    </ul>
  `,
})
export class TurnosComponent implements OnInit {
  private svc = inject(TurnoService);
  turnos: Turno[] = [];

  ngOnInit(): void {
    this.recargar();
  }

  crear(): void {
    this.svc.crear().subscribe(() => this.recargar());
  }

  private recargar(): void {
    this.svc.listarTodos().subscribe(data => (this.turnos = data));
  }
}
```

---

## 6. Códigos de respuesta

| Código | Cuándo |
|---|---|
| `200 OK` | GET/PUT exitoso |
| `201 Created` | POST exitoso |
| `400 Bad Request` | Transición inválida (p. ej. atender un turno ya cancelado). Body = texto. |
| `404 Not Found` | Recurso inexistente |
| `500 Internal Server Error` | Error del backend. Revisar logs del servidor. |

---

## 7. Reglas de negocio relevantes para el front

- Un turno nace **`pendiente`** y solo desde ahí puede pasar a `atendido` o `cancelado`.
- Intentar atender/cancelar un turno que ya no está `pendiente` → `400 Bad Request`.
- `horaAtencion` solo tiene valor cuando el estado es `atendido`.
- El `numero` lo asigna el backend secuencialmente — no mandarlo desde el front.
- `GET /siguiente` devuelve el pendiente más antiguo; útil para "llamar al próximo".

---

## 8. Estructura del repo

```
turnos-app/
├─ backend/
│  ├─ pom.xml
│  ├─ postman/
│  │  └─ TurnosAPI.postman_collection.json
│  └─ src/main/
│     ├─ java/com/assessment/turnos/
│     │  ├─ TurnosApplication.java
│     │  ├─ controller/TurnoController.java
│     │  ├─ model/Turno.java
│     │  ├─ repository/TurnoRepository.java
│     │  └─ service/TurnoService.java
│     └─ resources/application.properties
└─ README.md  ← este archivo
```

---

## 9. Contacto / soporte

Si algún endpoint responde `500` o algo no cuadra con esta documentación:

1. Verifica que el backend esté corriendo (`mvn spring-boot:run`).
2. Prueba el endpoint desde el Swagger UI (http://localhost:8080/swagger-ui.html).
3. Mira la consola del backend — el stacktrace está habilitado y la respuesta `500` incluye el campo `trace` con el detalle.
4. Reporta el problema con: endpoint llamado, request completo, response recibido, hora.
