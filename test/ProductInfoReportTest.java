package MobileStockServiceApi;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Types;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.json.JSONObject;

public class ProductInfoReportTest {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        boolean customerParameter = false;
        for (Method method : MobileStockServiceAPI.class.getMethods()) {
            if (method.getName().equals("getProductInfoReport") && method.getParameterCount() == 7)
                customerParameter = true;
        }
        check(customerParameter, "Report must accept optional custcode while retaining month");
        Class<?> report = Class.forName("MobileStockServiceApi.ProductInfoReport");
        Method today = report.getDeclaredMethod("today", Clock.class);
        check(today.invoke(null, Clock.fixed(Instant.parse("2025-12-31T17:01:00Z"), ZoneOffset.UTC))
                .toString().equals("2026-01-01"), "Report date must use Bangkok, including year rollover");
        check(today.invoke(null, Clock.fixed(Instant.parse("2024-02-29T16:59:59Z"), ZoneOffset.UTC))
                .toString().equals("2024-02-29"), "Leap day must not advance before Bangkok midnight");
        check(today.invoke(null, Clock.fixed(Instant.parse("2024-02-29T17:00:00Z"), ZoneOffset.UTC))
                .toString().equals("2024-03-01"), "Bangkok midnight must advance the report date");
        CachedRowSet rows = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl meta = new RowSetMetaDataImpl();
        String[] columns = {"branch_code", "branch_name", "warehouse_code", "warehouse_name",
            "previous_sales_qty", "current_sales_qty", "stock_qty"};
        meta.setColumnCount(columns.length);
        for (int i = 0; i < columns.length; i++) {
            meta.setColumnName(i + 1, columns[i]);
            meta.setColumnType(i + 1, i < 4 ? Types.VARCHAR : Types.NUMERIC);
        }
        rows.setMetaData(meta);
        Object[][] values = {
            {"B1", "One", "W1", "First", "1.25", "2", "-3"},
            {"B1", "One", "W2", "Second", "2.75", "3", "7"},
            {"", "Unassigned branch", "", "Unassigned warehouse", "4", "5", "0"},
            {"B2", "Empty", null, null, "0", "0", "0"}
        };
        for (Object[] value : values) {
            rows.moveToInsertRow();
            for (int i = 0; i < value.length; i++) {
                if (value[i] == null) rows.updateNull(i + 1);
                else if (i < 4) rows.updateString(i + 1, value[i].toString());
                else rows.updateBigDecimal(i + 1, new BigDecimal(value[i].toString()));
            }
            rows.insertRow(); rows.moveToCurrentRow();
        }
        rows.beforeFirst();
        JSONObject result = new JSONObject();
        ProductInfoReport.assemble(result, rows);
        check(result.getJSONArray("branches").length() == 3, "Keep empty and unassigned branches");
        check(result.getJSONObject("totals").getDouble("previous_sales_qty") == 8, "Previous total");
        check(result.getJSONObject("totals").getDouble("current_sales_qty") == 10, "Current total");
        check(result.getJSONObject("totals").getDouble("stock_qty") == 4, "Signed stock total");
        for (int i = 0; i < result.getJSONArray("branches").length(); i++) {
            JSONObject branch = result.getJSONArray("branches").getJSONObject(i);
            if (branch.getString("branch_code").equals("B2"))
                check(branch.getJSONArray("warehouses").length() == 0, "No fake warehouse for empty branch");
            for (String key : new String[]{"previous_sales_qty", "current_sales_qty", "stock_qty"}) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < branch.getJSONArray("warehouses").length(); j++)
                    sum = sum.add(new BigDecimal(branch.getJSONArray("warehouses").getJSONObject(j).get(key).toString()));
                check(sum.compareTo(new BigDecimal(branch.get(key).toString())) == 0, "Warehouse/branch reconciliation");
            }
        }
        System.out.println("ProductInfoReportTest passed");
    }
}
