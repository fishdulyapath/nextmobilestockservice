package MobileStockServiceApi;

public class ProductInfoSearchTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String[] terms = MobileStockServiceAPI.productInfoSearchTerms(" ซี   3 สี ");
        check(terms.length == 3, "Expected three search terms");
        check("ซี".equals(terms[0]) && "3".equals(terms[1]) && "สี".equals(terms[2]),
                "Search terms should preserve words and ignore extra spaces");
        check(MobileStockServiceAPI.productInfoSearchTerms(" ").length == 0,
                "Blank search should return no terms");

        String clause = MobileStockServiceAPI.productInfoItemsSearchClause(terms.length);
        for (String field : new String[]{"i.code", "i.name_1", "i.name_2",
                "i.name_eng_1", "i.name_eng_2", "b.barcode"}) {
            check(clause.contains(field + " ILIKE ?"), "Missing search field: " + field);
        }
        check(clause.split("\\?", -1).length - 1 == 18,
                "Expected six parameters for each search term");
        check(clause.split("AND \\(i.code ILIKE", -1).length - 1 == 3,
                "Every word should be required in a separate condition");
        check(MobileStockServiceAPI.productInfoItemsSearchClause(0).isEmpty(),
                "Blank search should not filter results");
        System.out.println("Product info search tests passed");
    }
}
