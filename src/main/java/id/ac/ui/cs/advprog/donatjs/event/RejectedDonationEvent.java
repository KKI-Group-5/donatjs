package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import org.springframework.context.ApplicationEvent;

public class RejectedDonationEvent extends ApplicationEvent {

    private final DonationResponse donation;

    public RejectedDonationEvent(Object source, DonationResponse donation) {
        super(source);
        this.donation = donation;
    }

    public DonationResponse getDonation() {
        return donation;
    }
}