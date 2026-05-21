package id.ac.ui.cs.advprog.donatjs.util;

/**
 * Indonesian Rupiah amounts are whole numbers. All wallet math uses {@code long}
 * rupiah internally so we never persist float noise (e.g. 49999.999999 vs 50000).
 */
public final class IdrMoney {

    private IdrMoney() {
    }

    public static long wholeRupiah(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Invalid amount.");
        }
        return Math.round(amount);
    }

    public static long wholeRupiah(Double amount) {
        if (amount == null) {
            return 0L;
        }
        return wholeRupiah(amount.doubleValue());
    }

    public static double asDouble(long wholeRupiah) {
        return (double) wholeRupiah;
    }
}
