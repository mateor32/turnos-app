package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object para la Vista Cliente (/clientes -> /clientes/turno).
 * Encapsula la interacción con el portal de autoservicio donde el cliente solicita su turno
 * y visualiza su código asignado (ej. A-011).
 */
public class ClientePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;
    private final long pausaVisualMs;

    private static final By BTN_PEDIR_TURNO = By.id("btn-pedir-turno");
    private static final By TICKET_NUMERO_ASIGNADO = By.id("ticket-numero-asignado");

    public ClientePage(WebDriver driver, WebDriverWait wait, String baseUrl, long pausaVisualMs) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
        this.pausaVisualMs = pausaVisualMs;
    }

    /**
     * Navega al portal de acceso de clientes (/clientes) y espera a que el botón
     * para solicitar turno esté visible.
     */
    public void navegar() {
        driver.get(baseUrl + "/clientes");
        wait.until(ExpectedConditions.visibilityOfElementLocated(BTN_PEDIR_TURNO));
    }

    /**
     * Limpia el sessionStorage para simular un nuevo cliente, hace clic en "Solicitar turno"
     * y mantiene el ticket generado en pantalla durante al menos el tiempo de pausa visual (3s).
     */
    public void pedirTurno() {
        if (driver.findElements(BTN_PEDIR_TURNO).isEmpty()) {
            navegar();
        }
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript("window.sessionStorage.removeItem('turnoId');");
        }
        wait.until(ExpectedConditions.elementToBeClickable(BTN_PEDIR_TURNO)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(TICKET_NUMERO_ASIGNADO));
        pausaVisual();
    }

    /**
     * Extrae el número entero del código de turno asignado en pantalla (ej. "A-011" -> 11).
     */
    public int obtenerNumeroTurnoAsignado() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TICKET_NUMERO_ASIGNADO))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }

    private void pausaVisual() {
        try {
            Thread.sleep(pausaVisualMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
