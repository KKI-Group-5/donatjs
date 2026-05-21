package id.ac.ui.cs.advprog.donatjs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PaymentFeeTest {

    // ── WALLET ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WALLET has zero fee")
    void wallet_feeIsZero() {
        assertEquals(0L, PaymentFee.calculateFee(Donation.PaymentMethod.WALLET));
    }

    @Test
    @DisplayName("WALLET total equals amount with no added fee")
    void wallet_totalEqualsAmount() {
        assertEquals(100_000L, PaymentFee.calculateTotal(100_000L, Donation.PaymentMethod.WALLET));
    }

    // ── Bank methods — Rp 1,500 ───────────────────────────────────────────────

    @Test
    @DisplayName("BANK_TRANSFER fee is Rp 1,500")
    void bankTransfer_feeIs1500() {
        assertEquals(PaymentFee.FEE_BANK,
                PaymentFee.calculateFee(Donation.PaymentMethod.BANK_TRANSFER));
    }

    @Test
    @DisplayName("BANK_BCA fee is Rp 1,500")
    void bankBca_feeIs1500() {
        assertEquals(PaymentFee.FEE_BANK,
                PaymentFee.calculateFee(Donation.PaymentMethod.BANK_BCA));
    }

    @Test
    @DisplayName("BANK_MANDIRI fee is Rp 1,500")
    void bankMandiri_feeIs1500() {
        assertEquals(PaymentFee.FEE_BANK,
                PaymentFee.calculateFee(Donation.PaymentMethod.BANK_MANDIRI));
    }

    @Test
    @DisplayName("BANK_BNI fee is Rp 1,500")
    void bankBni_feeIs1500() {
        assertEquals(PaymentFee.FEE_BANK,
                PaymentFee.calculateFee(Donation.PaymentMethod.BANK_BNI));
    }

    @Test
    @DisplayName("BANK_BRI fee is Rp 1,500")
    void bankBri_feeIs1500() {
        assertEquals(PaymentFee.FEE_BANK,
                PaymentFee.calculateFee(Donation.PaymentMethod.BANK_BRI));
    }

    @Test
    @DisplayName("Bank total = amount + Rp 1,500")
    void bank_totalIncludesFee() {
        assertEquals(151_500L,
                PaymentFee.calculateTotal(150_000L, Donation.PaymentMethod.BANK_BCA));
    }

    // ── E-wallet methods — Rp 2,000 ───────────────────────────────────────────

    @Test
    @DisplayName("GOPAY fee is Rp 2,000")
    void gopay_feeIs2000() {
        assertEquals(PaymentFee.FEE_EWALLET,
                PaymentFee.calculateFee(Donation.PaymentMethod.GOPAY));
    }

    @Test
    @DisplayName("OVO fee is Rp 2,000")
    void ovo_feeIs2000() {
        assertEquals(PaymentFee.FEE_EWALLET,
                PaymentFee.calculateFee(Donation.PaymentMethod.OVO));
    }

    @Test
    @DisplayName("DANA fee is Rp 2,000")
    void dana_feeIs2000() {
        assertEquals(PaymentFee.FEE_EWALLET,
                PaymentFee.calculateFee(Donation.PaymentMethod.DANA));
    }

    @Test
    @DisplayName("SHOPEEPAY fee is Rp 2,000")
    void shopeepay_feeIs2000() {
        assertEquals(PaymentFee.FEE_EWALLET,
                PaymentFee.calculateFee(Donation.PaymentMethod.SHOPEEPAY));
    }

    @Test
    @DisplayName("LINKAJA fee is Rp 2,000")
    void linkaja_feeIs2000() {
        assertEquals(PaymentFee.FEE_EWALLET,
                PaymentFee.calculateFee(Donation.PaymentMethod.LINKAJA));
    }

    @Test
    @DisplayName("E-wallet total = amount + Rp 2,000")
    void ewallet_totalIncludesFee() {
        assertEquals(152_000L,
                PaymentFee.calculateTotal(150_000L, Donation.PaymentMethod.GOPAY));
    }
}