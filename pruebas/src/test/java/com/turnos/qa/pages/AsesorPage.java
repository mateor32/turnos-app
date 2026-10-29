package com.turnos.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page Object para la Vista Asesor (/asesor).
 * Encapsula los selectores, métricas en tiempo real y acciones de llamado de turnos.
 */
public class AsesorPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    // Selectores requeridos en Vista Asesor
    private static final By BTN_LLAMAR_SIGUIENTE = By.id("btn-llamar-siguiente");
    private static final By TURNO_ACTUAL_LLAMADO = By.id("turno-actual-llamado");
    private static final By CONTADOR_TURNOS_ESPERA = By.id("contador-turnos-espera");
    private static final By TIEMPO_ESPERA_PROMEDIO = By.id("tiempo-espera-promedio");
    private static final By ALERTA_FILA_VACIA = By.id("alerta-fila-vacia");

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    public AsesorPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    /**
     * Navega hacia la ruta /asesor y espera a que el panel cargue sus elementos principales.
     */
    public void navegar() {
        driver.get(baseUrl + "/asesor");
        wait.until(ExpectedConditions.presenceOfElementLocated(BTN_LLAMAR_SIGUIENTE));
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA));
    }

    /**
     * Llama al siguiente turno en la cola esperando que el turno mostrado en pantalla
     * se actualice respecto al valor previo.
     */
    public void llamarSiguienteTurno() {
        String valorPrevio = obtenerTextoTurnoActualLlamadoSiExiste();
        WebElement boton = wait.until(ExpectedConditions.elementToBeClickable(BTN_LLAMAR_SIGUIENTE));
        boton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(TURNO_ACTUAL_LLAMADO));
        wait.until(d -> {
            String actual = d.findElement(TURNO_ACTUAL_LLAMADO).getText().trim();
            return !actual.isEmpty() && !actual.equals(valorPrevio);
        });
    }

    /**
     * Intenta hacer clic en el botón "Llamar Siguiente" cuando la fila está vacía
     * (en caso de que el botón no esté deshabilitado y dispare una alerta visual).
     */
    public void intentarClickLlamarSiguiente() {
        WebElement boton = wait.until(ExpectedConditions.presenceOfElementLocated(BTN_LLAMAR_SIGUIENTE));
        if (boton.isEnabled()) {
            boton.click();
        }
    }

    /**
     * Obtiene el número entero del turno actual llamado en pantalla.
     */
    public int obtenerNumeroTurnoActualLlamado() {
        WebElement elemento = wait.until(ExpectedConditions.visibilityOfElementLocated(TURNO_ACTUAL_LLAMADO));
        String texto = elemento.getText().trim();
        Matcher matcher = NUMBER_PATTERN.matcher(texto);
        if (matcher.find()) {
            return (int) Double.parseDouble(matcher.group());
        }
        throw new IllegalStateException("No se pudo extraer un número de turno válido desde 'turno-actual-llamado': " + texto);
    }

    /**
     * Obtiene el valor entero del contador de personas en espera.
     */
    public int obtenerContadorTurnosEnEspera() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(CONTADOR_TURNOS_ESPERA));
        wait.until(d -> {
            String t = d.findElement(CONTADOR_TURNOS_ESPERA).getText().trim();
            return !t.isEmpty() && NUMBER_PATTERN.matcher(t).find();
        });
        String texto = driver.findElement(CONTADOR_TURNOS_ESPERA).getText().trim();
        Matcher matcher = NUMBER_PATTERN.matcher(texto);
        if (matcher.find()) {
            return (int) Double.parseDouble(matcher.group());
        }
        throw new IllegalStateException("El campo 'contador-turnos-espera' no contiene un número válido: " + texto);
    }

    /**
     * Espera explícitamente a que el contador de turnos en espera alcance un valor esperado.
     */
    public void esperarContadorTurnosEnEsperaIgualA(int valorEsperado) {
        wait.until(d -> obtenerContadorTurnosEnEspera() == valorEsperado);
    }

    /**
     * Obtiene el valor numérico (double) del tiempo de espera promedio calculado por el backend.
     * Soporta formatos como "12 min", "8.5 minutos", "05:30" (toma el primer bloque numérico) o "12".
     */
    public double obtenerTiempoEsperaPromedio() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(TIEMPO_ESPERA_PROMEDIO));
        wait.until(d -> {
            String t = d.findElement(TIEMPO_ESPERA_PROMEDIO).getText().trim();
            return !t.isEmpty() && NUMBER_PATTERN.matcher(t).find();
        });
        String texto = driver.findElement(TIEMPO_ESPERA_PROMEDIO).getText().trim();
        Matcher matcher = NUMBER_PATTERN.matcher(texto);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        throw new IllegalStateException("El campo 'tiempo-espera-promedio' no contiene un valor numérico válido: " + texto);
    }

    /**
     * Obtiene el texto crudo del tiempo de espera promedio.
     */
    public String obtenerTextoTiempoEsperaPromedio() {
        WebElement elemento = wait.until(ExpectedConditions.visibilityOfElementLocated(TIEMPO_ESPERA_PROMEDIO));
        return elemento.getText().trim();
    }

    /**
     * Verifica si el botón "Llamar Siguiente" se encuentra deshabilitado
     * (mediante atributo disabled, aria-disabled="true" o isEnabled() == false).
     */
    public boolean isBotonLlamarSiguienteDeshabilitado() {
        WebElement boton = wait.until(ExpectedConditions.presenceOfElementLocated(BTN_LLAMAR_SIGUIENTE));
        String ariaDisabled = boton.getAttribute("aria-disabled");
        String disabledAttr = boton.getAttribute("disabled");
        return !boton.isEnabled()
                || "true".equalsIgnoreCase(ariaDisabled)
                || disabledAttr != null;
    }

    /**
     * Verifica si la alerta de fila vacía ('alerta-fila-vacia') es visible en pantalla.
     */
    public boolean isAlertaFilaVaciaVisible() {
        try {
            WebElement alerta = wait.until(ExpectedConditions.visibilityOfElementLocated(ALERTA_FILA_VACIA));
            return alerta.isDisplayed() && !alerta.getText().trim().isEmpty();
        } catch (TimeoutException e) {
            return false;
        }
    }

    private String obtenerTextoTurnoActualLlamadoSiExiste() {
        try {
            WebElement el = driver.findElement(TURNO_ACTUAL_LLAMADO);
            return el.isDisplayed() ? el.getText().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}

