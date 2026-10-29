package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object simple para la Vista Cliente (/cliente).
 */
public class ClientePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    private static final By BTN_PEDIR_TURNO = By.id("btn-pedir-turno");
    private static final By TICKET_NUMERO_ASIGNADO = By.id("ticket-numero-asignado");

    public ClientePage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    public void navegar() {
        driver.get(baseUrl + "/cliente");
        wait.until(ExpectedConditions.visibilityOfElementLocated(BTN_PEDIR_TURNO));
    }

    public void pedirTurno() {
        wait.until(ExpectedConditions.elementToBeClickable(BTN_PEDIR_TURNO)).click();
    }

    public int obtenerNumeroTurnoAsignado() {
        String texto = wait.until(ExpectedConditions.visibilityOfElementLocated(TICKET_NUMERO_ASIGNADO))
                .getText()
                .replaceAll("[^0-9]", "");
        return Integer.parseInt(texto);
    }
}
