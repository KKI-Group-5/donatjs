package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;

public interface CampaignWalletGateway {
    void requestPayout(Campaign campaign);

    void requestRefund(Campaign campaign);
}
