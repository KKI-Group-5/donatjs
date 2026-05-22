package id.ac.ui.cs.advprog.donatjs.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.donatjs.repository.VerificationTokenRepository;
import id.ac.ui.cs.advprog.donatjs.model.VerificationToken;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthAndProfileFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    private WebDriver driver;
    private String baseUrl;

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        this.driver = new ChromeDriver(options);
        this.baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testRegistrationAndLoginFlow() {
        // Phase 1: Register
        driver.get(baseUrl + "/register");
        
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        
        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("name")));
        WebElement emailInput = driver.findElement(By.id("email"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement dobInput = driver.findElement(By.id("dateOfBirth"));
        WebElement bioInput = driver.findElement(By.id("bio"));
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));

        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";

        nameInput.sendKeys("Test User");
        emailInput.sendKeys(testEmail);
        passwordInput.sendKeys("password123");
        dobInput.sendKeys("2000-01-01");
        bioInput.sendKeys("This is a test bio");
        submitButton.click();

        // Assuming registration redirects to login page
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));

        // Fetch token from DB
        AppUser user = userRepository.findByEmail(testEmail).orElseThrow();
        VerificationToken token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();

        // Verify email via URL
        driver.get(baseUrl + "/api/auth/verify?token=" + token.getToken());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // Go back to login explicitly if needed
        driver.get(baseUrl + "/login");

        // Phase 2: Login
        WebElement loginEmailInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement loginPasswordInput = driver.findElement(By.id("password"));
        WebElement loginSubmitButton = driver.findElement(By.cssSelector("button[type='submit']"));

        loginEmailInput.sendKeys(testEmail);
        loginPasswordInput.sendKeys("password123");
        loginSubmitButton.click();

        // Assuming login redirects to a dashboard or profile or at least not login
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    @Test
    public void testUnauthenticatedUserCannotAccessProfile() {
        driver.get(baseUrl + "/profile");
        
        // Unauthenticated users should be redirected to login
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/login"));
        
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }
}
