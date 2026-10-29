package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object simple para la Vista Asesor (/asesor).
 */
public class AsesorPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    private static final By BTN_LLAMAR_SIGUIENTE = By.id("btn-llamar-siguiente");
    private static final By TURNO_ACTUAL_LLAMADO = By.id("turno-actual-llamado");
    private static final By CONTADOR_TURNOS_ESPERA = By.id("contador-turnos-espera");
    private static final By TIEMPO_ESPERA_PROMEDIO = By.id("tiempo-espera-promedio");
    private static final By ALERTA_FILA_VACIA = By.id("alerta-fila-vacia");

    public AsesorPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    public void navegar() {
        driver.get(baseUrl + "/asesor");
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA));
    }

    public void llamarSiguiente() {
        wait.until(ExpectedConditions.elementToBeClickable(BTN_LLAMAR_SIGUIENTE)).click();
    }

    public int obtenerTurnoActualLlamado() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TURNO_ACTUAL_LLAMADO))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }

    public int obtenerTurnosEnEspera() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }

    public double obtenerTiempoEsperaPromedio() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TIEMPO_ESPERA_PROMEDIO))
                .getText()
                .replace(',', '.')
                .replaceAll("[^0-9.]", "");
        return Double.parseDouble(texto);
    }

    public boolean isBotonLlamarDeshabilitado() {
        WebElement boton = wait.until(ExpectedConditions.presenceOfElementLocated(BTN_LLAMAR_SIGUIENTE));
        return !boton.isEnabled() || boton.getAttribute("disabled") != null;
    }

    public boolean isAlertaFilaVaciaVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(ALERTA_FILA_VACIA)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
}
