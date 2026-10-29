# Pruebas Automatizadas - Fila Virtual de Turnos

Este directorio contiene las pruebas automatizadas con **JUnit 5** basadas directamente en los requerimientos del caso, dividiendo los escenarios en **Caminos Felices (Casos Exitosos)** y **Caminos Tristes (Casos Fallidos / Bloqueos)**:

1. **[`BackendTurnosApiTest.java`](src/test/java/com/turnos/qa/tests/BackendTurnosApiTest.java)**: Pruebas directas sobre los endpoints del **Backend** (`http://localhost:8080/api/turnos`). **Se pueden ejecutar desde ya sin necesidad de tener el Frontend listo.**
2. **[`FilaVirtualTurnosE2ETest.java`](src/test/java/com/turnos/qa/tests/FilaVirtualTurnosE2ETest.java)**: Pruebas End-to-End en Google Chrome con **Selenium WebDriver 4** para validar el **Frontend** (`http://localhost:4200`) cuando esté listo.

---

## 1. ¿En qué se basan las pruebas? (El Caso)

- **FRONT (`http://localhost:4200`)**: Una vista para pedir turno (`/cliente`) y otra para que el asesor llame al siguiente (`/asesor`), con los turnos en espera y la espera promedio.
- **BACK (`http://localhost:8080/api/turnos`)**: Un servicio que asigna los turnos, lleva el orden de la fila y calcula la espera promedio.
- **DATOS**: Los turnos de la mañana cargados en la base de datos (cerrando en el turno 10, con los turnos **6, 7, 8, 9 y 10** pendientes en orden de llegada).
- **PRUEBAS**: Pruebas que demuestren que el orden se respeta en todos los casos.
- **REGLA DEL CASO**:
  > *"Los turnos se atienden en orden de llegada, los números no se repiten y no se puede llamar al siguiente si la fila está vacía."*

---

## 2. Pruebas del Backend sin Frontend ([`BackendTurnosApiTest.java`](src/test/java/com/turnos/qa/tests/BackendTurnosApiTest.java))

Permiten validar que el backend cumpla todas las reglas del caso consumiendo directamente `http://localhost:8080/api/turnos`:

### Caminos Felices (Casos Exitosos)
1. **`test01_atencionEnOrdenDeLlegadaFIFO()`**: Llama a `GET /siguiente` y `PUT /{id}/atender` comprobando que los turnos pendientes se entregan y atienden en su estricto orden de llegada.
2. **`test02_crearTurno_asignaNumeroConsecutivo_yAumentaFila()`**: Llama a `POST /api/turnos` (`201 Created`), verifica que nazca en estado `pendiente` y que `GET /pendientes/count` aumente en `+1`.
3. **`test03_verificarMetricasBackend()`**: Comprueba que `GET /pendientes/count` y `GET /espera-promedio` respondan `200 OK` con valores numéricos válidos (`>= 0`).

### Caminos Tristes (Casos Fallidos / de Bloqueo)
4. **`test04_bloqueoCuandoFilaEstaVacia_yTurnoNoPendiente()`**:
   - Vacía los turnos pendientes hasta llegar a `0`.
   - Comprueba que `GET /api/turnos/siguiente` bloquee la llamada devolviendo **`404 Not Found`**.
   - Comprueba que intentar volver a atender un turno ya atendido o un ID inexistente (`PUT /{id}/atender`) falle con **`400 Bad Request`**.
5. **`test05_noRepetirNumeros_niSaltarseElOrden()`**:
   - Crea dos turnos seguidos con `POST /api/turnos`, verifica que sus números sean distintos y consecutivos, y comprueba con `GET /siguiente` que el backend **nunca se salte la fila** llamando al más reciente antes que al primero.

---

## 3. Pruebas E2E con Frontend ([`FilaVirtualTurnosE2ETest.java`](src/test/java/com/turnos/qa/tests/FilaVirtualTurnosE2ETest.java))

Cuando el frontend en Angular esté listo en `http://localhost:4200`, validan en Google Chrome los mismos **Caminos Felices** (`test01` a `test03`) y **Caminos Tristes** (`test04` y `test05`) usando los siguientes `id` HTML:

### Vista Cliente (`/cliente`)
- `id="btn-pedir-turno"`: Botón para pedir un turno.
- `id="ticket-numero-asignado"`: Número de turno generado.

### Vista Asesor (`/asesor`)
- `id="btn-llamar-siguiente"`: Botón para llamar al siguiente turno.
- `id="turno-actual-llamado"`: Número del turno llamado en pantalla.
- `id="contador-turnos-espera"`: Cantidad de turnos pendientes en la fila.
- `id="tiempo-espera-promedio"`: Tiempo de espera promedio.
- `id="alerta-fila-vacia"`: Alerta mostrada si se intenta llamar cuando la fila está vacía.

---

## 4. Cómo ejecutar las pruebas

### Opción A: Probar SOLO el Backend ahora mismo (sin Frontend)
1. Enciende el backend en una terminal:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. En otra terminal, ejecuta únicamente las pruebas del backend:
   ```bash
   cd pruebas
   mvn test -Dtest=BackendTurnosApiTest
   ```

### Opción B: Probar el Frontend + Backend en Google Chrome (E2E)
Cuando el frontend esté corriendo en `http://localhost:4200` y el backend en `http://localhost:8080`:
```bash
cd pruebas
mvn test -Dtest=FilaVirtualTurnosE2ETest
```
