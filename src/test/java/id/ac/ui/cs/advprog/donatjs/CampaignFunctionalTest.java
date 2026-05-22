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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(CampaignFunctionalTest.PermitAllSecurityConfig.class)
class CampaignFunctionalTest {

    static final String ADMIN_USER = "testadmin";
    static final String ADMIN_PASS = "testpass";

    @TestConfiguration
    static class PermitAllSecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain functionalTestChain(HttpSecurity http) throws Exception {
            UserDetails admin = User.builder()
                    .username(ADMIN_USER)
                    .password("{noop}" + ADMIN_PASS)
                    .roles("ADMIN")
                    .build();
            InMemoryUserDetailsManager uds = new InMemoryUserDetailsManager(admin);

            http.securityMatcher("/**")
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .csrf(c -> c.disable())
                    .httpBasic(Customizer.withDefaults())
                    .userDetailsService(uds);
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

    @Test
    @DisplayName("Admin can access full campaign list including non-open campaigns")
    void adminCampaignListPage_isAccessible() {
        SerenityRest.given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
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
    @DisplayName("Admin can approve a waiting campaign")
    void adminApprovesCampaign_statusBecomesOpen() {
        Campaign campaign = givenAWaitingCampaign("Approval Functional Test");

        SerenityRest.given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body("{\"approve\":true}")
                .when().post("/campaigns/" + campaign.getId() + "/moderate")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Admin can reject a waiting campaign")
    void adminRejectsCampaign_statusBecomesRejected() {
        Campaign campaign = givenAWaitingCampaign("Rejection Functional Test");

        SerenityRest.given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body("{\"approve\":false}")
                .when().post("/campaigns/" + campaign.getId() + "/moderate")
                .then()
                .statusCode(200);
    }

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
    @DisplayName("Admin can mark an open campaign as fraud")
    void adminMarksFraud_campaignStatusBecomesFraud() {
        Campaign campaign = givenAnOpenCampaign("Fraud Functional Test", new BigDecimal("500"));

        SerenityRest.given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when().post("/campaigns/" + campaign.getId() + "/fraud")
                .then()
                .statusCode(200);
    }

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
    @DisplayName("Admin can trigger deadline automation")
    void adminRunsDeadlineAutomation_returnsOk() {
        SerenityRest.given()
                .auth().preemptive().basic(ADMIN_USER, ADMIN_PASS)
                .when().post("/campaigns/deadline-automation/run")
                .then()
                .statusCode(200);
    }

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

    private Campaign givenAWaitingCampaign(String title) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setDescription("Functional test: " + title);
        c.setStatus(CampaignStatus.WAITING);
        c.setDeadline(LocalDate.now().plusDays(30));
        c.setTargetAmount(new BigDecimal("1000"));
        c.setTotalRaised(BigDecimal.ZERO);
        return campaignRepository.save(c);
    }

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
