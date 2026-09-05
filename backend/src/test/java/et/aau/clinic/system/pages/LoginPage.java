package et.aau.clinic.system.pages;

import org.openqa.selenium.WebDriver;

/**
 * Page Object for /login. Every locator here reads a stable id from
 * login.html - no raw driver.findElement calls are allowed in the
 * test classes themselves (CLAUDE.md), only inside page objects.
 */
public class LoginPage extends AbstractPage {

    public LoginPage(WebDriver driver, String baseUrl) {
        super(driver, baseUrl);
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
        return find("login-error").getText();
    }

    private void submit(String username, String password) {
        find("username").sendKeys(username);
        find("password").sendKeys(password);
        find("login-submit").click();
    }
}
