package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for /login. Every locator here reads a stable id from
 * login.html - no raw driver.findElement calls are allowed in the
 * test classes themselves (CLAUDE.md), only inside page objects.
 */
public class LoginPage {

    private final WebDriver driver;
    private final String baseUrl;

    public LoginPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
    }

    public LoginPage open() {
        driver.get(baseUrl + "/login");
        return this;
    }

    public SlotsPage loginAs(String username, String password) {
        submit(username, password);
        return new SlotsPage(driver, baseUrl);
    }

    public void submitInvalidLogin(String username, String password) {
        submit(username, password);
    }

    public String getErrorMessage() {
        return driver.findElement(By.id("login-error")).getText();
    }

    private void submit(String username, String password) {
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("login-submit")).click();
    }
}
