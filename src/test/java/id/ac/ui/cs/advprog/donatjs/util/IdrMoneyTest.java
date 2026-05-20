package id.ac.ui.cs.advprog.donatjs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdrMoneyTest {

    @Test
    void testWholeRupiahDoublePrimitive() {
        assertEquals(50000L, IdrMoney.wholeRupiah(50000.4));
        assertEquals(50001L, IdrMoney.wholeRupiah(50000.5));
    }

    @Test
    void testWholeRupiahDoubleObject() {
        assertEquals(0L, IdrMoney.wholeRupiah((Double) null));
        assertEquals(50000L, IdrMoney.wholeRupiah(Double.valueOf(50000.4)));
    }

    @Test
    void testWholeRupiahInvalidDouble() {
        assertThrows(IllegalArgumentException.class, () -> IdrMoney.wholeRupiah(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> IdrMoney.wholeRupiah(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> IdrMoney.wholeRupiah(Double.NEGATIVE_INFINITY));
    }

    @Test
    void testAsDouble() {
        assertEquals(50000.0, IdrMoney.asDouble(50000L), 0.001);
    }
}
