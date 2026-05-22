package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingCampaignWalletGatewayTest {

    private final LoggingCampaignWalletGateway gateway = new LoggingCampaignWalletGateway();

    private Campaign buildCampaign(Long id) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setTitle("Test Campaign");
        c.setDescription("desc");
        c.setDeadline(LocalDate.now().plusDays(10));
        c.setTargetAmount(new BigDecimal("1000000"));
        c.setTotalRaised(new BigDecimal("500000"));
        c.setStatus(CampaignStatus.OPEN);
        return c;
    }

    @Test
    void requestPayout_doesNotThrow() {
        Campaign campaign = buildCampaign(1L);
        assertThatCode(() -> gateway.requestPayout(campaign)).doesNotThrowAnyException();
    }

    @Test
    void requestRefund_doesNotThrow() {
        Campaign campaign = buildCampaign(2L);
        assertThatCode(() -> gateway.requestRefund(campaign)).doesNotThrowAnyException();
    }

    @Test
    void requestPayout_withNullId_doesNotThrow() {
        Campaign campaign = buildCampaign(null);
        assertThatCode(() -> gateway.requestPayout(campaign)).doesNotThrowAnyException();
    }
}
