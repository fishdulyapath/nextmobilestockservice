import MobileStockServiceApi.MobileStockServiceAPI;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class ProductInfoPriceParityTest {
    private static void check(Method method, String base, String formula, String expected) throws Exception {
        BigDecimal actual = (BigDecimal) method.invoke(new MobileStockServiceAPI(), new BigDecimal(base), formula);
        if (actual.compareTo(new BigDecimal(expected)) != 0) {
            throw new AssertionError(formula + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) throws Exception {
        Method method = MobileStockServiceAPI.class.getDeclaredMethod(
                "productInfoFormulaPrice", BigDecimal.class, String.class);
        method.setAccessible(true);
        check(method, "92", "=100,5", "100");
        check(method, "92", "+10,5", "102");
        check(method, "92", "-10,5", "82");
        check(method, "92", "=100", "100");
        check(method, "92", "", "92");
        System.out.println("Product info formula parity tests passed");
    }
}
