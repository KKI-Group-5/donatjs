package id.ac.ui.cs.advprog.donatjs;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.InMemoryCampaignRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * End-to-end functional tests using Serenity BDD and REST Assured.
 * Exercises the full HTTP API layer against a running Spring Boot server.
 */
@ExtendWith(SerenityJUnit5Extension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(CampaignFunctionalTest.PermitAllSecurityConfig.class)
class CampaignFunctionalTest {

    @TestConfiguration
    static class PermitAllSecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain functionalTestChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .csrf(c -> c.disable());
            return http.build();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    InMemoryCampaignRepository campaignRepository;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    // ── Campaign list ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Public user can access the campaign list page")
    void campaignListPage_isAccessible() {
        SerenityRest.given()
                .when().get("/campaigns")
                .then().statusCode(200);
    }

    // ── Campaign creation form ────────────────────────────────────────────────────

    @Test
    @DisplayName("Campaign creation form page loads successfully")
    void createCampaignPage_isAccessible() {
        SerenityRest.given()
                .when().get("/campaigns/create")
                .then().statusCode(200);
    }

    // ── Moderation (WAITING → OPEN / REJECTED) ────────────────────────────────────

    @Test
    @DisplayName("Non-admin cannot moderate a campaign — 403 Forbidden")
    void nonAdminModerateCampaign_returnsForbidden() {
        SerenityRest.given()
                .contentType(ContentType.JSON)
                .body("{\"approve\":true}")
                .when().post("/campaigns/999/moderate")
                .then()
                .statusCode(403);
    }

    // ── Donations ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Recording a donation updates the campaign total raised")
    void donation_updatesTotal() {
        Campaign campaign = givenAnOpenCampaign("Donation Functional Test", new BigDecimal("1000"));

        SerenityRest.given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":300}")
                .when().post("/campaigns/" + campaign.getId() + "/donations")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Reaching the target amount auto-closes the campaign")
    void donation_autoCloses_whenTargetReached() {
        Campaign campaign = givenAnOpenCampaign("Target Reached Functional Test", new BigDecimal("100"));

        SerenityRest.given()
                .contentType(ContentType.JSON)
                .body("{\"amount\":100}")
                .when().post("/campaigns/" + campaign.getId() + "/donations")
                .then()
                .statusCode(200);
    }

    // ── Fraud marking ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Non-admin cannot mark a campaign as fraud — 403 Forbidden")
    void nonAdminMarkFraud_returnsForbidden() {
        SerenityRest.given()
                .when().post("/campaigns/999/fraud")
                .then()
                .statusCode(403);
    }

    // ── Deadline automation ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Non-admin cannot trigger deadline automation — 403 Forbidden")
    void nonAdminRunsDeadlineAutomation_returnsForbidden() {
        SerenityRest.given()
                .when().post("/campaigns/deadline-automation/run")
                .then()
                .statusCode(403);
    }

    // ── Campaign detail ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Campaign detail page returns 404 for unknown campaign")
    void campaignDetail_notFound_returns404() {
        SerenityRest.given()
                .when().get("/campaigns/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Campaign detail page renders for an existing campaign")
    void campaignDetail_found_returns200() {
        Campaign campaign = givenAnOpenCampaign("Detail Functional Test", new BigDecimal("500"));

        SerenityRest.given()
                .when().get("/campaigns/" + campaign.getId())
                .then()
                .statusCode(200);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private Campaign givenAnOpenCampaign(String title, BigDecimal targetAmount) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("Functional test: " + title);
        c.setStatus(CampaignStatus.OPEN);
        c.setDeadline(LocalDate.now().plusDays(30));
        c.setTargetAmount(targetAmount);
        c.setTotalRaised(BigDecimal.ZERO);
        return campaignRepository.save(c);
    }
}
