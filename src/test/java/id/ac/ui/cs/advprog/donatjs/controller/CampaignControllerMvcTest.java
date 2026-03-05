package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CampaignController.class)
class CampaignControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;

    @Test
    void postCreate_withBlankRequiredFields_returnsCreateViewWithErrors() throws Exception {
        mockMvc.perform(post("/campaigns/create")
                        .param("title", "")
                        .param("description", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/create"))
                .andExpect(model().attributeHasFieldErrors("campaign", "title", "description"));
    }

    @Test
    void getCampaigns_returnsOnlyOpenCampaignsInModel() throws Exception {
        Campaign open1 = new Campaign();
        open1.setId(1L);
        open1.setTitle("Open 1");
        open1.setDescription("Desc 1");
        open1.setDeadline(LocalDate.now().plusDays(10));
        open1.setTargetAmount(new BigDecimal("1000"));
        open1.setTotalRaised(BigDecimal.ZERO);
        open1.setStatus(CampaignStatus.OPEN);

        Campaign open2 = new Campaign();
        open2.setId(2L);
        open2.setTitle("Open 2");
        open2.setDescription("Desc 2");
        open2.setDeadline(LocalDate.now().plusDays(20));
        open2.setTargetAmount(new BigDecimal("2000"));
        open2.setTotalRaised(BigDecimal.ZERO);
        open2.setStatus(CampaignStatus.OPEN);

        Mockito.when(campaignService.findOpenCampaigns())
                .thenReturn(List.of(open1, open2));

        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isOk())
                .andExpect(view().name("campaigns/list"))
                .andExpect(model().attribute("campaigns", notNullValue()))
                .andExpect(model().attribute("campaigns",
                        everyItem(hasProperty("status", is(CampaignStatus.OPEN)))));
    }

    @Test
    void getCampaign_notFound_returns404() throws Exception {
        Long missingId = 999999L;
        Mockito.when(campaignService.findById(missingId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/campaigns/" + missingId))
                .andExpect(status().isNotFound());
    }
}

