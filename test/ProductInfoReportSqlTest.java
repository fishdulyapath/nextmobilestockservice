package MobileStockServiceApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;

/** Executes production SQL with read-only CTE fixtures, then checks real data independently. */
public class ProductInfoReportSqlTest {
    private static final String FIXTURES = "WITH "
            + "erp_branch_list(code,name_1) AS (VALUES ('A','Alpha'),('B','Beta'),('EMPTY','Empty')), "
            + "ic_warehouse(code,name_1,branch_code) AS (VALUES ('W1','First','A'),('W2','Second','B')), "
            + "ic_trans(doc_no,trans_flag,last_status,cust_code) AS (VALUES "
            + "('prev',44,0,'C1'),('prev',44,0,'C1'),('current',44,0,'C1'),('other',44,0,'C2'),"
            + "('cancel',44,1,'C1'),('detailcancel',44,0,'C1'),('set',44,0,'C1'),"
            + "('early',44,0,'C1'),('future',44,0,'C1'),('unknown',44,0,'C1'),('blank',44,0,'C1')), "
            + "ic_trans_detail(doc_no,trans_flag,last_status,set_ref_line,item_code,branch_code,wh_code,doc_date,qty,stand_value,divide_value) AS (VALUES "
            + "('prev',44,0,'','ITEM','A','W1',DATE '2024-02-01',2::numeric,6::numeric,2::numeric),"
            + "('prev',44,0,'','ITEM','A','W1',DATE '2024-02-29',1,0,0),"
            + "('current',44,0,'','ITEM','B','W2',DATE '2024-03-01',4,6,2),"
            + "('other',44,0,'','ITEM','B','W2',DATE '2024-03-01',8,1,1),"
            + "('cancel',44,0,'','ITEM','A','W1',DATE '2024-03-01',100,1,1),"
            + "('detailcancel',44,1,'','ITEM','A','W1',DATE '2024-03-01',100,1,1),"
            + "('set',44,0,'parent','ITEM','A','W1',DATE '2024-03-01',100,1,1),"
            + "('early',44,0,'','ITEM','A','W1',DATE '2024-01-31',100,1,1),"
            + "('future',44,0,'','ITEM','A','W1',DATE '2024-03-02',100,1,1),"
            + "('unknown',44,0,'','ITEM','DELETED','OLD',DATE '2024-03-01',3,1,1),"
            + "('blank',44,0,'','ITEM',NULL,NULL,DATE '2024-03-01',2,1,1),"
            + "('blank',44,0,'','ITEM','','',DATE '2024-03-01',1,1,1)), "
            + "stock_fixture(warehouse,ic_unit_code,balance_qty) AS (VALUES "
            + "('W1','STD',24::numeric),('W1','STD',-4),('W2','STD',40),('W1','OTHER',999))";

