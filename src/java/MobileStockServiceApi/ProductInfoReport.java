package MobileStockServiceApi;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

final class ProductInfoReport {
    private ProductInfoReport() { }

    static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZoneId.of("Asia/Bangkok")));
    }

    static final String SQL =
            "WITH p AS (SELECT CAST(? AS date) AS previous_start, CAST(? AS date) AS current_start, "
            + "CAST(? AS date) AS end_exclusive, CAST(? AS date) AS as_of_date, "
            + "CAST(? AS text) AS item_code, CAST(? AS text) AS unit_code, "
            + "CAST(? AS numeric) AS ratio, CAST(? AS text) AS cust_code), "
            + "sales_raw AS (SELECT d.branch_code,d.wh_code, "
            + "SUM(CASE WHEN d.doc_date<p.current_start THEN d.qty*COALESCE(NULLIF(d.stand_value,0),1)"
            + "/COALESCE(NULLIF(d.divide_value,0),1) ELSE 0 END) AS previous_base, "
            + "SUM(CASE WHEN d.doc_date>=p.current_start THEN d.qty*COALESCE(NULLIF(d.stand_value,0),1)"
            + "/COALESCE(NULLIF(d.divide_value,0),1) ELSE 0 END) AS current_base "
            + "FROM ic_trans_detail d CROSS JOIN p WHERE d.item_code=p.item_code AND d.trans_flag=44 "
            + "AND COALESCE(d.last_status,0)=0 AND COALESCE(d.set_ref_line,'')='' "
            + "AND d.doc_date>=p.previous_start AND d.doc_date<p.end_exclusive "
            // ERP uniquely keys headers by (doc_no,trans_flag); legacy detail/header branches can differ.
            + "AND EXISTS (SELECT 1 FROM ic_trans h WHERE h.doc_no=d.doc_no AND h.trans_flag=d.trans_flag "
            + "AND COALESCE(h.last_status,0)=0 AND (p.cust_code='' OR h.cust_code=p.cust_code)) "
            + "GROUP BY d.branch_code,d.wh_code), "
            + "sales AS (SELECT COALESCE(branch_code,'') AS branch_code,COALESCE(wh_code,'') AS wh_code, "
            + "SUM(previous_base) AS previous_base,SUM(current_base) AS current_base FROM sales_raw "
            + "GROUP BY COALESCE(branch_code,''),COALESCE(wh_code,'')), "
            + "warehouses AS (SELECT code,MAX(name_1) AS name_1,COALESCE(branch_code,'') AS branch_code "
            + "FROM ic_warehouse GROUP BY code,COALESCE(branch_code,'')), "
            + "stock AS (SELECT w.branch_code,w.code,SUM(COALESCE(s.balance_qty,0)) AS stock_base "
            + "FROM warehouses w CROSS JOIN p LEFT JOIN LATERAL "
            + "sml_ic_function_stock_balance_warehouse_location(p.as_of_date,p.item_code,w.code,'') s "
            + "ON s.ic_unit_code=p.unit_code AND s.warehouse=w.code GROUP BY w.branch_code,w.code), "
            // Union sales keys so blank, deleted and reassigned master records cannot hide sales.
            + "keys AS (SELECT branch_code,code AS wh_code FROM warehouses UNION SELECT branch_code,wh_code FROM sales), "
            + "branches AS (SELECT code,MAX(name_1) AS name_1 FROM erp_branch_list GROUP BY code), "
            + "branch_keys AS (SELECT code FROM branches UNION SELECT branch_code FROM keys) "
            + "SELECT bk.code AS branch_code,COALESCE(b.name_1,CASE WHEN bk.code='' THEN '\u0e44\u0e21\u0e48\u0e23\u0e30\u0e1a\u0e38\u0e2a\u0e32\u0e02\u0e32' "
            + "ELSE '\u0e44\u0e21\u0e48\u0e1e\u0e1a\u0e2a\u0e32\u0e02\u0e32: '||bk.code END) AS branch_name,k.wh_code AS warehouse_code, "
            + "COALESCE(w.name_1,CASE WHEN k.wh_code='' THEN '\u0e44\u0e21\u0e48\u0e23\u0e30\u0e1a\u0e38\u0e04\u0e25\u0e31\u0e07' "
            + "ELSE '\u0e44\u0e21\u0e48\u0e1e\u0e1a\u0e04\u0e25\u0e31\u0e07: '||k.wh_code END) AS warehouse_name, "
            + "COALESCE(s.previous_base,0)/p.ratio AS previous_sales_qty, "
            + "COALESCE(s.current_base,0)/p.ratio AS current_sales_qty, "
            + "COALESCE(st.stock_base,0)/p.ratio AS stock_qty "
            + "FROM branch_keys bk CROSS JOIN p LEFT JOIN branches b ON b.code=bk.code "
            + "LEFT JOIN keys k ON k.branch_code=bk.code "
            // Warehouse codes are globally unique; retain the name even when legacy sales use another branch.
            + "LEFT JOIN warehouses w ON w.code=k.wh_code "
            + "LEFT JOIN sales s ON s.branch_code=k.branch_code AND s.wh_code=k.wh_code "
            + "LEFT JOIN stock st ON st.branch_code=k.branch_code AND st.code=k.wh_code "
            + "ORDER BY bk.code,k.wh_code";

    static void populate(Connection conn, JSONObject report, String itemCode, String standardUnit,
            BigDecimal ratio, String customer, LocalDate asOf) throws SQLException {
        try (PreparedStatement branchMode = conn.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM erp_branch_list WHERE COALESCE(code,'') NOT IN ('','0000'))")) {
            try (ResultSet mode = branchMode.executeQuery()) {
                mode.next();
                report.put("has_branches", mode.getBoolean(1));
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
            LocalDate currentStart = asOf.withDayOfMonth(1);
            stmt.setDate(1, java.sql.Date.valueOf(currentStart.minusMonths(1)));
            stmt.setDate(2, java.sql.Date.valueOf(currentStart));
            stmt.setDate(3, java.sql.Date.valueOf(asOf.plusDays(1)));
            stmt.setDate(4, java.sql.Date.valueOf(asOf));
            stmt.setString(5, itemCode);
            stmt.setString(6, standardUnit);
            stmt.setBigDecimal(7, ratio);
            stmt.setString(8, customer);
            try (ResultSet rows = stmt.executeQuery()) { assemble(report, rows); }
        }
    }

    private static final String[] QUANTITIES = {"previous_sales_qty", "current_sales_qty", "stock_qty"};

    private static JSONObject zeroQuantities() {
        JSONObject row = new JSONObject();
        for (String key : QUANTITIES) row.put(key, BigDecimal.ZERO);
        return row;
    }

    static void assemble(JSONObject report, ResultSet rows) throws SQLException {
        Map<String, JSONObject> branches = new LinkedHashMap<>();
        JSONObject totals = zeroQuantities();
        while (rows.next()) {
            String code = rows.getString("branch_code");
            JSONObject branch = branches.get(code);
            if (branch == null) {
                branch = zeroQuantities().put("branch_code", code)
                        .put("branch_name", rows.getString("branch_name")).put("warehouses", new JSONArray());
                branches.put(code, branch);
            }
            if (rows.getString("warehouse_code") == null) continue;
            JSONObject warehouse = new JSONObject().put("warehouse_code", rows.getString("warehouse_code"))
                    .put("warehouse_name", rows.getString("warehouse_name"));
            for (String key : QUANTITIES) {
                BigDecimal quantity = rows.getBigDecimal(key);
                warehouse.put(key, quantity);
                branch.put(key, new BigDecimal(branch.get(key).toString()).add(quantity));
                totals.put(key, new BigDecimal(totals.get(key).toString()).add(quantity));
            }
            branch.getJSONArray("warehouses").put(warehouse);
        }
        report.put("branches", new JSONArray(branches.values())).put("totals", totals);
    }
}
