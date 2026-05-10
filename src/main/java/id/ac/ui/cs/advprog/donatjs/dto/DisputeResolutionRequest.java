package id.ac.ui.cs.advprog.donatjs.dto;

public class DisputeResolutionRequest {
    private boolean approve;
    private String adminNotes;

    public DisputeResolutionRequest() {
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