    static void equal(Object actual, String expected, String message) {
        if (new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected)) != 0)
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    static JSONObject fixture(Connection real, String customer, String ratio) throws Exception {
        Connection conn = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        args[0] = args[0].toString().replace("WITH p AS", FIXTURES + ", p AS")
                                .replace("sml_ic_function_stock_balance_warehouse_location(p.as_of_date,p.item_code,w.code,'')",
                                        "(SELECT * FROM stock_fixture f WHERE f.warehouse=w.code)");
                    }
                    try { return method.invoke(real, args); }
                    catch (InvocationTargetException ex) { throw ex.getCause(); }
                });
        JSONObject result = new JSONObject();
        ProductInfoReport.populate(conn, result, "ITEM", "STD", new BigDecimal(ratio), customer,
                LocalDate.of(2024, 3, 1));
        reconcile(result);
        return result;
    }

    static void reconcile(JSONObject report) {
        JSONArray branches = report.getJSONArray("branches");
        for (String key : new String[]{"previous_sales_qty", "current_sales_qty", "stock_qty"}) {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < branches.length(); i++) {
                JSONObject branch = branches.getJSONObject(i);
                JSONArray warehouses = branch.getJSONArray("warehouses");
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < warehouses.length(); j++)
                    sum = sum.add(new BigDecimal(warehouses.getJSONObject(j).get(key).toString()));
                equal(branch.get(key), sum.toString(), "Warehouse/branch " + key);
                total = total.add(sum);
            }
            equal(report.getJSONObject("totals").get(key), total.toString(), "Branch/total " + key);
        }
    }

    static void live(Connection conn) throws Exception {
        LocalDate asOf = ProductInfoReport.today(java.time.Clock.systemUTC());
        String customer = "OR-99985";
        JSONObject all = new JSONObject(), filtered = new JSONObject(), alternate = new JSONObject();
        ProductInfoReport.populate(conn, all, "01-0004", "\u0e0a\u0e34\u0e49\u0e19", BigDecimal.ONE, "", asOf);
        ProductInfoReport.populate(conn, filtered, "01-0004", "\u0e0a\u0e34\u0e49\u0e19", BigDecimal.ONE, customer, asOf);
        ProductInfoReport.populate(conn, alternate, "01-0004", "\u0e0a\u0e34\u0e49\u0e19", new BigDecimal("24"), customer, asOf);
        reconcile(all); reconcile(filtered); reconcile(alternate);
        equal(all.getJSONObject("totals").get("stock_qty"), filtered.getJSONObject("totals").get("stock_qty").toString(),
                "Customer must not filter stock");
        String independent = "SELECT COALESCE(SUM(d.qty*COALESCE(NULLIF(d.stand_value,0),1)/COALESCE(NULLIF(d.divide_value,0),1)),0) "
                + "FROM ic_trans_detail d WHERE d.item_code='01-0004' AND d.trans_flag=44 AND COALESCE(d.last_status,0)=0 "
                + "AND COALESCE(d.set_ref_line,'')='' AND d.doc_date>='%s' AND d.doc_date<'%s' "
                + "AND (d.doc_no,d.trans_flag) IN (SELECT h.doc_no,h.trans_flag FROM ic_trans h "
                + "WHERE COALESCE(h.last_status,0)=0 AND h.cust_code='OR-99985')";
        String[] keys = {"previous_sales_qty", "current_sales_qty"};
        LocalDate start = asOf.withDayOfMonth(1);
        for (int i = 0; i < 2; i++) {
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(String.format(independent,
                    i == 0 ? start.minusMonths(1) : start, i == 0 ? start : asOf.plusDays(1)))) {
                rs.next(); equal(filtered.getJSONObject("totals").get(keys[i]), rs.getString(1), "Independent live sales " + keys[i]);
            }
        }
        for (String key : new String[]{"previous_sales_qty", "current_sales_qty", "stock_qty"}) {
            BigDecimal expected = new BigDecimal(filtered.getJSONObject("totals").get(key).toString());
            BigDecimal actual = new BigDecimal(alternate.getJSONObject("totals").get(key).toString()).multiply(new BigDecimal("24"));
            if (expected.subtract(actual).abs().compareTo(new BigDecimal("0.00000001")) > 0)
                throw new AssertionError("Alternate unit conversion: " + key);
        }
        String movementSql = "SELECT COALESCE(SUM(d.calc_flag*d.qty*(d.stand_value/d.divide_value)),0) "
                + "FROM ic_trans_detail d JOIN ic_inventory i ON i.code=d.item_code "
                + "WHERE d.item_code='01-0004' AND d.last_status=0 AND d.item_type<>5 AND i.item_type<>1 "
                + "AND d.doc_date_calc<=DATE '" + asOf + "' AND d.wh_code IN (SELECT code FROM ic_warehouse) AND ("
                + "d.trans_flag IN (70,54,60,58,310,12,56,68,72,44) OR d.trans_flag=66 AND d.qty<>0 "
                + "OR d.trans_flag=14 AND d.inquiry_type=0 OR d.trans_flag=48 AND d.inquiry_type<2 "
                + "OR d.trans_flag IN (46,16) AND d.inquiry_type IN (0,2) OR d.trans_flag=311 AND d.inquiry_type=0)";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(movementSql)) {
            rs.next();
            equal(filtered.getJSONObject("totals").get("stock_qty"), rs.getString(1),
                    "Empirical stock function result equals base-unit movement sum");
            System.out.println("Independent stock movement base quantity=" + rs.getString(1));
        }
        System.out.println("LIVE_ALL=" + all);
        System.out.println("LIVE_CUSTOMER=" + filtered);
        System.out.println("Live sales, stock customer independence, ratio-24 and hierarchy checks passed");
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = ProductInfoReportLiveTest.connect(args[0], args[1])) {
            JSONObject c1 = fixture(conn, "C1", "2");
            equal(c1.getJSONObject("totals").get("previous_sales_qty"), "3.5", "Previous calendar month, duplicate header and zero ratio fallback");
            equal(c1.getJSONObject("totals").get("current_sales_qty"), "9", "Today, cancelled/set exclusions and unassigned sales");
            equal(c1.getJSONObject("totals").get("stock_qty"), "30", "Non-1 standard ratio converts base stock exactly once");
            if (c1.getJSONArray("branches").length() != 5) throw new AssertionError("Dynamic, empty, unknown and blank branches");
            for (int i = 0; i < c1.getJSONArray("branches").length(); i++) {
                JSONObject branch = c1.getJSONArray("branches").getJSONObject(i);
                String code = branch.getString("branch_code");
                if (code.isEmpty() || code.equals("DELETED")) {
                    String expectedBranch = code.isEmpty() ? "\u0e44\u0e21\u0e48\u0e23\u0e30\u0e1a\u0e38\u0e2a\u0e32\u0e02\u0e32" : "\u0e44\u0e21\u0e48\u0e1e\u0e1a\u0e2a\u0e32\u0e02\u0e32: DELETED";
                    String expectedWarehouse = code.isEmpty() ? "\u0e44\u0e21\u0e48\u0e23\u0e30\u0e1a\u0e38\u0e04\u0e25\u0e31\u0e07" : "\u0e44\u0e21\u0e48\u0e1e\u0e1a\u0e04\u0e25\u0e31\u0e07: OLD";
                    if (!branch.getString("branch_name").equals(expectedBranch))
                        throw new AssertionError("Thai fallback branch label");
                    if (!branch.getJSONArray("warehouses").getJSONObject(0).getString("warehouse_name").equals(expectedWarehouse))
                        throw new AssertionError("Thai fallback warehouse label");
                }
            }
            JSONObject all = fixture(conn, "", "2");
            equal(all.getJSONObject("totals").get("current_sales_qty"), "13", "All customers");
            equal(all.getJSONObject("totals").get("stock_qty"), "30", "Stock ignores customer");
            JSONObject absent = fixture(conn, "C' OR 1=1 --", "2");
            equal(absent.getJSONObject("totals").get("current_sales_qty"), "0", "Customer is bound, not SQL interpolation");
            equal(absent.getJSONObject("totals").get("stock_qty"), "30", "Absent customer still has stock");
            System.out.println("Read-only PostgreSQL fixture tests passed");
            live(conn);
        }
    }
}
