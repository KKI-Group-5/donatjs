package id.ac.ui.cs.advprog.donatjs.model;

import java.util.EnumMap;
import java.util.Map;

public final class PaymentFee {

    public static final long FEE_WALLET  =     0L;
    public static final long FEE_BANK    = 1_500L;
    public static final long FEE_EWALLET = 2_000L;

    private static final Map<Donation.PaymentMethod, Long> FEE_MAP =
            new EnumMap<>(Donation.PaymentMethod.class);

    static {
        FEE_MAP.put(Donation.PaymentMethod.WALLET,        FEE_WALLET);

        FEE_MAP.put(Donation.PaymentMethod.BANK_TRANSFER, FEE_BANK);
        FEE_MAP.put(Donation.PaymentMethod.BANK_BCA,      FEE_BANK);
        FEE_MAP.put(Donation.PaymentMethod.BANK_MANDIRI,  FEE_BANK);
        FEE_MAP.put(Donation.PaymentMethod.BANK_BNI,      FEE_BANK);
        FEE_MAP.put(Donation.PaymentMethod.BANK_BRI,      FEE_BANK);

        FEE_MAP.put(Donation.PaymentMethod.GOPAY,         FEE_EWALLET);
        FEE_MAP.put(Donation.PaymentMethod.OVO,           FEE_EWALLET);
        FEE_MAP.put(Donation.PaymentMethod.DANA,          FEE_EWALLET);
        FEE_MAP.put(Donation.PaymentMethod.SHOPEEPAY,     FEE_EWALLET);
        FEE_MAP.put(Donation.PaymentMethod.LINKAJA,       FEE_EWALLET);
    }

    private PaymentFee() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static long calculateFee(Donation.PaymentMethod method) {
        Long fee = FEE_MAP.get(method);
        if (fee == null) {
            throw new IllegalArgumentException(
                    "No fee mapping found for payment method: " + method);
        }
        return fee;
    }

    public static long calculateTotal(long amount, Donation.PaymentMethod method) {
        return amount + calculateFee(method);
    }
}