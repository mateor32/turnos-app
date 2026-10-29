# Documentación de Pruebas Automatizadas (QA) — TurnoFácil

Este directorio contiene la suite completa de **10 pruebas automatizadas** con **JUnit 5** y **Selenium WebDriver 4** (5 pruebas de API sobre el Backend y 5 pruebas End-to-End en Google Chrome sobre el Frontend), estructuradas bajo el patrón **Page Object Model (POM)** y divididas en **Caminos Felices (Happy Paths)** y **Caminos Tristes (Unhappy Paths / Reglas de Bloqueo)**.

---

## 1. Objetivo y Regla de Negocio Validada

Todas las pruebas están diseñadas para demostrar el cumplimiento estricto de los requerimientos del caso:

- **FRONT (`http://localhost:4200`)**: Una vista para pedir turno (`/clientes` → `/clientes/turno`) y otra para que el asesor llame al siguiente (`/administradores/panel` o `/asesor`), mostrando los turnos en espera y el tiempo de espera promedio.
- **BACK (`http://localhost:8080/api/turnos`)**: Servicio REST que asigna los turnos secuencialmente, administra el orden de la fila y calcula la espera promedio.
- **DATOS**: Los turnos de la mañana cargados en la base de datos (1 al 5 atendidos y 6, 7, 8, 9 y 10 pendientes en orden de llegada).
- **REGLA DE ORO DEL CASO**:
  > *"Los turnos se atienden en orden de llegada, los números no se repiten y no se puede llamar al siguiente si la fila está vacía."*

---

## 2. Arquitectura del Proyecto de Pruebas (`pruebas/`)

```text
pruebas/
├─ pom.xml                                        # Configuración Maven (Selenium 4.25, JUnit 5.11, WebDriverManager)
├─ README.md                                      # Documentación detallada de la suite de pruebas
└─ src/test/java/com/turnos/qa/
   ├─ pages/
   │  ├─ AsesorPage.java                          # Page Object del Panel del Asesor (/administradores/panel)
   │  └─ ClientePage.java                         # Page Object del Portal del Cliente (/clientes -> /clientes/turno)
   └─ tests/
      ├─ BackendTurnosApiTest.java                # 5 pruebas directas sobre los endpoints REST del Backend
      └─ FilaVirtualTurnosE2ETest.java            # 5 pruebas E2E en Google Chrome sobre el Frontend Angular
```

### Tiempo de visualización en pantalla (Mínimo 3 segundos)
Para que cualquier evaluador o desarrollador pueda observar en tiempo real cada acción del navegador durante la ejecución de `FilaVirtualTurnosE2ETest`, cada paso visual (carga del panel, asignación de ticket en pantalla, llamado de turno y cierre de prueba) permanece visible en pantalla **como mínimo 3 segundos (`3000 ms`)**, configurado en la propiedad `<pausa.ms>3000</pausa.ms>` del [`pom.xml`](pom.xml).

---

## 3. Matriz Resumen de las 10 Pruebas Automatizadas

