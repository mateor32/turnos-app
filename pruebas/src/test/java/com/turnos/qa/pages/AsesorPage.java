package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page Object para el Panel del Asesor (/administradores/panel o /asesor).
 * Permite consultar las métricas de la fila (turnos en espera y espera promedio),
 * llamar al siguiente turno en orden de llegada y verificar el bloqueo cuando la fila está vacía.
 */
public class AsesorPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;
    private final long pausaVisualMs;

    private static final By BTN_LLAMAR_SIGUIENTE = By.id("btn-llamar-siguiente");
    private static final By TURNO_ACTUAL_LLAMADO = By.id("turno-actual-llamado");
    private static final By CONTADOR_TURNOS_ESPERA = By.id("contador-turnos-espera");
    private static final By TIEMPO_ESPERA_PROMEDIO = By.id("tiempo-espera-promedio");
    private static final By ALERTA_FILA_VACIA = By.id("alerta-fila-vacia");

    public AsesorPage(WebDriver driver, WebDriverWait wait, String baseUrl, long pausaVisualMs) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
        this.pausaVisualMs = pausaVisualMs;
    }

    /**
     * Abre el panel del asesor (/administradores/panel) y espera a que carguen los indicadores.
     */
    public void navegar() {
        driver.get(baseUrl + "/administradores/panel");
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA));
        pausaVisual();
    }

    /**
     * Hace clic en "Llamar siguiente turno", espera a que se actualice en pantalla el nuevo
     * turno en atención y el contador de espera, y mantiene visible el resultado durante al menos 3 segundos.
     */
    public void llamarSiguiente() {
        int esperaAntes = obtenerTurnosEnEspera();
        List<WebElement> turnoActualElements = driver.findElements(TURNO_ACTUAL_LLAMADO);
        String textoAnterior = turnoActualElements.isEmpty() ? "" : turnoActualElements.get(0).getText().trim();

        wait.until(ExpectedConditions.elementToBeClickable(BTN_LLAMAR_SIGUIENTE)).click();

        if (esperaAntes > 0) {
            wait.until(d -> {
                List<WebElement> els = d.findElements(TURNO_ACTUAL_LLAMADO);
                if (els.isEmpty() || !els.get(0).isDisplayed()) {
                    return false;
                }
                String actual = els.get(0).getText().trim();
                return !actual.isEmpty() && !actual.equals(textoAnterior);
            });
            wait.until(d -> obtenerTurnosEnEspera() < esperaAntes);
            pausaVisual();
        }
    }

    /**
     * Devuelve el número entero del turno actualmente en atención (ej. "A-006" -> 6).
     */
    public int obtenerTurnoActualLlamado() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TURNO_ACTUAL_LLAMADO))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }

    /**
     * Devuelve la cantidad de personas/turnos pendientes en la fila.
     */
    public int obtenerTurnosEnEspera() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }

    /**
     * Devuelve el tiempo promedio de espera mostrado en el panel (en minutos).
     */
    public double obtenerTiempoEsperaPromedio() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TIEMPO_ESPERA_PROMEDIO))
                .getText()
                .replace(',', '.')
                .replaceAll("[^0-9.]", "");
        return Double.parseDouble(texto);
    }

    /**
     * Indica si el botón "Llamar siguiente turno" se encuentra deshabilitado.
     */
    public boolean isBotonLlamarDeshabilitado() {
        WebElement boton = wait.until(ExpectedConditions.presenceOfElementLocated(BTN_LLAMAR_SIGUIENTE));
        return !boton.isEnabled() || boton.getAttribute("disabled") != null;
    }

    /**
     * Indica si el aviso de fila vacía ("No hay turnos pendientes en este momento.") está visible.
     */
    public boolean isAlertaFilaVaciaVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(ALERTA_FILA_VACIA)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void pausaVisual() {
        try {
            Thread.sleep(pausaVisualMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
