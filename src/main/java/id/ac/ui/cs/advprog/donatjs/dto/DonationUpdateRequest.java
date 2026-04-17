package id.ac.ui.cs.advprog.donatjs.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DonationUpdateRequest {
    @NotNull
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