| # | Suite | Método de Prueba | Tipo | Escenario / Qué valida | Resultado Esperado |
|---|---|---|---|---|---|
| **1** | Backend API | `test01_atencionEnOrdenDeLlegadaFIFO` | Camino Feliz | Consulta los turnos pendientes y los llama/atiende uno por uno (`GET /siguiente` + `PUT /{id}/atender`). | Se entregan y atienden en estricto orden de llegada (FIFO). |
| **2** | Backend API | `test02_crearTurno_asignaNumeroConsecutivo_yAumentaFila` | Camino Feliz | Solicita un nuevo turno con `POST /api/turnos`. | Responde `201 Created`, estado `pendiente` y el contador `/pendientes/count` sube en `+1`. |
| **3** | Backend API | `test03_verificarMetricasBackend` | Camino Feliz | Consulta `GET /pendientes/count` y `GET /espera-promedio`. | Ambos responden `200 OK` con valores numéricos válidos (`>= 0`). |
| **4** | Backend API | `test04_bloqueoCuandoFilaEstaVacia_yTurnoNoPendiente` | Camino Triste | Vacía la fila e intenta llamar al siguiente (`GET /siguiente`), re-atender un turno ya atendido o un ID inexistente. | `404 Not Found` cuando la fila está vacía y `400 Bad Request` al intentar atender un turno no pendiente o inexistente. |
| **5** | Backend API | `test05_noRepetirNumeros_niSaltarseElOrden` | Camino Triste | Crea dos turnos consecutivos y consulta cuál es el próximo en ser llamado (`GET /siguiente`). | Números distintos/consecutivos (`n` y `n+1`), y el sistema llama primero al que llegó antes sin saltarse la fila. |
| **6** | Frontend E2E | `test01_verificarAtencionEstrictoOrdenLlegadaFIFO` | Camino Feliz | Abre Chrome en el panel del asesor y llama uno por uno a los turnos en espera. | Los códigos llamados en pantalla (`#turno-actual-llamado`) siguen un orden estrictamente creciente (FIFO). |
| **7** | Frontend E2E | `test02_clientePideTurno_noSeRepiteNumero_ySeEncola` | Camino Feliz | Un cliente entra a `/clientes`, solicita turno y luego se revisa el panel del asesor. | Se asigna un código único (`>= 11`, ej. `A-011`) sin repetir y el contador de espera del asesor aumenta en `+1`. |
| **8** | Frontend E2E | `test03_verificarMetricasAsesor` | Camino Feliz | Abre el panel del asesor y lee los indicadores de fila en espera y espera promedio. | Se visualizan valores numéricos válidos (`>= 0`) en `#contador-turnos-espera` y `#tiempo-espera-promedio`. |
| **9** | Frontend E2E | `test04_bloqueoLlamarSiguiente_cuandoFilaEstaVacia` | Camino Triste | El asesor atiende todos los turnos hasta dejar la fila en `0`. | Con `0` en espera, el botón `#btn-llamar-siguiente` queda deshabilitado (`disabled`) y se muestra la alerta `#alerta-fila-vacia`. |
| **10** | Frontend E2E | `test05_noPermitirNumerosRepetidosNiSaltarseElOrden` | Camino Triste | Dos clientes piden turno de forma consecutiva en el navegador y el asesor llama al siguiente. | Los tickets asignados no se repiten y el asesor atiende primero al primer cliente sin saltarse el orden. |

---

## 4. Contrato de Selectores HTML (Frontend ↔ Selenium)

Los Page Objects [`ClientePage.java`](src/test/java/com/turnos/qa/pages/ClientePage.java) y [`AsesorPage.java`](src/test/java/com/turnos/qa/pages/AsesorPage.java) interactúan con los siguientes `id` en los componentes de Angular (`client/`):

### Vista Cliente (`/clientes` → `/clientes/turno`, alias `/cliente`)
- `id="btn-pedir-turno"`: Botón **"Solicitar turno"** en [`access.component.html`](../client/src/app/pages/access/access.component.html).
- `id="ticket-numero-asignado"`: Código del turno asignado en pantalla (ej. `A-011`) en [`client.component.html`](../client/src/app/pages/client/client.component.html).

### Vista Asesor (`/administradores/panel`, alias `/asesor`)
Definidos en [`admin-dashboard.component.html`](../client/src/app/pages/admin-dashboard/admin-dashboard.component.html):
- `id="contador-turnos-espera"`: Indicador numérico de personas en espera.
- `id="tiempo-espera-promedio"`: Indicador del tiempo promedio de espera en minutos.
- `id="turno-actual-llamado"`: Código del turno actualmente en atención (ej. `A-006`).
- `id="btn-llamar-siguiente"`: Botón principal **"Llamar siguiente turno"** (se deshabilita automáticamente cuando `waitingCount() === 0`).
- `id="alerta-fila-vacia"`: Mensaje de bloqueo **"No hay turnos pendientes en este momento."** cuando la fila queda vacía.

---

## 5. Guía de Ejecución

### Requisitos previos
1. **Backend encendido** en `http://localhost:8080`:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. **Frontend encendido** en `http://localhost:4200`:
   ```bash
   cd client
   npm install
   npm start
   ```

### Comandos para correr las pruebas

#### Ejecutar las 10 pruebas completas (Backend API + Frontend E2E en Chrome)
```bash
cd pruebas
mvn test
```

#### Ejecutar únicamente las 5 pruebas de API del Backend
```bash
cd pruebas
mvn test -Dtest=BackendTurnosApiTest
```

#### Ejecutar únicamente las 5 pruebas E2E del Frontend en Google Chrome
```bash
cd pruebas
mvn test -Dtest=FilaVirtualTurnosE2ETest
```

#### Personalizar el tiempo de visualización en pantalla (opcional)
Por defecto cada paso se muestra durante **mínimo 3 segundos (`3000 ms`)**. Si deseas aumentar el tiempo en pantalla (por ejemplo a 5 segundos por paso):
```bash
mvn test -Dtest=FilaVirtualTurnosE2ETest -Dpausa.ms=5000
```
