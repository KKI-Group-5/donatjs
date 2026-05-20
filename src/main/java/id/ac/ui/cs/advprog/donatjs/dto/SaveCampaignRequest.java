package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveCampaignRequest {

    private String userId;
    private String campaignId;
    private String campaignTitle;
    private String campaignOrganizer;
    private String campaignImageUrl;
}
