package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object para la Vista Cliente (/cliente).
 * Encapsula los selectores y acciones disponibles para solicitar un turno.
 */
public class ClientePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    // Selectores requeridos en Vista Cliente
    private static final By BTN_PEDIR_TURNO = By.id("btn-pedir-turno");
    private static final By TICKET_NUMERO_ASIGNADO = By.id("ticket-numero-asignado");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    public ClientePage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    /**
     * Navega hacia la ruta /cliente y espera a que el botón de pedir turno sea visible.
     */
    public void navegar() {
        driver.get(baseUrl + "/cliente");
        wait.until(ExpectedConditions.visibilityOfElementLocated(BTN_PEDIR_TURNO));
    }

    /**
     * Hace clic en el botón "Pedir Turno" usando espera explícita de clicabilidad.
     */
    public void solicitarNuevoTurno() {
        WebElement boton = wait.until(ExpectedConditions.elementToBeClickable(BTN_PEDIR_TURNO));
        boton.click();
    }

    /**
     * Obtiene el texto crudo del número de turno asignado tras solicitarlo.
     * Espera explícitamente a que el elemento sea visible y tenga contenido no vacío.
     */
    public String obtenerTextoTurnoAsignado() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(TICKET_NUMERO_ASIGNADO));
        wait.until(d -> {
            String text = d.findElement(TICKET_NUMERO_ASIGNADO).getText().trim();
            return !text.isEmpty() && NUMBER_PATTERN.matcher(text).find();
        });
        return driver.findElement(TICKET_NUMERO_ASIGNADO).getText().trim();
    }

    /**
     * Extrae el valor entero del turno asignado (ej. "Turno #11" o "11" -> 11).
     */
    public int obtenerNumeroTurnoAsignado() {
        String texto = obtenerTextoTurnoAsignado();
        Matcher matcher = NUMBER_PATTERN.matcher(texto);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        throw new IllegalStateException("No se encontró un valor numérico válido en 'ticket-numero-asignado': " + texto);
    }
}

