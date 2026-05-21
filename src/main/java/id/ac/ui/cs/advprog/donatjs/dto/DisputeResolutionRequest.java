package id.ac.ui.cs.advprog.donatjs.dto;

public class DisputeResolutionRequest {
    private boolean approve;
    private String adminNotes;

    public DisputeResolutionRequest() {
        // Default constructor required by JPA/Jackson
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
