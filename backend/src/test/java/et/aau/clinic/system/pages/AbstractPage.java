package et.aau.clinic.system.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Common plumbing for every page object: an explicit wait instead of
 * a bare driver.findElement(). DEF-003 - a Jenkins run of the exact
 * same journey that passes reliably in GitHub Actions failed with a
 * NoSuchElementException right after a form submit, because the
 * container gives Chrome and the app server less CPU than a hosted
 * runner does. WebDriver blocks on click() until navigation *starts*,
 * not until the target page has finished rendering, so a slow server
 * response was enough to lose the race. Waiting for the element we
 * actually need removes the assumption that rendering finishes
 * instantly, everywhere, without slowing down the fast case at all.
 */
abstract class AbstractPage {

    protected final WebDriver driver;
    protected final String baseUrl;
    private final WebDriverWait wait;

    protected AbstractPage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected WebElement find(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    protected WebElement find(String id) {
        return find(By.id(id));
    }
}
