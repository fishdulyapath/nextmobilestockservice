package MobileStockServiceApi;

import SMLWebService.MyLib;
import SMLWebService.Routine;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils._global;
import utils.myglobal;
import utils._routine;

@Path("v1")
public class MobileStockServiceAPI {

    private String _formatDate(Date date) {
        SimpleDateFormat _format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return _format.format(date);
    }

    private static boolean isNotNull(String txt) {
        return txt != null && txt.trim().length() > 0 ? true : false;
    }

    private void ensureProductInfoPermissionColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS public.msc_permission ("
                    + "user_code varchar(255) PRIMARY KEY,"
                    + "stock_list smallint NOT NULL DEFAULT 0,"
                    + "request_list smallint NOT NULL DEFAULT 0,"
                    + "transfer_list smallint NOT NULL DEFAULT 0,"
                    + "handheld_list smallint NOT NULL DEFAULT 0,"
                    + "barcode_list smallint NOT NULL DEFAULT 0,"
                    + "info_list smallint NOT NULL DEFAULT 0,"
                    + "permission_list smallint NOT NULL DEFAULT 0,"
                    + "product_info_list smallint NOT NULL DEFAULT 0)");
            stmt.executeUpdate("ALTER TABLE msc_permission ADD COLUMN IF NOT EXISTS product_info_list smallint NOT NULL DEFAULT 0");
        }
    }

    private boolean canViewProductInfo(Connection conn, String userCode) throws SQLException {
        if (userCode == null || userCode.trim().isEmpty()) return false;
        ensureProductInfoPermissionColumn(conn);
        String sql = "SELECT 1 FROM erp_user u LEFT JOIN msc_permission p ON upper(p.user_code)=upper(u.code) "
                + "WHERE upper(u.code)=upper(?) AND (upper(u.code)='SUPERADMIN' OR COALESCE(p.product_info_list,0)=1) LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userCode.trim());
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    private void ensurePricePermissionTable(Connection conn) throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS public.msc_price_permission ("
                + "user_code character varying(255) NOT NULL,"
                + "price_0 smallint NOT NULL DEFAULT 0,"
                + "price_1 smallint NOT NULL DEFAULT 0,"
                + "price_2 smallint NOT NULL DEFAULT 0,"
                + "price_3 smallint NOT NULL DEFAULT 0,"
                + "price_4 smallint NOT NULL DEFAULT 0,"
                + "price_5 smallint NOT NULL DEFAULT 0,"
                + "price_6 smallint NOT NULL DEFAULT 0,"
                + "price_7 smallint NOT NULL DEFAULT 0,"
                + "price_8 smallint NOT NULL DEFAULT 0,"
                + "price_9 smallint NOT NULL DEFAULT 0,"
                + "create_datetime timestamp without time zone DEFAULT timezone('Asia/Bangkok'::text, now()),"
                + "CONSTRAINT msc_price_permission_pkey PRIMARY KEY (user_code)"
                + ");";
        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        stmt.executeUpdate(query);
        stmt.close();
    }

    @GET
    @Path("/authentication")
    public Response Authentication(
            @QueryParam("provider_name") String strProvider,
            @QueryParam("database_name") String strDatabaseName,
            @QueryParam("user_code") String strUserCode,
            @QueryParam("password") String strPassword) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {
            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String __strQUERY1 = "SELECT code as user_code, name_1 as user_name FROM erp_user WHERE upper(code)=upper('" + strUserCode + "') AND password='" + strPassword + "' ORDER BY code";

            Statement __stmt1;
            ResultSet __rs1;
            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __rs1 = __stmt1.executeQuery(__strQUERY1);
            JSONArray __jsonArr = new JSONArray();
            while (__rs1.next()) {

                JSONObject obj = new JSONObject();
                obj.put("user_code", __rs1.getString("user_code"));
                obj.put("user_name", __rs1.getString("user_name"));

                __jsonArr.put(obj);
                __objResponse.put("success", true);
            }
            __rs1.close();
            __stmt1.close();

            __objResponse.put("data", __jsonArr);
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getBranchList")
    public Response getBranchList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "select code, name_1 from erp_branch_list order by code";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getSupplier")
    public Response getSupplier(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String where = "";
            if (!strSearch.equals("")) {
                where = " and (code like '%" + strSearch + "%' or name_1 like '%" + strSearch + "%')   ";
            }

            String __strQUERY1 = "select code, name_1 from ap_supplier where 1=1 " + where + " order by code limit 100";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemUnit")
    public Response getItemUnit(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select iuu.code,iu.name_1 from ic_unit_use iuu left join ic_unit iu on iu.code = iuu.code  where ic_code  = '" + strItemCode + "' ";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("unit_code", __rsHead.getString("code"));
                obj.put("unit_name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemPrice")
    public Response getItemPrice(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "SELECT \n"
                    + "    ic_code,\n"
                    + "    unit_code,\n"
                    + "    COALESCE(NULLIF(price_0,''),'0') AS price_0,\n"
                    + "    COALESCE(NULLIF(price_1,''),'0') AS price_1,\n"
                    + "    COALESCE(NULLIF(price_2,''),'0') AS price_2,\n"
                    + "    COALESCE(NULLIF(price_3,''),'0') AS price_3,\n"
                    + "    COALESCE(NULLIF(price_4,''),'0') AS price_4,\n"
                    + "    COALESCE(NULLIF(price_5,''),'0') AS price_5,\n"
                    + "    COALESCE(NULLIF(price_6,''),'0') AS price_6,\n"
                    + "    COALESCE(NULLIF(price_7,''),'0') AS price_7,\n"
                    + "    COALESCE(NULLIF(price_8,''),'0') AS price_8,\n"
                    + "    COALESCE(NULLIF(price_9,''),'0') AS price_9,\n"
                    + "    sale_type "
                    + "FROM ic_inventory_price_formula where ic_code = '" + strItemCode + "' ";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("price_0", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_0"))));
                obj.put("price_1", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_1"))));
                obj.put("price_2", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_2"))));
                obj.put("price_3", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_3"))));
                obj.put("price_4", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_4"))));
                obj.put("price_5", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_5"))));
                obj.put("price_6", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_6"))));
                obj.put("price_7", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_7"))));
                obj.put("price_8", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_8"))));
                obj.put("price_9", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_9"))));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemBarcodePrice")
    public Response getItemBarcodePrice(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select barcode,unit_code,price,price_2,price_3,price_4 from ic_inventory_barcode where ic_code  = '" + strItemCode + "' and status = 0";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("price", String.format("%.2f", Float.parseFloat(__rsHead.getString("price"))));
                obj.put("price_2", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_2"))));
                obj.put("price_3", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_3"))));
                obj.put("price_4", String.format("%.2f", Float.parseFloat(__rsHead.getString("price_4"))));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemPriceStandard")
    public Response getItemPriceStandard(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select unit_code,from_qty,to_qty,from_date,to_date,sale_type,cust_group_1,cust_group_2,price_type,cust_code,sale_price1,sale_price2 from ic_inventory_price where ic_code  = '" + strItemCode + "' and price_mode = 1 and to_date >= now() order by line_number asc";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("from_date", __rsHead.getString("from_date"));
                obj.put("to_date", __rsHead.getString("to_date"));
                obj.put("sale_type", __rsHead.getString("sale_type"));
                obj.put("cust_group_1", __rsHead.getString("cust_group_1"));
                obj.put("cust_group_2", __rsHead.getString("cust_group_2"));
                obj.put("price_type", __rsHead.getString("price_type"));
                obj.put("cust_code", __rsHead.getString("cust_code"));
                obj.put("from_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("from_qty"))));
                obj.put("to_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("to_qty"))));
                obj.put("sale_price1", String.format("%.2f", Float.parseFloat(__rsHead.getString("sale_price1"))));
                obj.put("sale_price2", String.format("%.2f", Float.parseFloat(__rsHead.getString("sale_price2"))));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemPriceNormal")
    public Response getItemPriceNormal(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select unit_code,from_qty,to_qty,from_date,to_date,sale_type,cust_group_1,cust_group_2,price_type,cust_code,sale_price1,sale_price2 from ic_inventory_price where ic_code  = '" + strItemCode + "' and price_mode = 0 and to_date >= now() order by line_number asc";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("from_date", __rsHead.getString("from_date"));
                obj.put("to_date", __rsHead.getString("to_date"));
                obj.put("sale_type", __rsHead.getString("sale_type"));
                obj.put("cust_group_1", __rsHead.getString("cust_group_1"));
                obj.put("cust_group_2", __rsHead.getString("cust_group_2"));
                obj.put("price_type", __rsHead.getString("price_type"));
                obj.put("cust_code", __rsHead.getString("cust_code"));
                obj.put("from_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("from_qty"))));
                obj.put("to_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("to_qty"))));
                obj.put("sale_price1", String.format("%.2f", Float.parseFloat(__rsHead.getString("sale_price1"))));
                obj.put("sale_price2", String.format("%.2f", Float.parseFloat(__rsHead.getString("sale_price2"))));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getStockDetail")
    public Response getStockDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select balance_qty,ic_unit_code,coalesce((select name_1 from ic_unit where ic_unit.code = ic_unit_code),'') as unit_name, \n"
                    + "                coalesce((SELECT ic_warehouse.name_1 FROM ic_warehouse WHERE ic_warehouse.code = sml_ic_function_stock_balance_warehouse_location.warehouse ),'') AS wh_name ,\n"
                    + "                coalesce((SELECT ic_shelf.name_1 FROM ic_shelf WHERE ic_shelf.code = sml_ic_function_stock_balance_warehouse_location.location and whcode = sml_ic_function_stock_balance_warehouse_location.warehouse ),'') AS sh_name \n"
                    + "                from sml_ic_function_stock_balance_warehouse_location('NOW()','" + strItemCode + "', '', '') ";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("shelf_name", __rsHead.getString("sh_name"));
                obj.put("balance_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("balance_qty"))));
                obj.put("unit_code", __rsHead.getString("ic_unit_code"));
                obj.put("unit_name", __rsHead.getString("unit_name"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getAccrued")
    public Response getAccrued(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select ic_code,\n"
                    + "                --ค้างรับ\n"
                    + "                ((select accrued_in_qty from ic_inventory where code=ic_code)/((select unit_standard_stand_value from ic_inventory where code=ic_code)/\n"
                    + "                (select unit_standard_divide_value from ic_inventory where code=ic_code))) as accrued_in_qty, "
                    + "                --ค้างจอง\n"
                    + "                ((select book_out_qty from ic_inventory where code=ic_code)/((select unit_standard_stand_value from ic_inventory where code=ic_code)/\n"
                    + "                (select unit_standard_divide_value from ic_inventory where code=ic_code))) as book_out_qty, "
                    + "                --ค้างส่ง\n"
                    + "                ((select accrued_out_qty from ic_inventory where code=ic_code)/((select unit_standard_stand_value from ic_inventory where code=ic_code)/\n"
                    + "                (select unit_standard_divide_value from ic_inventory where code=ic_code))) as accrued_out_qty "
                    + "                from ic_inventory_barcode where ic_code = '" + strItemCode + "' "
                    + "                group by ic_code limit 1";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("accrued_in_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("accrued_in_qty"))));
                obj.put("book_out_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("book_out_qty"))));
                obj.put("accrued_out_qty", String.format("%.2f", Float.parseFloat(__rsHead.getString("accrued_out_qty"))));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getInventoryMaster")
    public Response getInventoryMaster(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";
            if (strSearch.trim().length() > 0) {
                String[] __fieldList = {"code", "name_1"};
                String[] __keyword = strSearch.trim().split(" ");
                for (int __field = 0; __field < __fieldList.length; __field++) {
                    if (__keyword.length > 0) {
                        if (__where.length() > 0) {
                            __where.append(" or ");
                        } else {
                            __where.append("  ");
                        }
                        __where.append("(");
                        for (int __loop = 0; __loop < __keyword.length; __loop++) {
                            if (__loop > 0) {
                                __where.append(" and ");
                            }
                            __where.append("upper(" + __fieldList[__field]
                                    + ") like \'%"
                                    + __keyword[__loop].toUpperCase() + "%\'");
                        }
                        __where.append(")");
                        _where += " and " + __where.toString();
                    }
                }
            }

            _whereFinal = _where;
            String __strQUERY1 = "select code,name_1,unit_standard from ic_inventory where 1=1 " + _whereFinal + " limit 100";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("item_code", __rsHead.getString("code"));
                obj.put("item_name", __rsHead.getString("name_1"));
                obj.put("unit_standard", __rsHead.getString("unit_standard"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getUnitMaster")
    public Response getUnitMaster(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            _whereFinal = _where;
            String __strQUERY1 = "select iuu.code,ic.name_1,stand_value,divide_value,ratio from ic_unit_use  iuu left join ic_unit ic on ic.code = iuu.code where iuu.ic_code = '" + strItemCode + "' limit 100";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));
                obj.put("stand_value", __rsHead.getString("stand_value"));
                obj.put("divide_value", __rsHead.getString("divide_value"));
                obj.put("ratio", __rsHead.getString("ratio"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getBarcodeMaster")
    public Response getBarcodeMaster(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String strItemCode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            _whereFinal = _where;
            String __strQUERY1 = "select barcode,unit_code,price,price_member from ic_inventory_barcode where ic_code = '" + strItemCode + "' limit 100";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("price", __rsHead.getString("price"));
                obj.put("price_member", __rsHead.getString("price_member"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/checkBarcodeExists")
    public Response checkBarcodeExists(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("barcode") String strBarcode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {
            __objResponse.put("existx", false);
            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            _whereFinal = _where;
            String __strQUERY1 = "select barcode,unit_code,price,price_member from ic_inventory_barcode where barcode = '" + strBarcode + "' limit 100";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            __rsHead.next();
            int row = __rsHead.getRow();
            if (row > 0) {
                __objResponse.put("existx", true);
            } else {
                __objResponse.put("existx", false);
            }
            __objResponse.put("success", true);

            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/createNewBarcode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewBarcode(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String item_code = req.optString("item_code");
            String item_name = req.optString("item_name");
            String barcode = req.optString("barcode");
            String unit_code = req.optString("unit_code");
            String price_nm = req.optString("price");
            String price_member = req.optString("price_member");

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "INSERT INTO ic_inventory_barcode (ic_code, barcode,description, unit_code, price, "
                    + "price_member) "
                    + "VALUES (?, ?, ?, ?, ?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, item_code);
                ps.setString(2, barcode);
                ps.setString(3, item_name);
                ps.setString(4, unit_code);
                ps.setDouble(5, Double.parseDouble(price_nm));
                ps.setDouble(6, Double.parseDouble(price_member));
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/updateBarcode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBarcode(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String barcode = req.optString("barcode");
            String price_nm = req.optString("price");
            String price_member = req.optString("price_member");

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "update ic_inventory_barcode set price=? , price_member=? where barcode = ?";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setDouble(1, Double.parseDouble(price_nm));
                ps.setDouble(2, Double.parseDouble(price_member));
                ps.setString(3, barcode);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @GET
    @Path("/getItemSearch")
    public Response getItemSearch(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";
            if (strSearch.trim().length() > 0) {
                String[] __fieldList = {"ic_code", "description", "barcode"};
                String[] __keyword = strSearch.trim().split(" ");
                for (int __field = 0; __field < __fieldList.length; __field++) {
                    if (__keyword.length > 0) {
                        if (__where.length() > 0) {
                            __where.append(" or ");
                        } else {
                            __where.append("  ");
                        }
                        __where.append("(");
                        for (int __loop = 0; __loop < __keyword.length; __loop++) {
                            if (__loop > 0) {
                                __where.append(" and ");
                            }
                            __where.append("upper(" + __fieldList[__field]
                                    + ") like \'%"
                                    + __keyword[__loop].toUpperCase() + "%\'");
                        }
                        __where.append(")");
                        _where += " and " + __where.toString();
                    }
                }
            }

            _whereFinal = _where;
            String __strQUERY1 = "select ic_code,barcode,description,unit_code from ic_inventory_barcode where 1=1 " + _whereFinal + " limit 100";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("item_name", __rsHead.getString("description"));
                obj.put("unit_code", __rsHead.getString("unit_code"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemList")
    public Response getItemList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";
            if (strSearch.trim().length() > 0) {
                String[] __fieldList = {"ic.code", "ic.name_1", "icb.barcode"};
                String[] __keyword = strSearch.trim().split(" ");
                for (int __field = 0; __field < __fieldList.length; __field++) {
                    if (__keyword.length > 0) {
                        if (__where.length() > 0) {
                            __where.append(" or ");
                        } else {
                            __where.append("  ");
                        }
                        __where.append("(");
                        for (int __loop = 0; __loop < __keyword.length; __loop++) {
                            if (__loop > 0) {
                                __where.append(" and ");
                            }
                            __where.append("upper(" + __fieldList[__field]
                                    + ") like \'%"
                                    + __keyword[__loop].toUpperCase() + "%\'");
                        }
                        __where.append(")");
                        _where += " and " + __where.toString();
                    }
                }
            }

            _whereFinal = _where;
            String __strQUERY1 = "select ic.code,ic.name_1 from ic_inventory ic left join ic_inventory_barcode icb on icb.ic_code = ic.code where 1=1 " + _whereFinal + " group by ic.code,ic.name_1  limit 200";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("item_code", __rsHead.getString("code"));
                obj.put("item_name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCustomerList")
    public Response getCustomerList(@QueryParam("provider") String provider,
            @QueryParam("dbname") String database, @QueryParam("search") String search,
            @QueryParam("usercode") String userCode) {
        try (Connection conn = new _routine()._connect(database.toLowerCase(), _global.FILE_CONFIG(provider));
                PreparedStatement stmt = conn.prepareStatement("SELECT code,name_1 FROM ar_customer WHERE code ILIKE ? OR name_1 ILIKE ? ORDER BY code LIMIT 50")) {
            String term = "%" + (search == null ? "" : search.trim()) + "%";
            if (!canViewProductInfo(conn, userCode)) return Response.status(403).entity("{\"ERROR\":\"Permission denied\"}").build();
            stmt.setString(1, term);
            stmt.setString(2, term);
            JSONArray rows = new JSONArray();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.put(new JSONObject().put("code", rs.getString(1)).put("name", rs.getString(2)));
                }
            }
            return Response.ok(new JSONObject().put("success", true).put("data", rows).toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            return Response.status(400).entity(new JSONObject().put("ERROR", ex.getMessage()).toString()).build();
        }
    }

    static String[] productInfoSearchTerms(String search) {
        if (search == null || search.trim().isEmpty()) return new String[0];
        return search.trim().split("\\s+");
    }

    static String productInfoItemsSearchClause(int termCount) {
        StringBuilder clause = new StringBuilder();
        for (int index = 0; index < termCount; index++) {
            clause.append(" AND (i.code ILIKE ? OR i.name_1 ILIKE ? OR i.name_2 ILIKE ? ")
                    .append("OR i.name_eng_1 ILIKE ? OR i.name_eng_2 ILIKE ? ")
                    .append("OR EXISTS (SELECT 1 FROM ic_inventory_barcode b ")
                    .append("WHERE b.ic_code=i.code AND b.barcode ILIKE ?))");
        }
        return clause.toString();
    }

    @GET
    @Path("/getProductInfoItems")
    public Response getProductInfoItems(@QueryParam("provider") String provider,
            @QueryParam("dbname") String database, @QueryParam("search") String search,
            @QueryParam("usercode") String userCode) {
        String[] terms = productInfoSearchTerms(search);
        String sql = "SELECT i.code,i.name_1,i.unit_standard AS unit_code,"
                + "i.item_category,COALESCE(i.balance_qty,0)/"
                + "COALESCE(NULLIF(i.unit_standard_stand_value,0),1)*"
                + "COALESCE(NULLIF(i.unit_standard_divide_value,0),1) AS stock_qty, "
                + "(SELECT COALESCE(json_agg(json_build_object('unit_code',u.code,'stock_qty',"
                + "COALESCE(i.balance_qty,0)/COALESCE(NULLIF(u.stand_value,0),1)*"
                + "COALESCE(NULLIF(u.divide_value,0),1)) "
                + "ORDER BY CASE WHEN u.code=i.unit_standard THEN 0 ELSE 1 END,u.code),'[]'::json) "
                + "FROM ic_unit_use u WHERE u.ic_code=i.code) AS units "
                + "FROM ic_inventory i WHERE 1=1"
                + productInfoItemsSearchClause(terms.length)
                + " ORDER BY i.code LIMIT 50";
        try (Connection conn = new _routine()._connect(database.toLowerCase(), _global.FILE_CONFIG(provider));
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (!canViewProductInfo(conn, userCode)) return Response.status(403).entity("{\"ERROR\":\"Permission denied\"}").build();
            int parameter = 1;
            for (String term : terms) {
                String pattern = "%" + term + "%";
                for (int index = 0; index < 6; index++) stmt.setString(parameter++, pattern);
            }
            JSONArray rows = new JSONArray();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONArray units = new JSONArray(rs.getString("units"));
                    boolean hasStandard = false;
                    for (int index = 0; index < units.length(); index++) {
                        if (rs.getString("unit_code").equals(units.getJSONObject(index).getString("unit_code"))) {
                            hasStandard = true;
                            break;
                        }
                    }
                    if (!hasStandard) {
                        JSONArray withStandard = new JSONArray();
                        withStandard.put(new JSONObject().put("unit_code", rs.getString("unit_code"))
                                .put("stock_qty", rs.getBigDecimal("stock_qty")));
                        for (int index = 0; index < units.length(); index++) withStandard.put(units.get(index));
                        units = withStandard;
                    }
                    rows.put(new JSONObject().put("item_code", rs.getString("code"))
                            .put("item_name", rs.getString("name_1"))
                            .put("unit_code", rs.getString("unit_code"))
                            .put("item_category", rs.getString("item_category"))
                            .put("stock_qty", rs.getBigDecimal("stock_qty"))
                            .put("units", units));
                }
            }
            return Response.ok(new JSONObject().put("success", true).put("data", rows).toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            return Response.status(400).entity(new JSONObject().put("ERROR", ex.getMessage()).toString()).build();
        }
    }

    @GET
    @Path("/getProductInfoReport")
    public Response getProductInfoReport(@QueryParam("provider") String provider,
            @QueryParam("dbname") String database, @QueryParam("itemcode") String itemCode,
            @QueryParam("month") String month, @QueryParam("unitcode") String unitCode,
            @QueryParam("usercode") String userCode, @QueryParam("custcode") String customerCode) {
        try {
            // Keep accepting month for older clients, but this report always describes today.
            LocalDate asOf = ProductInfoReport.today(java.time.Clock.systemUTC());
            YearMonth selected = YearMonth.from(asOf);
            String customer = customerCode == null ? "" : customerCode.trim();
            if (itemCode == null || itemCode.trim().isEmpty()) {
                return Response.status(400).entity("{\"ERROR\":\"Invalid item\"}").build();
            }
            try (Connection conn = new _routine()._connect(database.toLowerCase(), _global.FILE_CONFIG(provider));
                    PreparedStatement itemStmt = conn.prepareStatement("SELECT name_1,unit_standard,item_category,COALESCE(NULLIF(unit_standard_stand_value,0),1)/COALESCE(NULLIF(unit_standard_divide_value,0),1) AS ratio FROM ic_inventory WHERE code=?")) {
                itemStmt.setString(1, itemCode);
                if (!canViewProductInfo(conn, userCode)) return Response.status(403).entity("{\"ERROR\":\"Permission denied\"}").build();
                try (ResultSet item = itemStmt.executeQuery()) {
                    if (!item.next()) return Response.status(404).entity("{\"ERROR\":\"Item not found\"}").build();
                    String reportUnit = unitCode == null || unitCode.trim().isEmpty()
                            ? item.getString("unit_standard") : unitCode.trim();
                    BigDecimal ratio = item.getBigDecimal("ratio");
                    if (ratio == null || ratio.signum() == 0) ratio = BigDecimal.ONE;
                    if (!reportUnit.equals(item.getString("unit_standard"))) {
                        try (PreparedStatement unitStmt = conn.prepareStatement(
                                "SELECT COALESCE(NULLIF(stand_value,0),1)/COALESCE(NULLIF(divide_value,0),1) AS ratio "
                                + "FROM ic_unit_use WHERE ic_code=? AND code=? LIMIT 1")) {
                            unitStmt.setString(1, itemCode);
                            unitStmt.setString(2, reportUnit);
                            try (ResultSet unit = unitStmt.executeQuery()) {
                                if (!unit.next()) return Response.status(400).entity("{\"ERROR\":\"Invalid unit\"}").build();
                                ratio = unit.getBigDecimal("ratio");
                            }
                        }
                    }
                    JSONObject report = new JSONObject().put("item_code", itemCode)
                            .put("item_name", item.getString("name_1"))
                            .put("unit_code", reportUnit)
                            .put("month", selected.toString())
                            .put("previous_month", selected.minusMonths(1).toString())
                            .put("as_of_date", asOf.toString()).put("cust_code", customer);
                    ProductInfoReport.populate(conn, report, itemCode, item.getString("unit_standard"),
                            ratio, customer, asOf);
                    return Response.ok(new JSONObject().put("success", true).put("data", report).toString(), MediaType.APPLICATION_JSON).build();
                }
            }
        } catch (Exception ex) {
            return Response.status(400).entity(new JSONObject().put("ERROR", ex.getMessage()).toString()).build();
        }
    }

    private BigDecimal productInfoFormulaPrice(BigDecimal base, String formula) {
        if (formula == null || formula.trim().isEmpty()) return base;
        String expression = formula.trim();
        char operator = expression.charAt(0);
        String firstPart = expression.split(",", 2)[0];
        try {
            if (operator == '=') return new BigDecimal(firstPart.substring(1));
            if (operator == '+') return base.add(new BigDecimal(firstPart.substring(1)));
            if (operator == '-') return base.subtract(new BigDecimal(firstPart.substring(1)));
            return new BigDecimal(expression);
        } catch (NumberFormatException ex) {
            return base;
        }
    }

    @GET
    @Path("/getCustomerItemPrice")
    public Response getCustomerItemPrice(@QueryParam("provider") String provider,
            @QueryParam("dbname") String database, @QueryParam("itemcode") String itemCode,
            @QueryParam("unitcode") String unitCode, @QueryParam("custcode") String customerCode,
            @QueryParam("sale_type") Integer saleTypeOverride,
            @QueryParam("vat_type") Integer vatTypeOverride,
            @QueryParam("usercode") String userCode) {
        if (itemCode == null || itemCode.trim().isEmpty() || unitCode == null || unitCode.trim().isEmpty()) {
            return Response.status(400).entity("{\"ERROR\":\"Item and unit are required\"}").build();
        }
        if ((saleTypeOverride != null && (saleTypeOverride < 0 || saleTypeOverride > 2))
                || (vatTypeOverride != null && (vatTypeOverride < 0 || vatTypeOverride > 3))) {
            return Response.status(400).entity("{\"ERROR\":\"Invalid sale or VAT type\"}").build();
        }
        String customer = customerCode == null ? "" : customerCode.trim();
        try (Connection conn = new _routine()._connect(database.toLowerCase(), _global.FILE_CONFIG(provider))) {
            if (!canViewProductInfo(conn, userCode)) return Response.status(403).entity("{\"ERROR\":\"Permission denied\"}").build();
            int vatType = 0;
            int saleType = 0;
            BigDecimal vatRate = null;
            if (!customer.isEmpty()) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COALESCE(inquiry_type,0),COALESCE(vat_type,0),COALESCE(vat_rate,0) "
                        + "FROM pos_basket WHERE cust_code=? ORDER BY basket_id DESC LIMIT 1")) {
                    stmt.setString(1, customer);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            saleType = rs.getInt(1);
                            vatType = rs.getInt(2);
                            vatRate = rs.getBigDecimal(3);
                        }
                    }
                } catch (SQLException ignored) { }
            }
            if (saleTypeOverride != null) saleType = saleTypeOverride;
            if (vatTypeOverride != null) vatType = vatTypeOverride;
            String saleTypes = saleType == 1 ? "(0,1)" : "(0,2)";
            String formulaSaleTypes = saleType == 2 ? "(0)" : saleTypes;
            String formulaTaxTypes = vatType == 0 ? "(0,1)" : vatType == 1 ? "(0,2)" : "(0,3)";
            String priceSql = "SELECT p.sale_price1,p.sale_price2,p.price_type FROM ic_inventory_price p "
                    + "WHERE p.ic_code=? AND p.unit_code=? AND CURRENT_DATE BETWEEN p.from_date AND p.to_date "
                    + "AND 1 BETWEEN p.from_qty AND p.to_qty AND p.sale_type IN " + saleTypes + " "
                    + "AND ((p.price_type=3 AND p.cust_code=?) OR (p.price_type=2 AND p.cust_group_1="
                    + "(SELECT group_main FROM ar_customer_detail WHERE ar_code=?) AND "
                    + "(COALESCE(p.cust_group_2,'')='' OR p.cust_group_2 IN "
                    + "(SELECT group_sub_1 FROM ar_customer_detail WHERE ar_code=? UNION SELECT group_sub_3 FROM ar_customer_detail WHERE ar_code=? "
                    + "UNION SELECT group_sub_4 FROM ar_customer_detail WHERE ar_code=?))) "
                    + "OR (p.price_type=1 AND p.price_mode IN (0,1))) "
                    + "ORDER BY CASE p.price_type WHEN 3 THEN 0 WHEN 2 THEN 1 ELSE 2 END,"
                    + "p.price_mode DESC,p.sale_type DESC,p.transport_type DESC,p.cust_group_2 LIMIT 1";
            BigDecimal price = null;
            String source = "";
            boolean foundPrice = false;
            try (PreparedStatement stmt = conn.prepareStatement(priceSql)) {
                stmt.setString(1, itemCode); stmt.setString(2, unitCode);
                for (int index = 3; index <= 7; index++) stmt.setString(index, customer);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        foundPrice = true;
                        price = rs.getBigDecimal(vatType == 0 ? "sale_price1" : "sale_price2");
                        source = "price_" + rs.getInt("price_type");
                    }
                }
            }
            if (!foundPrice) {
                String formulaSql = "SELECT price_0,price_1,price_2,price_3,price_4,price_5,price_6,price_7,price_8,price_9 "
                        + "FROM ic_inventory_price_formula WHERE ic_code=? AND unit_code=? AND sale_type IN " + formulaSaleTypes + " "
                        + "AND COALESCE(tax_type,0) IN " + formulaTaxTypes + " "
                        + "ORDER BY sale_type DESC LIMIT 1";
                int level = 0;
                if (!customer.isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(price_level,0) FROM ar_customer WHERE code=?")) {
                        stmt.setString(1, customer);
                        try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) level = rs.getInt(1); }
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(formulaSql)) {
                    stmt.setString(1, itemCode); stmt.setString(2, unitCode);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            foundPrice = true;
                            String baseText = rs.getString("price_0");
                            if (baseText != null && !baseText.trim().isEmpty()) {
                                try {
                                    BigDecimal base = new BigDecimal(baseText);
                                    String formula = level >= 1 && level <= 9 ? rs.getString("price_" + level) : baseText;
                                    price = productInfoFormulaPrice(base, formula == null || formula.isEmpty() ? baseText : formula);
                                    source = "formula";
                                } catch (NumberFormatException ignored) { }
                            }
                        }
                    }
                }
            }
            if (!foundPrice) {
                int lastPriceType = 0;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COALESCE(get_last_price_type,0) FROM erp_option LIMIT 1")) {
                    if (rs.next()) lastPriceType = rs.getInt(1);
                }
                if (lastPriceType == 1 || lastPriceType == 2) {
                    String lastSql = lastPriceType == 1
                            ? "SELECT price_exclude_vat,price,(SELECT vat_type FROM ic_trans WHERE ic_trans.doc_no=ic_trans_detail.doc_no AND ic_trans.trans_flag=ic_trans_detail.trans_flag) AS vat_type FROM ic_trans_detail WHERE cust_code=? AND item_code=? AND unit_code=? AND trans_flag=44 AND last_status=0 AND price_exclude_vat>0 ORDER BY doc_date DESC,doc_time DESC LIMIT 1"
                            : "SELECT AVG(price_exclude_vat) FROM ic_trans_detail WHERE cust_code=? AND item_code=? AND unit_code=? AND trans_flag=44 AND last_status=0 AND price_exclude_vat>0";
                    try (PreparedStatement stmt = conn.prepareStatement(lastSql)) {
                        stmt.setString(1, customer); stmt.setString(2, itemCode); stmt.setString(3, unitCode);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                price = rs.getBigDecimal(1);
                                if (price != null) {
                                    if (vatType == 1 && vatRate != null) {
                                        price = price.add(price.multiply(vatRate).divide(new BigDecimal("100")));
                                    }
                                    if (vatType == 1 && lastPriceType == 1 && "1".equals(rs.getString("vat_type")) && rs.getBigDecimal("price") != null) {
                                        price = rs.getBigDecimal("price");
                                    }
                                    source = "last_sale";
                                }
                            }
                        }
                    }
                }
            }
            JSONObject row = new JSONObject().put("item_code", itemCode).put("unit_code", unitCode)
                    .put("price", price == null ? JSONObject.NULL : price).put("source", source);
            return Response.ok(new JSONObject().put("success", true).put("data", row).toString(), MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            return Response.status(400).entity(new JSONObject().put("ERROR", ex.getMessage()).toString()).build();
        }
    }

    @GET
    @Path("/images")
    @Produces("image/png")
    public Response _getImage(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("itemcode") String itemCode,
            @Context Request request
    ) {

        if (itemCode == null || itemCode.trim().isEmpty()) {
            return Response.status(400)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("ERROR: item_code is required")
                    .build();
        }

        byte[] imageBytes = null;

        // กัน SQL injection
        String sql = "select image_file from images where image_id = ? limit 1";

        _routine routine = new _routine();

        try (Connection conn = routine._connect(strDatabaseName.toLowerCase() + "_images", _global.FILE_CONFIG(strProvider));
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    imageBytes = rs.getBytes(1);
                }
            }

        } catch (Exception ex) {
            return Response.status(500)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("ERROR: " + ex.getMessage())
                    .build();
        }

        if (imageBytes == null || imageBytes.length == 0) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("ERROR: image not found")
                    .build();
        }

        // Cache-Control
        CacheControl cc = new CacheControl();
        cc.setPrivate(false);
        cc.setNoStore(false);
        cc.setNoCache(false);
        cc.setMaxAge(604800);

        // ETag (ใช้ hash ของ bytes เพื่อให้ client revalidate แล้วได้ 304)
        EntityTag etag = new EntityTag(md5Hex(imageBytes));

        // ถ้า client ส่ง If-None-Match ที่ตรงกับ etag -> return 304
        Response.ResponseBuilder precond = request.evaluatePreconditions(etag);
        if (precond != null) {
            return precond
                    .cacheControl(cc)
                    .tag(etag)
                    .build();
        }

        // 200 OK + ส่งรูป
        return Response.ok(new ByteArrayInputStream(imageBytes))
                .type("image/png")
                .cacheControl(cc)
                .tag(etag)
                .header("Content-Length", String.valueOf(imageBytes.length))
                .build();
    }

    private String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            // fallback กัน error
            return String.valueOf(data.length);
        }
    }

    @GET
    @Path("/getItemDetailSheet")
    public Response getItemDetailSheet(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("barcode") String strBarcode,
            @QueryParam("whcode") String strWhCode,
            @QueryParam("lccode") String strLcCode,
            @QueryParam("docdate") String strDocDate,
            @QueryParam("doctime") String strDocTime) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select ic_code,barcode,description,unit_code,coalesce((select sum(balance_qty) as balance_qty from sml_ic_function_stock_balance_warehouse_location('" + strDocDate + " " + strDocTime + "', ic_code,'" + strWhCode + "','" + strLcCode + "') where ic_unit_code = unit_code),0)as balance_qty from ic_inventory_barcode where barcode= '" + strBarcode + "' limit 1";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("item_name", __rsHead.getString("description"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                float balanceQty = Float.parseFloat(__rsHead.getString("balance_qty"));
                String formattedBalance = String.format("%.2f", balanceQty);
                obj.put("balance_qty", formattedBalance);

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemDetail")
    public Response getItemDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("barcode") String strBarcode,
            @QueryParam("whcode") String strWhCode,
            @QueryParam("lccode") String strLcCode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select ic_code,barcode,description,unit_code,coalesce((select sum(balance_qty) as balance_qty from sml_ic_function_stock_balance_warehouse_location(now()::date, ic_code,'" + strWhCode + "','" + strLcCode + "') where ic_unit_code = unit_code),0)as balance_qty from ic_inventory_barcode where barcode= '" + strBarcode + "' limit 1";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("item_name", __rsHead.getString("description"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                float balanceQty = Float.parseFloat(__rsHead.getString("balance_qty"));
                String formattedBalance = String.format("%.2f", balanceQty);
                obj.put("balance_qty", formattedBalance);

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemScan")
    public Response getItemScan(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("barcode") String strBarcode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select ic_code,barcode,description,unit_code from ic_inventory_barcode where barcode= '" + strBarcode + "' limit 1";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("item_name", __rsHead.getString("description"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("balance_qty", "0");

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getItemBalance")
    public Response getItemBalance(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("barcode") String strBarcode,
            @QueryParam("whcode") String strWhCode,
            @QueryParam("lccode") String strLcCode,
            @QueryParam("docdate") String strDocDate,
            @QueryParam("doctime") String strDocTime) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            StringBuilder __where = new StringBuilder();
            String _whereFinal = "";

            String __strQUERY1 = "select barcode,item_code,unit_code,coalesce((select sum(balance_qty) as balance_qty from sml_ic_function_stock_balance_warehouse_location('" + strDocDate + " " + strDocTime + "', item_code,'" + strWhCode + "','" + strLcCode + "') where ic_unit_code = unit_code),0)as balance_qty from stc_tag_card where tag_code= '" + strBarcode + "' limit 1";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("item_code"));

                obj.put("unit_code", __rsHead.getString("unit_code"));
                float balanceQty = Float.parseFloat(__rsHead.getString("balance_qty"));
                String formattedBalance = String.format("%.2f", balanceQty);
                obj.put("balance_qty", formattedBalance);

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getWarehouse")
    public Response getWarehouse(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("branchcode") String strBranchcode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            if (!strBranchcode.equals("")) {
                _where = " and branch_code = '" + strBranchcode.toUpperCase() + "'";
            }
            String __strQUERY1 = "SELECT code,name_1 FROM ic_warehouse  where 1=1 " + _where + " ORDER BY code";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getAllWarehouse")
    public Response getAllWarehouse(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName
    ) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT code,name_1 FROM ic_warehouse ORDER BY code";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getAllLocation")
    public Response getAllLocation(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT code,name_1,whcode FROM ic_shelf ORDER BY code";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));
                obj.put("whcode", __rsHead.getString("whcode"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(__objResponse).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getLocation")
    public Response getLocation(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("whcode") String strWhcode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT code,name_1 FROM ic_shelf where whcode = '" + strWhcode + "' ORDER BY code";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name", __rsHead.getString("name_1"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(__objResponse).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/sendApprove")
    public Response sendApprove(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "update msc_cart set status=4 WHERE doc_no ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/cancelApprove")
    public Response cancelApprove(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "UPDATE msc_cart_sub "
                    + "SET status = 0 "
                    + "WHERE (doc_ref, roworder) IN ( "
                    + "    SELECT doc_ref, MAX(roworder) "
                    + "    FROM msc_cart_sub "
                    + "    WHERE doc_ref = '" + strDocno + "' "
                    + "    GROUP BY doc_ref "
                    + ");update msc_cart set status=0 WHERE doc_no ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/inApproveCartDoc")
    public Response inApproveCartDoc(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "update msc_cart set status=6 WHERE doc_no ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getErpUserPermission")
    public Response getErpUserPermission(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensureProductInfoPermissionColumn(__conn);

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) like '%" + strSearch.toUpperCase() + "%' or upper(name_1) like '%" + strSearch.toUpperCase() + "%' ";
            }

            String __strQUERY1 = "select code,name_1,"
                    + "coalesce((select stock_list from msc_permission where user_code = code),0) as stock_list, "
                    + "coalesce((select request_list from msc_permission where user_code = code),0) as request_list, "
                    + "coalesce((select transfer_list from msc_permission where user_code = code),0) as transfer_list, "
                    + "coalesce((select handheld_list from msc_permission where user_code = code),0) as handheld_list, "
                    + "coalesce((select info_list from msc_permission where user_code = code),0) as info_list, "
                    + "coalesce((select barcode_list from msc_permission where user_code = code),0) as barcode_list, "
                    + "coalesce((select permission_list from msc_permission where user_code = code),0) as permission_list, "
                    + "coalesce((select product_info_list from msc_permission where user_code = code),0) as product_info_list "
                    + "from erp_user " + _where;
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1;
            ResultSet __rsHead;
            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            //JSONArray __jsonArr = _convertResultSetIntoJSON(__rs1);
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name_1", __rsHead.getString("name_1"));
                obj.put("stock_list", __rsHead.getString("stock_list"));
                obj.put("request_list", __rsHead.getString("request_list"));
                obj.put("transfer_list", __rsHead.getString("transfer_list"));
                obj.put("handheld_list", __rsHead.getString("handheld_list"));
                obj.put("info_list", __rsHead.getString("info_list"));
                obj.put("barcode_list", __rsHead.getString("barcode_list"));
                obj.put("permission_list", __rsHead.getString("permission_list"));
                obj.put("product_info_list", __rsHead.getString("product_info_list"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __conn.close();
            __rsHead.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/upDatePricePermission")
    public Response upDatePricePermission(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("user") String user_code,
            @QueryParam("price_0") String price_0,
            @QueryParam("price_1") String price_1,
            @QueryParam("price_2") String price_2,
            @QueryParam("price_3") String price_3,
            @QueryParam("price_4") String price_4,
            @QueryParam("price_5") String price_5,
            @QueryParam("price_6") String price_6,
            @QueryParam("price_7") String price_7,
            @QueryParam("price_8") String price_8,
            @QueryParam("price_9") String price_9
    ) throws Exception {

        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensurePricePermissionTable(__conn);

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery("select user_code from msc_price_permission where user_code = '" + user_code + "'");

            __rsHead.next();

            int row = __rsHead.getRow();

            if (row > 0) {
                Statement __stmtdelete = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtdelete.executeUpdate("delete from msc_price_permission where user_code = '" + user_code + "';");
                __stmtdelete.close();
            }

            StringBuilder __query_builder = new StringBuilder();
            __query_builder.append("INSERT INTO msc_price_permission (user_code,price_0,price_1,price_2,price_3,price_4,price_5,price_6,price_7,price_8,price_9) values ('"
                    + user_code + "','" + price_0 + "','" + price_1 + "','" + price_2 + "','" + price_3 + "','" + price_4 + "','" + price_5 + "','" + price_6 + "','" + price_7 + "','" + price_8 + "','" + price_9 + "');");

            System.out.println("__query_builder" + __query_builder);

            Statement __stmt2 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt2.executeUpdate(__query_builder.toString());

            __objResponse.put("msg", "success");
            __objResponse.put("success", true);

            __rsHead.close();
            __stmt1.close();
            __stmt2.close();
            __conn.close();

        } catch (JSONException ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(__objResponse.toString(), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getErpUserPricePermission")
    public Response getErpUserPricePermission(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensurePricePermissionTable(__conn);

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) like '%" + strSearch.toUpperCase() + "%' or upper(name_1) like '%" + strSearch.toUpperCase() + "%' ";
            }

            String __strQUERY1 = "select code,name_1,"
                    + "coalesce((select price_0 from msc_price_permission where user_code = code),0) as price_0, "
                    + "coalesce((select price_1 from msc_price_permission where user_code = code),0) as price_1, "
                    + "coalesce((select price_2 from msc_price_permission where user_code = code),0) as price_2, "
                    + "coalesce((select price_3 from msc_price_permission where user_code = code),0) as price_3, "
                    + "coalesce((select price_4 from msc_price_permission where user_code = code),0) as price_4, "
                    + "coalesce((select price_5 from msc_price_permission where user_code = code),0) as price_5, "
                    + "coalesce((select price_6 from msc_price_permission where user_code = code),0) as price_6, "
                    + "coalesce((select price_7 from msc_price_permission where user_code = code),0) as price_7, "
                    + "coalesce((select price_8 from msc_price_permission where user_code = code),0) as price_8, "
                    + "coalesce((select price_9 from msc_price_permission where user_code = code),0) as price_9 "
                    + "from erp_user " + _where + " order by code";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name_1", __rsHead.getString("name_1"));
                obj.put("price_0", __rsHead.getString("price_0"));
                obj.put("price_1", __rsHead.getString("price_1"));
                obj.put("price_2", __rsHead.getString("price_2"));
                obj.put("price_3", __rsHead.getString("price_3"));
                obj.put("price_4", __rsHead.getString("price_4"));
                obj.put("price_5", __rsHead.getString("price_5"));
                obj.put("price_6", __rsHead.getString("price_6"));
                obj.put("price_7", __rsHead.getString("price_7"));
                obj.put("price_8", __rsHead.getString("price_8"));
                obj.put("price_9", __rsHead.getString("price_9"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getErpUserPricePermissionLogin")
    public Response getErpUserPricePermissionLogin(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("usercode") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensurePricePermissionTable(__conn);

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) = '" + strSearch.toUpperCase() + "' ";
            }

            String __strQUERY1 = "select code,name_1,"
                    + "coalesce((select price_0 from msc_price_permission where user_code = code),0) as price_0, "
                    + "coalesce((select price_1 from msc_price_permission where user_code = code),0) as price_1, "
                    + "coalesce((select price_2 from msc_price_permission where user_code = code),0) as price_2, "
                    + "coalesce((select price_3 from msc_price_permission where user_code = code),0) as price_3, "
                    + "coalesce((select price_4 from msc_price_permission where user_code = code),0) as price_4, "
                    + "coalesce((select price_5 from msc_price_permission where user_code = code),0) as price_5, "
                    + "coalesce((select price_6 from msc_price_permission where user_code = code),0) as price_6, "
                    + "coalesce((select price_7 from msc_price_permission where user_code = code),0) as price_7, "
                    + "coalesce((select price_8 from msc_price_permission where user_code = code),0) as price_8, "
                    + "coalesce((select price_9 from msc_price_permission where user_code = code),0) as price_9 "
                    + "from erp_user " + _where;
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name_1", __rsHead.getString("name_1"));
                obj.put("price_0", __rsHead.getString("price_0"));
                obj.put("price_1", __rsHead.getString("price_1"));
                obj.put("price_2", __rsHead.getString("price_2"));
                obj.put("price_3", __rsHead.getString("price_3"));
                obj.put("price_4", __rsHead.getString("price_4"));
                obj.put("price_5", __rsHead.getString("price_5"));
                obj.put("price_6", __rsHead.getString("price_6"));
                obj.put("price_7", __rsHead.getString("price_7"));
                obj.put("price_8", __rsHead.getString("price_8"));
                obj.put("price_9", __rsHead.getString("price_9"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __stmt1.close();
            __conn.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getErpUserPermissionLogin")
    public Response getErpUserPermissionLogin(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("usercode") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensureProductInfoPermissionColumn(__conn);

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) = '" + strSearch.toUpperCase() + "' ";
            }

            String __strQUERY1 = "select code,name_1,"
                    + "coalesce((select stock_list from msc_permission where user_code = code),0) as stock_list, "
                    + "coalesce((select request_list from msc_permission where user_code = code),0) as request_list, "
                    + "coalesce((select transfer_list from msc_permission where user_code = code),0) as transfer_list, "
                    + "coalesce((select handheld_list from msc_permission where user_code = code),0) as handheld_list, "
                    + "coalesce((select info_list from msc_permission where user_code = code),0) as info_list, "
                    + "coalesce((select permission_list from msc_permission where user_code = code),0) as permission_list, "
                    + "coalesce((select barcode_list from msc_permission where user_code = code),0) as barcode_list, "
                    + "coalesce((select product_info_list from msc_permission where user_code = code),0) as product_info_list "
                    + "from erp_user " + _where;
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1;
            ResultSet __rsHead;
            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            //JSONArray __jsonArr = _convertResultSetIntoJSON(__rs1);
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("code", __rsHead.getString("code"));
                obj.put("name_1", __rsHead.getString("name_1"));
                obj.put("stock_list", __rsHead.getString("stock_list"));
                obj.put("request_list", __rsHead.getString("request_list"));
                obj.put("transfer_list", __rsHead.getString("transfer_list"));
                obj.put("handheld_list", __rsHead.getString("handheld_list"));
                obj.put("info_list", __rsHead.getString("info_list"));
                obj.put("barcode_list", __rsHead.getString("barcode_list"));
                obj.put("permission_list", __rsHead.getString("permission_list"));
                obj.put("product_info_list", __rsHead.getString("product_info_list"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __conn.close();
            __rsHead.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/upDatePermission")
    public Response upDatePermission(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("user") String user_code,
            @QueryParam("stock_list") String stock_list,
            @QueryParam("request_list") String request_list,
            @QueryParam("transfer_list") String transfer_list,
            @QueryParam("handheld_list") String handheld_list,
            @QueryParam("barcode_list") String barcode_list,
            @QueryParam("info_list") String info_list,
            @QueryParam("product_info_list") String product_info_list,
            @QueryParam("permission_list") String permission_list
    ) throws Exception {

        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            UUID uuid = UUID.randomUUID();
            String strGUID = uuid.toString();

            StringBuilder __result = new StringBuilder();
            StringBuilder __query_builder = new StringBuilder();
            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            ensureProductInfoPermissionColumn(__conn);

            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery("select user_code from msc_permission where user_code = '" + user_code + "'");

            __rsHead.next();

            int row = __rsHead.getRow();

            if (row > 0) {
                Statement __stmtdelete = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtdelete.executeUpdate("delete from msc_permission where user_code = '" + user_code + "';");
            }

            __query_builder.append("INSERT INTO msc_permission (user_code,stock_list,request_list,transfer_list,handheld_list,barcode_list,info_list,permission_list,product_info_list) values ('" + user_code + "','" + stock_list + "','" + request_list + "','" + transfer_list + "','" + handheld_list + "','" + barcode_list + "','" + info_list + "','" + permission_list + "','" + ("1".equals(product_info_list) ? "1" : "0") + "');");

            System.out.println("__query_builder" + __query_builder);

            Statement __stmt2;

            __stmt2 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt2.executeUpdate(__query_builder.toString());

            __objResponse.put("msg", "success");
            __objResponse.put("success", true);

            __stmt2.close();
            __conn.close();

        } catch (JSONException ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(__objResponse.toString(), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/approveSaveStock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSaveStock(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String docno = req.optString("docno");
            String docref = req.optString("docref");
            String branchcode = req.optString("branchcode");
            String whcode = req.optString("whcode");
            String locationcode = req.optString("locationcode");
            String remark = req.optString("remark");
            String usercode = req.optString("usercode").toUpperCase();
            String docdate = req.optString("docdate");
            String doctime = req.optString("doctime");
            JSONArray details = req.optJSONArray("details");

            if (details == null || details.length() == 0) {
                throw new Exception("details is empty");
            }

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "INSERT INTO ic_trans (trans_flag, trans_type, doc_no, doc_date, doc_time, "
                    + "doc_ref, doc_ref_date, total_amount, doc_format_code, remark, branch_code) "
                    + "VALUES (76, 3, ?, ?, ?, ?, ?, 0, 'CO', ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docno);
                ps.setDate(5, java.sql.Date.valueOf(docdate));
                ps.setString(6, remark);
                ps.setString(7, branchcode);
                ps.executeUpdate();
            }

            // ---------- Insert ic_trans_detail ----------
            String sqlDetail
                    = "INSERT INTO ic_trans_detail ("
                    + "trans_flag, trans_type, calc_flag, doc_no, doc_date, doc_time, "
                    + "doc_date_calc, doc_time_calc, last_status, line_number, ratio, "
                    + "stand_value, divide_value, item_code, item_name, unit_code, qty, "
                    + "wh_code, shelf_code, price, sum_amount, branch_code,barcode"
                    + ") VALUES ("
                    + "76, 3, 1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?"
                    + ")";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setDate(4, java.sql.Date.valueOf(docdate));
                    ps.setString(5, doctime);
                    ps.setInt(6, i);

                    ps.setString(7, d.getString("unit_code"));
                    ps.setString(8, d.getString("item_code"));
                    ps.setString(9, d.getString("unit_code"));
                    ps.setString(10, d.getString("item_code"));
                    ps.setString(11, d.getString("unit_code"));
                    ps.setString(12, d.getString("item_code"));

                    ps.setString(13, d.getString("item_code"));
                    ps.setString(14, d.getString("item_name"));
                    ps.setString(15, d.getString("unit_code"));
                    ps.setBigDecimal(16, new BigDecimal(d.getString("qty")));
                    ps.setString(17, whcode);
                    ps.setString(18, locationcode);
                    ps.setString(19, branchcode);
                    ps.setString(20, d.getString("barcode"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ---------- Update msc_cart ----------
            String sqlUpdateCart
                    = "UPDATE msc_cart SET status=5, doc_ref=?, user_approve=?, approve_datetime=now() "
                    + "WHERE doc_no=?";

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCart)) {
                ps.setString(1, docref);
                ps.setString(2, usercode);
                ps.setString(3, docno);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            response.put("docref", docref);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/approveSaveHandHeld")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSaveHandHeld(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String docno = req.optString("docno");
            String docref = req.optString("docref");
            String branchcode = req.optString("branchcode");
            String whcode = req.optString("whcode");
            String locationcode = req.optString("locationcode");
            String remark = req.optString("remark");
            String usercode = req.optString("usercode").toUpperCase();
            String docdate = req.optString("docdate");
            String doctime = req.optString("doctime");
            JSONArray details = req.optJSONArray("details");

            if (details == null || details.length() == 0) {
                throw new Exception("details is empty");
            }

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans_detail ----------
            String sqlDetail
                    = "INSERT INTO barcode_import_list_detail ("
                    + "doc_no, doc_date, doc_time, "
                    + " item_code, unit_code, qty,barcode"
                    + ") VALUES ("
                    + " ?, ?, ?, ?, ?, ?, ?  "
                    + ")";
            double sum_qty = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setString(4, d.getString("item_code"));
                    ps.setString(5, d.getString("unit_code"));
                    ps.setBigDecimal(6, new BigDecimal(d.getString("qty")));
                    ps.setString(7, d.getString("barcode"));
                    sum_qty += Double.parseDouble(d.getString("qty"));

                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "INSERT INTO barcode_import_list (machine_name, doc_no, doc_date, doc_time, "
                    + "qty,wh_code,shelf_code) "
                    + "VALUES (?, ?, ?, ?, ?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, usercode);
                ps.setString(2, docref);
                ps.setDate(3, java.sql.Date.valueOf(docdate));
                ps.setString(4, doctime);
                ps.setDouble(5, sum_qty);
                ps.setString(6, whcode);
                ps.setString(7, locationcode);
                ps.executeUpdate();
            }

            // ---------- Update msc_cart ----------
            String sqlUpdateCart
                    = "UPDATE msc_cart SET status=5, doc_ref=?, user_approve=?, approve_datetime=now() "
                    + "WHERE doc_no=?";

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCart)) {
                ps.setString(1, docref);
                ps.setString(2, usercode);
                ps.setString(3, docno);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            response.put("docref", docref);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/approveSaveRequestTransfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSaveRequestTransfer(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String docno = req.optString("docno");
            String docref = req.optString("docref");
            String branchcode = req.optString("branchcode");
            String whcode = req.optString("whcode");
            String locationcode = req.optString("locationcode");
            String towhcode = req.optString("whto");
            String tolocationcode = req.optString("locationto");
            String remark = req.optString("remark");
            String usercode = req.optString("usercode").toUpperCase();
            String docdate = req.optString("docdate");
            String doctime = req.optString("doctime");
            String custcode = req.optString("custcode");
            JSONArray details = req.optJSONArray("details");

            if (details == null || details.length() == 0) {
                throw new Exception("details is empty");
            }

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "INSERT INTO ic_trans (trans_flag, trans_type, doc_no, doc_date, doc_time, "
                    + "doc_ref, doc_ref_date, total_amount, doc_format_code, remark, branch_code,cust_code,wh_from,location_from,wh_to,location_to) "
                    + "VALUES (124, 3, ?, ?, ?, ?, ?, 0, 'RTF', ?, ?,?,?,?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docref);
                ps.setDate(5, java.sql.Date.valueOf(docdate));
                ps.setString(6, remark);
                ps.setString(7, branchcode);
                ps.setString(8, custcode);
                ps.setString(9, whcode);
                ps.setString(10, locationcode);
                ps.setString(11, towhcode);
                ps.setString(12, tolocationcode);

                ps.executeUpdate();
            }

            // ---------- Insert ic_trans_detail ----------
            String sqlDetail
                    = "INSERT INTO ic_trans_detail ("
                    + "trans_flag, trans_type, calc_flag, doc_no, doc_date, doc_time, "
                    + "doc_date_calc, doc_time_calc, last_status, line_number, ratio, "
                    + "stand_value, divide_value, item_code, item_name, unit_code, qty, "
                    + "wh_code, shelf_code, price, sum_amount, branch_code,wh_code_2, shelf_code_2,barcode"
                    + ") VALUES ("
                    + "124, 3, 0, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?,?,?"
                    + ")";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setDate(4, java.sql.Date.valueOf(docdate));
                    ps.setString(5, doctime);
                    ps.setInt(6, i);

                    ps.setString(7, d.getString("unit_code"));
                    ps.setString(8, d.getString("item_code"));
                    ps.setString(9, d.getString("unit_code"));
                    ps.setString(10, d.getString("item_code"));
                    ps.setString(11, d.getString("unit_code"));
                    ps.setString(12, d.getString("item_code"));

                    ps.setString(13, d.getString("item_code"));
                    ps.setString(14, d.getString("item_name"));
                    ps.setString(15, d.getString("unit_code"));
                    ps.setBigDecimal(16, new BigDecimal(d.getString("qty")));
                    ps.setString(17, whcode);
                    ps.setString(18, locationcode);
                    ps.setString(19, branchcode);
                    ps.setString(20, towhcode);
                    ps.setString(21, tolocationcode);
                    ps.setString(22, d.getString("barcode"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ---------- Update msc_cart ----------
            String sqlUpdateCart
                    = "UPDATE msc_cart SET status=5, doc_ref=?, user_approve=?, approve_datetime=now() "
                    + "WHERE doc_no=?";

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCart)) {
                ps.setString(1, docref);
                ps.setString(2, usercode);
                ps.setString(3, docno);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            response.put("docref", docref);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/approveSaveTransfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSaveTransfer(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String docno = req.optString("docno");
            String docref = req.optString("docref");
            String branchcode = req.optString("branchcode");
            String whcode = req.optString("whcode");
            String locationcode = req.optString("locationcode");
            String towhcode = req.optString("whto");
            String tolocationcode = req.optString("locationto");
            String remark = req.optString("remark");
            String usercode = req.optString("usercode").toUpperCase();
            String docdate = req.optString("docdate");
            String doctime = req.optString("doctime");
            String custcode = req.optString("custcode");
            JSONArray details = req.optJSONArray("details");

            if (details == null || details.length() == 0) {
                throw new Exception("details is empty");
            }

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            // โอนออก
            String sqlTrans
                    = "INSERT INTO ic_trans (trans_flag, trans_type, doc_no, doc_date, doc_time, "
                    + "doc_ref, doc_ref_date, total_amount, doc_format_code, remark, branch_code,cust_code,wh_from,location_from,wh_to,location_to) "
                    + "VALUES (72, 3, ?, ?, ?, ?, ?, 0, 'TF', ?, ?,?,?,?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docref);
                ps.setDate(5, java.sql.Date.valueOf(docdate));
                ps.setString(6, remark);
                ps.setString(7, branchcode);
                ps.setString(8, custcode);
                ps.setString(9, whcode);
                ps.setString(10, locationcode);
                ps.setString(11, towhcode);
                ps.setString(12, tolocationcode);

                ps.executeUpdate();
            }
            // โอนเข้า
            String sqlTransIn
                    = "INSERT INTO ic_trans (trans_flag, trans_type, doc_no, doc_date, doc_time, "
                    + "doc_ref, doc_ref_date, total_amount, doc_format_code, remark, branch_code,cust_code,wh_from,location_from,wh_to,location_to) "
                    + "VALUES (70, 3, ?, ?, ?, ?, ?, 0, 'TF', ?, ?,?,?,?,?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTransIn)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docref);
                ps.setDate(5, java.sql.Date.valueOf(docdate));
                ps.setString(6, remark);
                ps.setString(7, branchcode);
                ps.setString(8, custcode);
                ps.setString(9, whcode);
                ps.setString(10, locationcode);
                ps.setString(11, towhcode);
                ps.setString(12, tolocationcode);

                ps.executeUpdate();
            }

            // ---------- Insert ic_trans_detail ----------
            // โอนออก
            String sqlDetail
                    = "INSERT INTO ic_trans_detail ("
                    + "trans_flag, trans_type, calc_flag, doc_no, doc_date, doc_time, "
                    + "doc_date_calc, doc_time_calc, last_status, line_number, ratio, "
                    + "stand_value, divide_value, item_code, item_name, unit_code, qty, "
                    + "wh_code, shelf_code, price, sum_amount, branch_code , wh_code_2, shelf_code_2,barcode"
                    + ") VALUES ("
                    + "72, 3, -1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?,?,?"
                    + ")";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setDate(4, java.sql.Date.valueOf(docdate));
                    ps.setString(5, doctime);
                    ps.setInt(6, i);

                    ps.setString(7, d.getString("unit_code"));
                    ps.setString(8, d.getString("item_code"));
                    ps.setString(9, d.getString("unit_code"));
                    ps.setString(10, d.getString("item_code"));
                    ps.setString(11, d.getString("unit_code"));
                    ps.setString(12, d.getString("item_code"));

                    ps.setString(13, d.getString("item_code"));
                    ps.setString(14, d.getString("item_name"));
                    ps.setString(15, d.getString("unit_code"));
                    ps.setBigDecimal(16, new BigDecimal(d.getString("qty")));
                    ps.setString(17, whcode);
                    ps.setString(18, locationcode);
                    ps.setString(19, branchcode);
                    ps.setString(20, towhcode);
                    ps.setString(21, tolocationcode);
                    ps.setString(22, d.getString("barcode"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // โอนเข้า
            String sqlDetailIn
                    = "INSERT INTO ic_trans_detail ("
                    + "trans_flag, trans_type, calc_flag, doc_no, doc_date, doc_time, "
                    + "doc_date_calc, doc_time_calc, last_status, line_number, ratio, "
                    + "stand_value, divide_value, item_code, item_name, unit_code, qty, "
                    + "wh_code, shelf_code, price, sum_amount, branch_code , wh_code_2, shelf_code_2"
                    + ") VALUES ("
                    + "70, 3, 1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?,?"
                    + ")";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetailIn)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setDate(4, java.sql.Date.valueOf(docdate));
                    ps.setString(5, doctime);
                    ps.setInt(6, i);

                    ps.setString(7, d.getString("unit_code"));
                    ps.setString(8, d.getString("item_code"));
                    ps.setString(9, d.getString("unit_code"));
                    ps.setString(10, d.getString("item_code"));
                    ps.setString(11, d.getString("unit_code"));
                    ps.setString(12, d.getString("item_code"));

                    ps.setString(13, d.getString("item_code"));
                    ps.setString(14, d.getString("item_name"));
                    ps.setString(15, d.getString("unit_code"));
                    ps.setBigDecimal(16, new BigDecimal(d.getString("qty")));
                    ps.setString(17, towhcode);
                    ps.setString(18, tolocationcode);
                    ps.setString(19, branchcode);
                    ps.setString(20, whcode);
                    ps.setString(21, locationcode);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ---------- Update msc_cart ----------
            String sqlUpdateCart
                    = "UPDATE msc_cart SET status=5, doc_ref=?, user_approve=?, approve_datetime=now() "
                    + "WHERE doc_no=?";

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCart)) {
                ps.setString(1, docref);
                ps.setString(2, usercode);
                ps.setString(3, docno);
                ps.executeUpdate();
            }

            ///-----------------process--------------///
            String sqlProcess
                    = "insert into process (process_name, wherein) select 'IC', item_code from ic_trans_detail where doc_no =? and trans_flag = 70";

            try (PreparedStatement ps = conn.prepareStatement(sqlProcess)) {
                ps.setString(1, docref);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            response.put("docref", docref);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/approveSaveReceive")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSaveReceive(
            @QueryParam("provider") String provider,
            @QueryParam("dbname") String dbName,
            String data) {

        JSONObject response = new JSONObject();
        Connection conn = null;

        try {
            // ---------- Parse JSON ----------
            JSONObject req = new JSONObject(data);

            String docno = req.optString("docno");
            String docref = req.optString("docref");
            String branchcode = req.optString("branchcode");
            String whcode = req.optString("whcode");
            String locationcode = req.optString("locationcode");
            String remark = req.optString("remark");
            String usercode = req.optString("usercode").toUpperCase();
            String docdate = req.optString("docdate");
            String doctime = req.optString("doctime");
            String custcode = req.optString("custcode");
            JSONArray details = req.optJSONArray("details");

            if (details == null || details.length() == 0) {
                throw new Exception("details is empty");
            }

            // ---------- Connect DB ----------
            _routine routine = new _routine();
            conn = routine._connect(dbName.toLowerCase(), _global.FILE_CONFIG(provider));
            conn.setAutoCommit(false); // 🔥 START TRANSACTION

            // ---------- Insert ic_trans ----------
            String sqlTrans
                    = "INSERT INTO ic_trans (trans_flag, trans_type, doc_no, doc_date, doc_time, "
                    + "doc_ref, doc_ref_date, total_amount, doc_format_code, remark, branch_code,cust_code) "
                    + "VALUES (310, 1, ?, ?, ?, ?, ?, 0, 'PI', ?, ?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docref);
                ps.setDate(5, java.sql.Date.valueOf(docdate));
                ps.setString(6, remark);
                ps.setString(7, branchcode);
                ps.setString(8, custcode);
                ps.executeUpdate();
            }

            // ---------- Insert ic_trans_detail ----------
            String sqlDetail
                    = "INSERT INTO ic_trans_detail ("
                    + "trans_flag, trans_type, calc_flag, doc_no, doc_date, doc_time, "
                    + "doc_date_calc, doc_time_calc, last_status, line_number, ratio, "
                    + "stand_value, divide_value, item_code, item_name, unit_code, qty, "
                    + "wh_code, shelf_code, price, sum_amount, branch_code,barcode"
                    + ") VALUES ("
                    + "310, 1, 1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?"
                    + ")";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject d = details.getJSONObject(i);

                    ps.setString(1, docref);
                    ps.setDate(2, java.sql.Date.valueOf(docdate));
                    ps.setString(3, doctime);
                    ps.setDate(4, java.sql.Date.valueOf(docdate));
                    ps.setString(5, doctime);
                    ps.setInt(6, i);

                    ps.setString(7, d.getString("unit_code"));
                    ps.setString(8, d.getString("item_code"));
                    ps.setString(9, d.getString("unit_code"));
                    ps.setString(10, d.getString("item_code"));
                    ps.setString(11, d.getString("unit_code"));
                    ps.setString(12, d.getString("item_code"));

                    ps.setString(13, d.getString("item_code"));
                    ps.setString(14, d.getString("item_name"));
                    ps.setString(15, d.getString("unit_code"));
                    ps.setBigDecimal(16, new BigDecimal(d.getString("qty")));
                    ps.setString(17, whcode);
                    ps.setString(18, locationcode);
                    ps.setString(19, branchcode);
                    ps.setString(20, d.getString("barcode"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ---------- Update msc_cart ----------
            String sqlUpdateCart
                    = "UPDATE msc_cart SET status=5, doc_ref=?, user_approve=?, approve_datetime=now() "
                    + "WHERE doc_no=?";

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateCart)) {
                ps.setString(1, docref);
                ps.setString(2, usercode);
                ps.setString(3, docno);
                ps.executeUpdate();
            }

            // ---------- COMMIT ----------
            conn.commit();

            response.put("success", true);
            response.put("docref", docref);
            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 🔥 ROLLBACK
                }
            } catch (Exception ignore) {
            }

            return Response.status(400)
                    .entity(new JSONObject()
                            .put("success", false)
                            .put("error", e.getMessage())
                            .toString())
                    .build();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @POST
    @Path("/approveProcessStock")
    public Response approveProcessStock(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        String docdate = "";
        String doctime = "";
        String carts = "";
        String doc_no_add = "";
        String doc_no_minus = "";

        JSONArray details_add = new JSONArray();
        JSONArray details_minus = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";

            branchcode = objJSData.has("branchcode") ? objJSData.getString("branchcode") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("usercode") ? objJSData.getString("usercode").toUpperCase() : "";
            docdate = objJSData.has("docdate") ? objJSData.getString("docdate") : "";
            doctime = objJSData.has("doctime") ? objJSData.getString("doctime") : "";

            doc_no_add = objJSData.has("doc_no_add") ? objJSData.getString("doc_no_add") : "";
            doc_no_minus = objJSData.has("doc_no_minus") ? objJSData.getString("doc_no_minus") : "";
            if (objJSData.has("details_add")) {
                details_add = objJSData.getJSONArray("details_add");
            }
            if (objJSData.has("details_minus")) {
                details_minus = objJSData.getJSONArray("details_minus");
            }
        }
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            for (int i = 0; i < details_add.length(); i++) {

                JSONObject objJSDataItem = details_add.getJSONObject(i);
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";
                String _queryDetail = "insert into ic_trans_detail (remark,trans_flag, trans_type, calc_flag,doc_no, doc_date, doc_time, doc_date_calc, doc_time_calc, last_status, line_number, ratio, stand_value, divide_value, item_code, item_name, unit_code, qty,price,sum_amount, wh_code, shelf_code, branch_code,creator_code ) "
                        + "values ('" + location + "',66,3,1,'" + doc_no_add + "','" + docdate + "', '" + doctime + "', '" + docdate + "',  '" + doctime + "', 0, " + i + ", (select ratio from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1), "
                        + "(select stand_value from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1), (select divide_value from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1),"
                        + " '" + objJSDataItem.getString("item_code") + "', '" + objJSDataItem.getString("item_name") + "', '" + objJSDataItem.getString("unit_code") + "',  '" + objJSDataItem.get("diff_qty") + "','" + objJSDataItem.get("price") + "','" + objJSDataItem.get("sum_amount") + "', '" + whcode + "', '" + locationcode + "',  '" + branchcode + "', '" + usercode + "');";
                Statement __stmtDetail;
                System.out.println(_queryDetail);

                __stmtDetail = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtDetail.executeUpdate(_queryDetail);
                __stmtDetail.close();
            }
            //WSC-2024111039D05C
            for (int i = 0; i < details_minus.length(); i++) {
                JSONObject objJSDataItem = details_minus.getJSONObject(i);
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";
                String _queryDetail = "insert into ic_trans_detail (remark,trans_flag, trans_type, calc_flag,doc_no, doc_date, doc_time, doc_date_calc, doc_time_calc, last_status, line_number, ratio, stand_value, divide_value, item_code, item_name, unit_code, qty,price,sum_amount, wh_code, shelf_code, branch_code,creator_code ) "
                        + "values ('" + location + "',68,3,-1,'" + doc_no_minus + "', '" + docdate + "', '" + doctime + "', '" + docdate + "',  '" + doctime + "', 0, " + i + ", (select ratio from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1), "
                        + "(select stand_value from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1), (select divide_value from ic_unit_use where ic_unit_use.code = '" + objJSDataItem.getString("unit_code") + "' and ic_unit_use.ic_code = '" + objJSDataItem.getString("item_code") + "' limit 1),"
                        + " '" + objJSDataItem.getString("item_code") + "', '" + objJSDataItem.getString("item_name") + "', '" + objJSDataItem.getString("unit_code") + "',  '" + objJSDataItem.get("diff_qty") + "','" + objJSDataItem.get("price") + "','" + objJSDataItem.get("sum_amount") + "', '" + whcode + "', '" + locationcode + "',  '" + branchcode + "', '" + usercode + "');";
                Statement __stmtDetail;

                __stmtDetail = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtDetail.executeUpdate(_queryDetail);
                __stmtDetail.close();
            }
            if (details_add.length() > 0) {
                String _queryAdd = "insert into ic_trans(trans_flag, trans_type, doc_no, doc_date, doc_time, branch_code, creator_code,wh_from,location_from,remark,doc_format_code) values(66,3,'" + doc_no_add + "','" + docdate + "', '" + doctime + "','" + branchcode + "','" + usercode + "','" + whcode + "','" + locationcode + "','" + remark + "','IAR')";
                Statement __stmtAdd;
                __stmtAdd = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtAdd.executeUpdate(_queryAdd);
                __stmtAdd.close();
            }
            if (details_minus.length() > 0) {
                String _queryMinus = "insert into ic_trans(trans_flag, trans_type, doc_no, doc_date, doc_time, branch_code, creator_code,wh_from,location_from,remark,doc_format_code) values(68,3,'" + doc_no_minus + "','" + docdate + "', '" + doctime + "','" + branchcode + "','" + usercode + "','" + whcode + "','" + locationcode + "','" + remark + "','ISP')";
                Statement __stmtMinus;
                __stmtMinus = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtMinus.executeUpdate(_queryMinus);
                __stmtMinus.close();
            }
            String queryProcess = "insert into process (process_name, wherein) select 'IC', item_code from ic_trans_detail where doc_no in ('" + doc_no_add + "','" + doc_no_minus + "')";
            Statement __stmtProcess;
            __stmtProcess = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmtProcess.executeUpdate(queryProcess);
            __stmtProcess.close();

            String __strQUERY1 = "update msc_cart set status=5,doc_no_add='" + doc_no_add + "',doc_no_minus='" + doc_no_minus + "',user_approve='" + usercode + "',approve_datetime='now()' WHERE doc_no ='" + docno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/sendCart")
    public Response sendCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "update  msc_cart set status=1 WHERE doc_no ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/sendSubCart")
    public Response sendSubCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERYsub = "select * from msc_cart_sub_detail where doc_no='" + strDocno + "'";

            Statement __stmtSub = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmtSub.executeQuery(__strQUERYsub);
            while (__rsHead.next()) {

                String __strQUERY3 = "update msc_cart_detail set qty=" + __rsHead.getInt("qty") + "  WHERE doc_no =(select doc_ref from msc_cart_sub where doc_no = '" + strDocno + "' limit 1) and barcode = '" + __rsHead.getString("barcode") + "' and item_code = '" + __rsHead.getString("item_code") + "' and unit_code = '" + __rsHead.getString("unit_code") + "' ;";
                Statement __stmt3;
                __stmt3 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmt3.executeUpdate(__strQUERY3);
                __stmt3.close();
            }
            __rsHead.close();
            __stmtSub.close();
            String __strQUERY1 = "update  msc_cart_sub set status=1 WHERE doc_no ='" + strDocno + "' ;";
            Statement __stmt1;
            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();

            String __strQUERY2 = "update msc_cart set status=3 WHERE doc_no =(select doc_ref from msc_cart_sub where doc_no = '" + strDocno + "') ;";
            Statement __stmt2;
            __stmt2 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt2.executeUpdate(__strQUERY2);
            __stmt2.close();

            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/deleteCart")
    public Response deleteCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "delete FROM msc_cart WHERE doc_no ='" + strDocno + "' ;"
                    + " delete FROM msc_cart_detail WHERE doc_no ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/deleteCartDoc")
    public Response deleteCartDoc(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "delete FROM msc_cart WHERE doc_no ='" + strDocno + "' ;"
                    + " delete FROM msc_cart_detail WHERE doc_no ='" + strDocno + "' ;"
                    + " delete FROM msc_cart_sub WHERE doc_ref ='" + strDocno + "' ;";
            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/deleteTagCard")
    public Response deleteTagCard(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "delete FROM stc_tag_card WHERE tag_code ='" + strDocno + "' ;";

            System.out.println("__strQUERY1");
            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();
            __conn.close();

            __objResponse.put("success", true);
            __objResponse.put("data", new JSONArray());
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCountSheetList")
    public Response getCountSheetList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("fromdate") String strFromdate,
            @QueryParam("todate") String strTodate,
            @QueryParam("whcode") String strWhcode,
            @QueryParam("locationcode") String strLocation) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String _where = "";
            if (!strDocno.isEmpty() && strDocno != "") {
                _where += " and (doc_no like '%" + strDocno + "%' or wh_code like '%" + strDocno + "%' or creator_code like '%" + strDocno + "%'  or user_code like '%" + strDocno + "%' ) ";
            }

            if (!strBranchcode.isEmpty() && strBranchcode != "") {
                _where += " and branch_code='" + strBranchcode + "' ";
            }

            if (!strWhcode.isEmpty() && strWhcode != "") {
                _where += " and wh_code='" + strWhcode + "' ";
            }

            if (!strLocation.isEmpty() && strLocation != "") {
                _where += " and location_code='" + strLocation + "' ";
            }

            if (!strFromdate.isEmpty() && strFromdate != "" && !strTodate.isEmpty() && strTodate != "") {
                _where += " and doc_date between '" + strFromdate + "' and '" + strTodate + "' ";
            }

            String __strQUERY1 = "SELECT *,(select name_1 from erp_branch_list where code=branch_code limit 1 ) as branch_name,(select name_1 from erp_user where upper(code)=upper(creator_code) limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_detail where msc_cart_detail.doc_no=msc_cart.doc_no) as item_count,carts FROM msc_cart where status=0 and is_merge = 0  " + _where + " ORDER BY create_datetime desc limit 1000";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("user_count", __rsHead.getString("user_code"));
                obj.put("carts", __rsHead.getString("carts"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCartList")
    public Response getCartList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("transflag") String strTransFlag,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String strSearchSafe = strSearch == null ? "" : strSearch.trim().replace("'", "''");
            String strSearchWhere = "";
            if (strSearchSafe.length() > 0) {
                strSearchWhere = " and (upper(doc_no) like upper('%" + strSearchSafe + "%')"
                        + " or upper(coalesce(remark,'')) like upper('%" + strSearchSafe + "%')"
                        + " or upper(creator_code) like upper('%" + strSearchSafe + "%')"
                        + " or upper((select name_1 from erp_user where code=creator_code limit 1)) like upper('%" + strSearchSafe + "%'))";
            }

            String __strQUERY1 = "SELECT *,(select name_1 from ap_supplier where code=cust_code limit 1 ) as cust_name,(select name_1 from erp_user where code=creator_code limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_to limit 1 ) as to_wh_name,(select name_1 from ic_shelf where code=location_to and whcode=wh_to limit 1 ) as to_location_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_detail where msc_cart_detail.doc_no=msc_cart.doc_no) as item_count,carts FROM msc_cart where  is_merge = 0 and branch_code='" + strBranchcode + "' and trans_flag = '" + strTransFlag + "' " + strSearchWhere + " ORDER BY status asc,doc_date desc,doc_time desc limit 200";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("wh_to", __rsHead.getString("wh_to"));
                obj.put("wh_to_name", __rsHead.getString("to_wh_name"));
                obj.put("location_to", __rsHead.getString("location_to"));
                obj.put("location_to_name", __rsHead.getString("to_location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("user_approve"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("cust_code", __rsHead.getString("cust_code"));
                obj.put("cust_name", __rsHead.getString("cust_name"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCartSubList")
    public Response getCartSubList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("branchcode") String strBranchcode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from erp_user where upper(code)=upper(creator_code) limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_sub_detail where msc_cart_sub_detail.doc_no=msc_cart_sub.doc_no) as item_count,carts FROM msc_cart_sub where status = 0 and branch_code='" + strBranchcode + "' ORDER BY create_datetime desc limit 1000";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("doc_ref", __rsHead.getString("doc_ref"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCartSubSheetList")
    public Response getCartSubSheetList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("fromdate") String strFromdate,
            @QueryParam("todate") String strTodate) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            String _where = "";
            if (!strDocno.isEmpty() && strDocno != "") {
                _where += " and (doc_no like '%" + strDocno + "%' or wh_code like '%" + strDocno + "%' or creator_code like '%" + strDocno + "%' or user_code like '%" + strDocno + "%' ) ";
            }

            if (!strBranchcode.isEmpty() && strBranchcode != "") {
                _where += " and branch_code='" + strBranchcode + "' ";
            }

            if (!strFromdate.isEmpty() && strFromdate != "" && !strTodate.isEmpty() && strTodate != "") {
                _where += " and doc_date between '" + strFromdate + "' and '" + strTodate + "' ";
            }

            String __strQUERY1 = "SELECT *,(select name_1 from erp_branch_list where code=branch_code limit 1 ) as branch_name,(select name_1 from erp_user where upper(code)=upper(creator_code) limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_sub_detail where msc_cart_sub_detail.doc_no=msc_cart_sub.doc_no) as item_count,carts FROM msc_cart_sub where status = 0 " + _where + " ORDER BY create_datetime desc limit 1000";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("doc_ref", __rsHead.getString("doc_ref"));
                obj.put("user_count", __rsHead.getString("user_code"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocApproveList")
    public Response getDocApproveList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("fromdate") String strFromdate,
            @QueryParam("todate") String strTodate) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String _where = "";
            if (!strDocno.isEmpty() && strDocno != "") {
                _where += " and (sc.user_code like '%" + strDocno + "%' or sc.doc_no like '%" + strDocno + "%' or sc.wh_code like '%" + strDocno + "%'or ic.name_1 like '%" + strDocno + "%' or sc.creator_code like '%" + strDocno + "%' or erp.name_1 like '%" + strDocno + "%' or br.name_1 like '%" + strDocno + "%' ) ";
            }

            if (!strBranchcode.isEmpty() && strBranchcode != "") {
                _where += " and sc.branch_code='" + strBranchcode + "' ";
            }

            if (!strFromdate.isEmpty() && strFromdate != "" && !strTodate.isEmpty() && strTodate != "") {
                _where += " and sc.doc_date between '" + strFromdate + "' and '" + strTodate + "' ";
            }

            String __strQUERY1 = "SELECT sc.doc_no,sc.user_code,sc.doc_date,sc.doc_time,sc.wh_code,sc.location_code,sc.branch_code,sc.creator_code,sc.remark,sc.status,sc.is_merge,sc.is_approve,sc.approve_code,sc.approve_date_time,sc.create_datetime,sc.carts,erp.name_1 as creator_name,br.name_1 as branch_name,ic.name_1 as wh_name,\n"
                    + "(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) \n"
                    + "from msc_cart_detail where msc_cart_detail.doc_no=sc.doc_no) as item_count,carts FROM msc_cart sc left join erp_user erp on upper(erp.code) = upper(creator_code) left join ic_warehouse ic on ic.code = sc.wh_code left join erp_branch_list br on br.code = sc.branch_code where sc.status in (4) " + _where + " ORDER BY create_datetime desc limit 1000";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("user_count", __rsHead.getString("user_code"));
                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocHistoryList")
    public Response getDocHistoryList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("fromdate") String strFromdate,
            @QueryParam("todate") String strTodate,
            @QueryParam("status") String strStatus,
            @QueryParam("whcode") String strWhcode,
            @QueryParam("locationcode") String strLocation) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String _where = "";
            if (!strDocno.isEmpty() && strDocno != "") {
                _where += " and (erp2.name_1 like '%" + strDocno + "%' or  sc.user_approve like '%" + strDocno + "%' or sc.user_code like '%" + strDocno + "%' or sc.doc_no like '%" + strDocno + "%' or sc.wh_code like '%" + strDocno + "%'or ic.name_1 like '%" + strDocno + "%' or sc.creator_code like '%" + strDocno + "%' or erp.name_1 like '%" + strDocno + "%' or br.name_1 like '%" + strDocno + "%' ) ";
            }

            if (!strBranchcode.isEmpty() && strBranchcode != "") {
                _where += " and sc.branch_code='" + strBranchcode + "' ";
            }

            if (!strWhcode.isEmpty() && strWhcode != "") {
                _where += " and sc.wh_code='" + strWhcode + "' ";
            }

            if (!strLocation.isEmpty() && strLocation != "") {
                _where += " and sc.location_code='" + strLocation + "' ";
            }

            if (!strFromdate.isEmpty() && strFromdate != "" && !strTodate.isEmpty() && strTodate != "") {
                _where += " and sc.doc_date between '" + strFromdate + "' and '" + strTodate + "' ";
            }
            if (!strStatus.isEmpty() && strStatus != "") {
                if (strStatus.equals("0")) {
                    _where += " and sc.status='" + strStatus + "' and sc.is_merge = 0 ";
                } else {
                    _where += " and sc.status='" + strStatus + "' ";
                }

            }
            String __strQUERY1 = "SELECT sc.roworder,sc.user_approve,erp2.name_1 as approve_name,sc.user_code,sc.doc_no,sc.doc_no_minus,sc.doc_no_add,sc.doc_date,sc.doc_time,sc.wh_code,sc.location_code,sc.branch_code,sc.creator_code,sc.remark,sc.status,sc.is_merge,sc.is_approve,sc.approve_code,sc.approve_date_time,sc.create_datetime,sc.carts,erp.name_1 as creator_name,br.name_1 as branch_name,ic.name_1 as wh_name,\n"
                    + "(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) \n"
                    + "from msc_cart_detail where msc_cart_detail.doc_no=sc.doc_no) as item_count,carts FROM msc_cart sc left join erp_user erp on upper(erp.code) = upper(creator_code) left join erp_user erp2 on upper(erp2.code) = upper(sc.user_approve) left join ic_warehouse ic on ic.code = sc.wh_code left join erp_branch_list br on br.code = sc.branch_code where 1=1 " + _where + " ORDER BY create_datetime desc limit 1000";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("roworder", __rsHead.getString("roworder"));
                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("doc_no_add", __rsHead.getString("doc_no_add"));
                obj.put("doc_no_minus", __rsHead.getString("doc_no_minus"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("user_count", __rsHead.getString("user_code"));
                obj.put("user_approve", __rsHead.getString("user_approve"));
                obj.put("approve_name", __rsHead.getString("approve_name"));
                String __strQUERYsub = "select doc_no from msc_cart_sub where doc_ref = '" + __rsHead.getString("doc_no") + "' order by doc_no asc";
                System.out.println(__strQUERYsub);
                Statement __stmtsub = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet __rsHeadsub = __stmtsub.executeQuery(__strQUERYsub);
                JSONArray jsarrSub = new JSONArray();
                while (__rsHeadsub.next()) {
                    JSONObject objSub = new JSONObject();
                    objSub.put("doc_no", __rsHeadsub.getString("doc_no"));
                    jsarrSub.put(objSub);
                }
                obj.put("cart_sub", jsarrSub);
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocWailList")
    public Response getDocWailList(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocno,
            @QueryParam("branchcode") String strBranchcode,
            @QueryParam("fromdate") String strFromdate,
            @QueryParam("todate") String strTodate) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String _where = "";
            if (!strDocno.isEmpty() && strDocno != "") {
                _where += " and (sc.user_code like '%" + strDocno + "%' or sc.doc_no like '%" + strDocno + "%' or sc.wh_code like '%" + strDocno + "%'or ic.name_1 like '%" + strDocno + "%' or sc.creator_code like '%" + strDocno + "%' or erp.name_1 like '%" + strDocno + "%' or br.name_1 like '%" + strDocno + "%' ) ";
            }

            if (!strBranchcode.isEmpty() && strBranchcode != "") {
                _where += " and sc.branch_code='" + strBranchcode + "' ";
            }

            if (!strFromdate.isEmpty() && strFromdate != "" && !strTodate.isEmpty() && strTodate != "") {
                _where += " and sc.doc_date between '" + strFromdate + "' and '" + strTodate + "' ";
            }

            String __strQUERY1 = "SELECT sc.doc_no,sc.user_code,sc.doc_date,sc.doc_time,sc.wh_code,sc.location_code,sc.branch_code,sc.creator_code,sc.remark,sc.status,sc.is_merge,sc.is_approve,sc.approve_code,sc.approve_date_time,sc.create_datetime,sc.carts,erp.name_1 as creator_name,br.name_1 as branch_name,ic.name_1 as wh_name,\n"
                    + "(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) \n"
                    + "from msc_cart_detail where msc_cart_detail.doc_no=sc.doc_no) as item_count,carts FROM msc_cart sc left join erp_user erp on upper(erp.code) = upper(creator_code) left join ic_warehouse ic on ic.code = sc.wh_code left join erp_branch_list br on br.code = sc.branch_code where sc.status in (1,3,6) " + _where + " ORDER BY create_datetime desc limit 1000";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("user_count", __rsHead.getString("user_code"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocData")
    public Response getDocData(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,coalesce((select count(doc_no) from msc_cart_sub where msc_cart_sub.doc_ref = msc_cart.doc_no),0) as sub_cart_count,(select name_1 from erp_branch_list where code=branch_code limit 1 ) as branch_name,(select name_1 from erp_user where upper(code)=upper(creator_code) limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_detail where msc_cart_detail.doc_no=msc_cart.doc_no) as item_count,carts FROM msc_cart where doc_no = '" + strDocNo + "'";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));
                obj.put("sub_cart_count", __rsHead.getString("sub_cart_count"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocSubData")
    public Response getDocSubData(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from erp_branch_list where code=branch_code limit 1 ) as branch_name,(select name_1 from erp_user where upper(code)=upper(creator_code) limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_sub_detail where msc_cart_sub_detail.doc_no=msc_cart_sub.doc_no) as item_count,carts FROM msc_cart_sub where doc_no = '" + strDocNo + "'";
            System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_time", __rsHead.getString("doc_time"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("branch_code", __rsHead.getString("branch_code"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("creator_code", __rsHead.getString("creator_code"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("status", __rsHead.getInt("status"));
                obj.put("is_merge", __rsHead.getInt("is_merge"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("item_count", __rsHead.getInt("item_count"));
                obj.put("approve_code", __rsHead.getString("approve_code"));
                obj.put("approve_date_time", __rsHead.getString("approve_date_time"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("carts", __rsHead.getString("carts"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocSubDetail")
    public Response getDocSubDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from ic_inventory where code = item_code limit 1 ) as item_name,coalesce((select case when price_2 = '' then '0' else price_2 end from ic_inventory_price_formula where ic_inventory_price_formula.ic_code = msc_cart_sub_detail.item_code and ic_inventory_price_formula.unit_code = msc_cart_sub_detail.unit_code and sale_type = 0 limit 1),'0') as price ,\n"
                    + " coalesce((select sum(balance_qty) as balance_qty from sml_ic_function_stock_balance_warehouse_location((select doc_date from msc_cart_sub where msc_cart_sub.doc_no = msc_cart_sub_detail.doc_no limit 1), item_code,wh_code,location_code) where ic_unit_code = unit_code),0)as event_balance_qty"
                    + " FROM msc_cart_sub_detail where doc_no ='" + strDocNo + "' order by line_number asc";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("roworder", __rsHead.getString("roworder"));
                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("qty", __rsHead.getFloat("qty"));
                obj.put("balance_qty", __rsHead.getFloat("event_balance_qty"));
                obj.put("diff_qty", (__rsHead.getFloat("qty") - __rsHead.getFloat("event_balance_qty")));
                obj.put("is_approve", __rsHead.getFloat("is_approve"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("location", __rsHead.getString("location"));
                obj.put("is_no_stock", __rsHead.getInt("is_no_stock"));
                obj.put("line_number", __rsHead.getInt("line_number"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getDocDetail")
    public Response getDocDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from ic_inventory where code = item_code limit 1 ) as item_name,coalesce((select case when price_2 = '' then '0' else price_2 end from ic_inventory_price_formula where ic_inventory_price_formula.ic_code = msc_cart_detail.item_code and ic_inventory_price_formula.unit_code = msc_cart_detail.unit_code and sale_type = 0 limit 1),'0') as price ,\n"
                    + " balance_qty,remark "
                    + " FROM msc_cart_detail where doc_no ='" + strDocNo + "' order by line_number asc";
            //System.out.println("__strQUERY1 "+__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("roworder", __rsHead.getString("roworder"));
                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("qty", __rsHead.getFloat("qty"));
                obj.put("balance_qty", __rsHead.getFloat("balance_qty"));
                obj.put("diff_qty", (__rsHead.getFloat("qty") - __rsHead.getFloat("balance_qty")));
                obj.put("is_approve", __rsHead.getFloat("is_approve"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("sum_amount", __rsHead.getFloat("qty") * __rsHead.getFloat("price"));
                obj.put("remark", __rsHead.getString("remark"));
                obj.put("location", __rsHead.getString("location"));
                obj.put("is_no_stock", __rsHead.getInt("is_no_stock"));
                obj.put("line_number", __rsHead.getInt("line_number"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getReportStockZero")
    public Response getReportStockZero(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String where_doc = "";
            String[] docsplit = strDocNo.split(",");

            for (int i = 0; i < docsplit.length; i++) {
                if (i == 0) {
                    where_doc += "'" + docsplit[i] + "'";
                } else {
                    where_doc += ",'" + docsplit[i] + "'";
                }
            }

            String __strQUERY1 = "WITH selected_docs AS (\n"
                    + "    SELECT doc_no\n"
                    + "    FROM msc_cart\n"
                    + "    WHERE roworder IN (" + where_doc + ")\n"
                    + "),\n"
                    + "item_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS item_code,\n"
                    + "        name_1 AS item_name,\n"
                    + "        group_main,\n"
                    + "        group_sub\n"
                    + "    FROM ic_inventory\n"
                    + "),\n"
                    + "price_details AS (\n"
                    + "    SELECT \n"
                    + "        ic_code AS item_code,\n"
                    + "        unit_code,\n"
                    + "        COALESCE(NULLIF(price_2, ''), '0') AS price\n"
                    + "    FROM ic_inventory_price_formula\n"
                    + "    WHERE sale_type = 0\n"
                    + "),\n"
                    + "warehouse_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS wh_code,\n"
                    + "        name_1 AS wh_name\n"
                    + "    FROM ic_warehouse\n"
                    + "),\n"
                    + "shelf_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS location_code,\n"
                    + "        whcode AS wh_code,\n"
                    + "        name_1 AS location_name\n"
                    + "    FROM ic_shelf\n"
                    + ")\n"
                    + "SELECT \n"
                    + "    d.wh_code,\n"
                    + "    d.location, "
                    + "    d.location_code,\n"
                    + "    SUM(d.qty) AS qty,\n"
                    + "    SUM(d.balance_qty) AS balance_qty,\n"
                    + "    d.item_code,\n"
                    + "    d.unit_code,\n"
                    + "    i.item_name,\n"
                    + "    i.group_main,\n"
                    + "    i.group_sub,\n"
                    + "    COALESCE(p.price, '0') AS price,\n"
                    + "    w.wh_name,\n"
                    + "    s.location_name\n"
                    + "FROM \n"
                    + "    msc_cart_detail d\n"
                    + "LEFT JOIN selected_docs sd ON d.doc_no = sd.doc_no\n"
                    + "LEFT JOIN item_details i ON i.item_code = d.item_code\n"
                    + "LEFT JOIN price_details p ON p.item_code = d.item_code AND p.unit_code = d.unit_code\n"
                    + "LEFT JOIN warehouse_details w ON w.wh_code = d.wh_code\n"
                    + "LEFT JOIN shelf_details s ON s.location_code = d.location_code AND s.wh_code = d.wh_code\n"
                    + "WHERE \n"
                    + "    sd.doc_no IS NOT NULL\n"
                    + "    AND d.is_no_stock = 1\n"
                    + "GROUP BY \n"
                    + "    d.wh_code, \n"
                    + "    d.location_code, \n"
                    + "    d.item_code, \n"
                    + "    d.unit_code, \n"
                    + "    d.location, \n"
                    + "    i.item_name, \n"
                    + "    i.group_main, \n"
                    + "    i.group_sub, \n"
                    + "    p.price, \n"
                    + "    w.wh_name, \n"
                    + "    s.location_name\n"
                    + "ORDER BY \n"
                    + "    d.item_code ASC;";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("qty", __rsHead.getFloat("qty"));
                obj.put("balance_qty", __rsHead.getFloat("balance_qty"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("sum_amount", __rsHead.getFloat("qty") * __rsHead.getFloat("price"));
                obj.put("location", __rsHead.getString("location"));
                obj.put("group_main", __rsHead.getString("group_main"));
                obj.put("group_sub", __rsHead.getString("group_sub"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getReportStockAdjust")
    public Response getReportStockAdjust(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String where_doc = "";
            String[] docsplit = strDocNo.split(",");

            for (int i = 0; i < docsplit.length; i++) {
                if (i == 0) {
                    where_doc += "'" + docsplit[i] + "'";
                } else {
                    where_doc += ",'" + docsplit[i] + "'";
                }
            }

            String __strQUERY1 = "SELECT \n"
                    + "    itd.doc_date,\n"
                    + "    itd.doc_no,\n"
                    + "    itd.item_code,\n"
                    + "    itd.unit_code,\n"
                    + "    itd.item_name,\n"
                    + "    COALESCE((SELECT name_1 FROM erp_user WHERE code = itd.creator_code), '') AS creator_name,\n"
                    + "    COALESCE((SELECT group_main FROM ic_inventory WHERE code = itd.item_code), '') AS group_main,\n"
                    + "    COALESCE((SELECT group_sub FROM ic_inventory WHERE code = itd.item_code), '') AS group_sub,\n"
                    + "(SELECT name_1 FROM erp_branch_list WHERE code = itd.branch_code LIMIT 1) AS branch_name,\n"
                    + "    (SELECT name_1 FROM ic_warehouse WHERE code = itd.wh_code LIMIT 1) AS wh_name,\n"
                    + "    (SELECT name_1 FROM ic_shelf WHERE code = itd.shelf_code AND whcode = itd.wh_code LIMIT 1) AS location_name,\n"
                    + "    itd.price,\n"
                    + "    CASE \n"
                    + "        WHEN itd.doc_no = sc.doc_no_minus THEN -itd.qty\n"
                    + "        ELSE itd.qty\n"
                    + "    END AS qty,\n"
                    + "    itd.price * \n"
                    + "        CASE \n"
                    + "            WHEN itd.doc_no = sc.doc_no_minus THEN -itd.qty\n"
                    + "            ELSE itd.qty\n"
                    + "        END AS sum_amount,\n"
                    + "    CASE \n"
                    + "        WHEN itd.doc_no = sc.doc_no_add THEN 'ปรับปรุงเพิ่ม'\n"
                    + "        WHEN itd.doc_no = sc.doc_no_minus THEN 'ปรับปรุงลด'\n"
                    + "        ELSE NULL\n"
                    + "    END AS doc_type\n"
                    + "FROM \n"
                    + "    ic_trans_detail itd\n"
                    + "JOIN \n"
                    + "    msc_cart sc ON itd.doc_no = sc.doc_no_add OR itd.doc_no = sc.doc_no_minus\n"
                    + "WHERE \n"
                    + "    sc.roworder in (" + where_doc + ")\n"
                    + "ORDER BY \n"
                    + "    itd.doc_date, itd.doc_no;";
            //System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();
                obj.put("doc_date", __rsHead.getString("doc_date"));
                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("doc_type", __rsHead.getString("doc_type"));
                obj.put("creator_name", __rsHead.getString("creator_name"));
                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("group_main", __rsHead.getString("group_main"));
                obj.put("group_sub", __rsHead.getString("group_sub"));
                obj.put("branch_name", __rsHead.getString("branch_name"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("qty", __rsHead.getFloat("qty"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("sum_amount", __rsHead.getFloat("qty") * __rsHead.getFloat("price"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getReportNoCheck")
    public Response getReportNoCheck(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo,
            @QueryParam("datetime") String strDatetime,
            @QueryParam("whcode") String strWhcode,
            @QueryParam("locationcode") String strLocationCode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String where_doc = "";
            String[] docsplit = strDocNo.split(",");

            for (int i = 0; i < docsplit.length; i++) {
                if (i == 0) {
                    where_doc += "'" + docsplit[i] + "'";
                } else {
                    where_doc += ",'" + docsplit[i] + "'";
                }
            }

            String __strQUERY1 = "WITH \n"
                    + "price_details AS (\n"
                    + "    SELECT \n"
                    + "        ic_code,\n"
                    + "        unit_code,\n"
                    + "        COALESCE(NULLIF(price_0, ''), '0') AS price\n"
                    + "    FROM \n"
                    + "        ic_inventory_price_formula\n"
                    + "    WHERE \n"
                    + "        sale_type = 0\n"
                    + "),\n"
                    + "inventory_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS ic_code,\n"
                    + "        group_main,\n"
                    + "        group_sub\n"
                    + "    FROM \n"
                    + "        ic_inventory\n"
                    + "),\n"
                    + "warehouse_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS wh_code,\n"
                    + "        name_1 AS wh_name\n"
                    + "    FROM \n"
                    + "        ic_warehouse\n"
                    + "),\n"
                    + "shelf_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS location_code,\n"
                    + "        whcode AS warehouse_code,\n"
                    + "        name_1 AS location_name\n"
                    + "    FROM \n"
                    + "        ic_shelf\n"
                    + "),\n"
                    + "cart_details AS (\n"
                    + "    SELECT \n"
                    + "        d.item_code, \n"
                    + "        d.unit_code\n"
                    + "    FROM \n"
                    + "        msc_cart_detail d\n"
                    + "    JOIN \n"
                    + "        msc_cart c ON d.doc_no = c.doc_no\n"
                    + "    WHERE \n"
                    + "        c.roworder IN (" + where_doc + ")\n"
                    + ")\n"
                    + "SELECT \n"
                    + "    sb.ic_code,\n"
                    + "    sb.ic_name,\n"
                    + "    sb.ic_unit_code,\n"
                    + "    sb.balance_qty,\n"
                    + "    COALESCE(p.price, '0') AS price,\n"
                    + "    COALESCE(i.group_main, '') AS group_main,\n"
                    + "    COALESCE(i.group_sub, '') AS group_sub,\n"
                    + "    w.wh_name,\n"
                    + "    s.location_name\n"
                    + "FROM \n"
                    + "    sml_ic_function_stock_balance_warehouse_location('" + strDatetime + "', '', '" + strWhcode + "', '" + strLocationCode + "') sb\n"
                    + "LEFT JOIN price_details p \n"
                    + "    ON sb.ic_code = p.ic_code AND sb.ic_unit_code = p.unit_code\n"
                    + "LEFT JOIN inventory_details i \n"
                    + "    ON sb.ic_code = i.ic_code\n"
                    + "LEFT JOIN warehouse_details w \n"
                    + "    ON sb.warehouse = w.wh_code\n"
                    + "LEFT JOIN shelf_details s \n"
                    + "    ON sb.location = s.location_code AND sb.warehouse = s.warehouse_code\n"
                    + "LEFT JOIN cart_details cd \n"
                    + "    ON sb.ic_code = cd.item_code AND sb.ic_unit_code = cd.unit_code\n"
                    + "WHERE \n"
                    + "    sb.balance_qty != 0 \n"
                    + "    AND cd.item_code IS NULL \n"
                    + "ORDER BY \n"
                    + "    sb.ic_code ASC;";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("item_code", __rsHead.getString("ic_code"));
                obj.put("item_name", __rsHead.getString("ic_name"));
                obj.put("unit_code", __rsHead.getString("ic_unit_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("balance_qty", __rsHead.getFloat("balance_qty"));
                obj.put("sum_amount", __rsHead.getFloat("balance_qty") * __rsHead.getFloat("price"));
                obj.put("group_main", __rsHead.getString("group_main"));
                obj.put("group_sub", __rsHead.getString("group_sub"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getReportStockDiff")
    public Response getReportStockDiff(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo,
            @QueryParam("datetime") String strDatetime,
            @QueryParam("whcode") String strWhcode,
            @QueryParam("locationcode") String strLocationCode) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String where_doc = "";
            String[] docsplit = strDocNo.split(",");

            for (int i = 0; i < docsplit.length; i++) {
                if (i == 0) {
                    where_doc += "'" + docsplit[i] + "'";
                } else {
                    where_doc += ",'" + docsplit[i] + "'";
                }
            }

            String __strQUERY1 = "WITH \n"
                    + "selected_docs AS (\n"
                    + "    SELECT doc_no\n"
                    + "    FROM msc_cart\n"
                    + "    WHERE roworder IN (" + where_doc + ")\n"
                    + "),\n"
                    + "item_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS item_code,\n"
                    + "        name_1 AS item_name,\n"
                    + "        group_main,\n"
                    + "        group_sub\n"
                    + "    FROM ic_inventory\n"
                    + "),\n"
                    + "price_details AS (\n"
                    + "    SELECT \n"
                    + "        ic_code AS item_code,\n"
                    + "        unit_code,\n"
                    + "        COALESCE(NULLIF(price_0, ''), '0') AS price\n"
                    + "    FROM ic_inventory_price_formula\n"
                    + "    WHERE sale_type = 0\n"
                    + "),\n"
                    + "warehouse_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS wh_code,\n"
                    + "        name_1 AS wh_name\n"
                    + "    FROM ic_warehouse\n"
                    + "),\n"
                    + "shelf_details AS (\n"
                    + "    SELECT \n"
                    + "        code AS location_code,\n"
                    + "        whcode AS wh_code,\n"
                    + "        name_1 AS location_name\n"
                    + "    FROM ic_shelf\n"
                    + "),\n"
                    + "balance_details AS (\n"
                    + "    SELECT \n"
                    + "        d.item_code,\n"
                    + "        d.unit_code,\n"
                    + "        COALESCE(sb.balance_qty, 0) AS balance_qty\n"
                    + "    FROM msc_cart_detail d\n"
                    + "    LEFT JOIN LATERAL sml_ic_function_stock_balance_warehouse_location(\n"
                    + "        '" + strDatetime + "', \n"
                    + "        d.item_code, \n"
                    + "        '" + strWhcode + "', \n"
                    + "        '" + strLocationCode + "'\n"
                    + "    ) sb ON sb.ic_unit_code = d.unit_code\n"
                    + "    WHERE d.doc_no IN (SELECT doc_no FROM selected_docs)\n"
                    + "    GROUP BY d.item_code, d.unit_code, sb.balance_qty\n"
                    + ")\n"
                    + "SELECT \n"
                    + "    d.location,\n"
                    + "    d.wh_code,\n"
                    + "    d.location_code,\n"
                    + "    SUM(d.qty) AS qty,\n"
                    + "    d.item_code,\n"
                    + "    d.unit_code,\n"
                    + "    i.item_name,\n"
                    + "    i.group_main,\n"
                    + "    i.group_sub,\n"
                    + "    COALESCE(p.price, '0') AS price,\n"
                    + "    w.wh_name,\n"
                    + "    s.location_name,\n"
                    + "    b.balance_qty\n"
                    + "FROM \n"
                    + "    msc_cart_detail d\n"
                    + "LEFT JOIN selected_docs sd ON d.doc_no = sd.doc_no\n"
                    + "LEFT JOIN item_details i ON i.item_code = d.item_code\n"
                    + "LEFT JOIN price_details p ON p.item_code = d.item_code AND p.unit_code = d.unit_code\n"
                    + "LEFT JOIN warehouse_details w ON w.wh_code = d.wh_code\n"
                    + "LEFT JOIN shelf_details s ON s.location_code = d.location_code AND s.wh_code = d.wh_code\n"
                    + "LEFT JOIN balance_details b ON b.item_code = d.item_code AND b.unit_code = d.unit_code\n"
                    + "WHERE \n"
                    + "    sd.doc_no IS NOT NULL\n"
                    + "GROUP BY \n"
                    + "    d.location, \n"
                    + "    d.wh_code, \n"
                    + "    d.location_code, \n"
                    + "    d.item_code, \n"
                    + "    d.unit_code, \n"
                    + "    i.item_name, \n"
                    + "    i.group_main, \n"
                    + "    i.group_sub, \n"
                    + "    p.price, \n"
                    + "    w.wh_name, \n"
                    + "    s.location_name, \n"
                    + "    b.balance_qty\n"
                    + "ORDER BY \n"
                    + "    d.item_code ASC;";
            //System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_name", __rsHead.getString("wh_name"));
                obj.put("location_name", __rsHead.getString("location_name"));
                obj.put("qty", __rsHead.getFloat("qty"));
                obj.put("price", __rsHead.getFloat("price"));
                obj.put("balance_qty", __rsHead.getFloat("balance_qty"));
                obj.put("diff_qty", (__rsHead.getFloat("qty") - __rsHead.getFloat("balance_qty")));
                obj.put("sum_amount", (__rsHead.getFloat("qty") - __rsHead.getFloat("balance_qty")) * __rsHead.getFloat("price"));
                obj.put("location", __rsHead.getString("location"));
                obj.put("group_main", __rsHead.getString("group_main"));
                obj.put("group_sub", __rsHead.getString("group_sub"));
                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCartSubDetail")
    public Response getCartSubDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from ic_inventory where code = item_code limit 1 ) as item_name FROM msc_cart_sub_detail where doc_no = '" + strDocNo + "' order by line_number asc ";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("qty", __rsHead.getInt("qty"));
                obj.put("balance_qty", __rsHead.getInt("balance_qty"));
                obj.put("diff_qty", __rsHead.getInt("diff_qty"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/getCartDetail")
    public Response getCartDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("docno") String strDocNo) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from ic_inventory where code = item_code limit 1 ) as item_name FROM msc_cart_detail where doc_no = '" + strDocNo + "' order by line_number asc ";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery(__strQUERY1);
            JSONArray jsarr = new JSONArray();
            while (__rsHead.next()) {

                JSONObject obj = new JSONObject();

                obj.put("doc_no", __rsHead.getString("doc_no"));
                obj.put("barcode", __rsHead.getString("barcode"));
                obj.put("item_code", __rsHead.getString("item_code"));
                obj.put("item_name", __rsHead.getString("item_name"));
                obj.put("unit_code", __rsHead.getString("unit_code"));
                obj.put("wh_code", __rsHead.getString("wh_code"));
                obj.put("location_code", __rsHead.getString("location_code"));
                obj.put("qty", __rsHead.getInt("qty"));
                obj.put("balance_qty", __rsHead.getInt("balance_qty"));
                obj.put("diff_qty", __rsHead.getInt("diff_qty"));
                obj.put("is_approve", __rsHead.getInt("is_approve"));
                obj.put("is_no_stock", __rsHead.getInt("is_no_stock"));
                obj.put("line_number", __rsHead.getInt("line_number"));
                obj.put("create_datetime", __rsHead.getString("create_datetime"));

                jsarr.put(obj);
            }

            __objResponse.put("success", true);
            __objResponse.put("data", jsarr);
            __rsHead.close();
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            return Response.status(400).entity("{ERROR: " + ex.getMessage() + "}").build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/mergeCart")
    public Response mergeCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        String docdate = "";
        String doctime = "";
        String carts = "";
        String user_code = "";
        String cust_code = "";
        String trans_flag = "";
        String whto = "";
        String locationto = "";
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";

            branchcode = objJSData.has("branchcode") ? objJSData.getString("branchcode") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            whto = objJSData.has("whto") ? objJSData.getString("whto") : "";
            locationto = objJSData.has("locationto") ? objJSData.getString("locationto") : "";
            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("usercode") ? objJSData.getString("usercode") : "";
            docdate = objJSData.has("docdate") ? objJSData.getString("docdate") : "";
            doctime = objJSData.has("doctime") ? objJSData.getString("doctime") : "";
            carts = objJSData.has("carts") ? objJSData.getString("carts") : "";
            user_code = objJSData.has("usercount") ? objJSData.getString("usercount") : "";
            cust_code = objJSData.has("custcode") ? objJSData.getString("custcode") : "";
            trans_flag = objJSData.has("transflag") ? objJSData.getString("transflag") : "0";

        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "insert into msc_cart (wh_to,location_to,cust_code,trans_flag,doc_no,branch_code,wh_code,location_code,creator_code,remark,doc_date,doc_time,carts,user_code) values ('" + whto + "','" + locationto + "','" + cust_code + "','" + trans_flag + "','" + docno + "','" + branchcode + "','" + whcode + "','" + locationcode + "','" + usercode + "','" + remark + "','" + docdate + "','" + doctime + "','" + carts + "','" + user_code + "')";
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);
            __stmt1.close();

            String[] cart = carts.split(",");
            String docNoIn = "";
            for (int i = 0; i < cart.length; i++) {
                if (i == 0) {
                    docNoIn = "'" + cart[i] + "'";
                } else {
                    docNoIn += ",'" + cart[i] + "'";
                }
                String __strQUERY2 = "update msc_cart set is_merge=1,carts='" + docno + "' where doc_no = '" + cart[i] + "'";
                Statement __stmt2 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmt2.executeUpdate(__strQUERY2);
                __stmt2.close();
            }

            String __strQUERYdetail = "insert into msc_cart_detail (doc_no,barcode,item_code,unit_code,wh_code,location_code,qty,balance_qty,diff_qty,is_approve,location,is_no_stock,line_number) "
                    + "select '" + docno + "',barcode,item_code,unit_code,'" + whcode + "','" + locationcode + "',sum(qty) as qty,MAX(balance_qty) AS balance_qty,MAX(balance_qty) - SUM(qty) AS diff_qty,0,STRING_AGG(COALESCE(NULLIF(location, ''), NULL), ', ') AS location,min(is_no_stock),MAX(line_number) from msc_cart_detail where doc_no in (" + docNoIn + ") group by item_code,unit_code,barcode ";
            System.out.println("__strQUERYdetail " + __strQUERYdetail);
            Statement __stmtdetail = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmtdetail.executeUpdate(__strQUERYdetail);
            __stmtdetail.close();

            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "success");

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/updateCartItems")
    public Response updateCartItems(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("doc_no") ? objJSData.getString("doc_no") : "";
            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            StringBuilder __strQUERYz = new StringBuilder();
            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                __strQUERYz.append("update msc_cart_detail set remark ='" + objJSDataItem.getString("remark") + "',qty='" + objJSDataItem.get("qty") + "',diff_qty='" + objJSDataItem.get("diff_qty") + "' where doc_no='" + docno + "' and roworder ='" + objJSDataItem.get("roworder") + "';");

            }
            System.out.println("" + __strQUERYz);
            Statement __stmtz;
            __stmtz = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmtz.executeUpdate(__strQUERYz.toString());
            __stmtz.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/updateDetailRemark")
    public Response updateDetailRemark(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String roworder = "";
        String remark = "";
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("doc_no") ? objJSData.getString("doc_no") : "";
            roworder = objJSData.has("roworder") ? objJSData.getString("roworder") : "";
            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "update msc_cart_detail set remark ='" + remark + "' where doc_no='" + docno + "' and roworder ='" + roworder + "'";
            //System.out.println(__strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/createCart")
    public Response createCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String whto = "";
        String locationto = "";
        String remark = "";
        String usercode = "";
        String user_code = "";
        String docdate = "";
        String doctime = "";
        String trans_flag = "";
        String cust_code = "";
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";

            branchcode = objJSData.has("branchcode") ? objJSData.getString("branchcode") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            docdate = objJSData.has("docdate") ? objJSData.getString("docdate") : "now()";
            doctime = objJSData.has("doctime") ? objJSData.getString("doctime") : "now()";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            whto = objJSData.has("whto") ? objJSData.getString("whto") : "";
            locationto = objJSData.has("locationto") ? objJSData.getString("locationto") : "";

            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("usercode") ? objJSData.getString("usercode") : "";
            user_code = objJSData.has("usercount") ? objJSData.getString("usercount") : "";
            trans_flag = objJSData.has("transflag") ? objJSData.getString("transflag") : "0";
            cust_code = objJSData.has("custcode") ? objJSData.getString("custcode") : "";
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "insert into msc_cart (wh_to,location_to,cust_code,trans_flag,doc_date,doc_time,doc_no,branch_code,wh_code,location_code,creator_code,remark,user_code) values ('" + whto + "','" + locationto + "','" + cust_code + "','" + trans_flag + "','" + docdate + "','" + doctime + "','" + docno + "','" + branchcode + "','" + whcode + "','" + locationcode + "','" + usercode + "','" + remark + "','" + user_code + "')";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/createSubCart")
    public Response createSubCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        String doc_ref = "";
        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";

            branchcode = objJSData.has("branchcode") ? objJSData.getString("branchcode") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("usercode") ? objJSData.getString("usercode") : "";
            doc_ref = objJSData.has("doc_ref") ? objJSData.getString("doc_ref") : "";
            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "insert into msc_cart_sub (doc_no,branch_code,wh_code,location_code,creator_code,remark,doc_ref) values ('" + docno + "','" + branchcode + "','" + whcode + "','" + locationcode + "','" + usercode + "','" + remark + "','" + doc_ref + "')";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            __stmt1.close();
            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                System.out.println("" + objJSDataItem);
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";
                String __strQUERYz = "insert into msc_cart_sub_detail (doc_no,barcode,item_code,unit_code,wh_code,location_code,qty,balance_qty,diff_qty,is_approve,location,is_no_stock,line_number) "
                        + "values ('" + docno + "','','" + objJSDataItem.getString("item_code") + "','" + objJSDataItem.getString("unit_code") + "','" + whcode + "','" + locationcode + "','" + objJSDataItem.getInt("qty") + "','" + objJSDataItem.get("balance_qty") + "','" + objJSDataItem.getInt("diff_qty") + "','" + objJSDataItem.getInt("is_approve") + "','" + location + "','" + objJSDataItem.getInt("is_no_stock") + "','" + objJSDataItem.getInt("line_number") + "');";
                System.out.println("" + __strQUERYz);
                Statement __stmtz;
                __stmtz = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtz.executeUpdate(__strQUERYz);
                __stmtz.close();
            }

            String __strQUERYUpdate = "update msc_cart set status=2 where doc_no = '" + doc_ref + "'";

            Statement __stmtx = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmtx.executeUpdate(__strQUERYUpdate);

            __stmtx.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "success");
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveTagCardCountSheet")
    public Response saveTagCardCountSheet(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String docdate = "";
        String doctime = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        String user_code = "";
        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("doc_no") ? objJSData.getString("doc_no") : "";
            docdate = objJSData.has("doc_date") ? objJSData.getString("doc_date") : "";
            doctime = objJSData.has("doc_time") ? objJSData.getString("doc_time") : "";

            branchcode = objJSData.has("branch_code") ? objJSData.getString("branch_code") : "";
            whcode = objJSData.has("wh_code") ? objJSData.getString("wh_code") : "";
            locationcode = objJSData.has("location_code") ? objJSData.getString("location_code") : "";
            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("creator_code") ? objJSData.getString("creator_code") : "";
            user_code = objJSData.has("usercount") ? objJSData.getString("usercount") : "";
            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }

        }
        StringBuilder _insert = new StringBuilder();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            _insert.append("insert into msc_cart (doc_date,doc_time,doc_no,branch_code,wh_code,location_code,creator_code,remark,user_code) values ('" + docdate + "','" + doctime + "','" + docno + "','" + branchcode + "','" + whcode + "','" + locationcode + "','" + usercode + "','" + remark + "','" + user_code + "');");
            String list_tag_code = "";
            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                System.out.println("" + objJSDataItem);
                _insert.append("insert into msc_cart_detail (doc_no,barcode,item_code,unit_code,wh_code,location_code,qty,balance_qty,diff_qty,is_approve,location,is_no_stock,line_number) "
                        + "values ('" + docno + "','" + objJSDataItem.getString("barcode") + "','" + objJSDataItem.getString("item_code") + "','" + objJSDataItem.getString("unit_code") + "','" + whcode + "','" + locationcode + "','" + objJSDataItem.getInt("qty") + "','" + objJSDataItem.get("balance_qty") + "','" + objJSDataItem.getInt("diff_qty") + "','" + objJSDataItem.getInt("is_approve") + "','" + objJSDataItem.getString("location") + "','" + objJSDataItem.getInt("is_no_stock") + "','" + objJSDataItem.getInt("line_number") + "');");
                if (i == 0) {
                    list_tag_code += "'" + objJSDataItem.getString("tag_code") + "'";
                } else {
                    list_tag_code += ",'" + objJSDataItem.getString("tag_code") + "'";
                }
            }
            _insert.append("update stc_tag_card set is_approve=1 where tag_code in (" + list_tag_code + ");");
            System.out.println(_insert.toString());
            PreparedStatement __stmt = __conn.prepareStatement(_insert.toString());
            int rowsAffected = __stmt.executeUpdate(); // Use executeUpdate() instead of executeQuery()
            __stmt.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "Inserted rows: " + rowsAffected);

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveCartDetail")
    public Response saveCartDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";

        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";

            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "delete from msc_cart_detail where doc_no='" + docno + "' ";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);

                String is_no_stock = objJSDataItem.optString("is_no_stock", "0");
                String line_number = objJSDataItem.optString("line_number", "0");
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";

                String __strQUERYz = "insert into msc_cart_detail (doc_no,barcode,item_code,unit_code,wh_code,location_code,qty,balance_qty,diff_qty,is_approve,location,is_no_stock,line_number) "
                        + "values ('" + docno + "','" + objJSDataItem.getString("barcode") + "','" + objJSDataItem.getString("item_code") + "','" + objJSDataItem.getString("unit_code") + "','" + whcode + "','" + locationcode + "','" + objJSDataItem.getInt("qty") + "','" + objJSDataItem.get("balance_qty") + "','" + objJSDataItem.getInt("diff_qty") + "','" + objJSDataItem.getInt("is_approve") + "','" + location + "','" + is_no_stock + "','" + line_number + "');";
                System.out.println("" + __strQUERYz);
                Statement __stmtz;
                __stmtz = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtz.executeUpdate(__strQUERYz);
                __stmtz.close();
            }

            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveTagCardCount")
    public Response saveTagCardCount(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String tag_code = "";
        String qty = "";
        String mode = "1";
        String locationcode = "";
        String remark = "";
        String creator_code = "";

        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            creator_code = objJSData.has("creator_code") ? objJSData.getString("creator_code") : "";
            tag_code = objJSData.has("tag_code") ? objJSData.getString("tag_code") : "";
            qty = objJSData.has("qty") ? objJSData.getString("qty") : "";
            mode = objJSData.has("mode") ? objJSData.getString("mode") : "";
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            StringBuilder _insert = new StringBuilder();
            if (mode.equals("1")) {
                _insert.append("update stc_tag_card set qty_1 = '" + qty + "',creator_code_1='" + creator_code + "',last_update_datetime_1='now()' where tag_code = '" + tag_code + "';");
            } else {
                _insert.append("update stc_tag_card set qty_2 = '" + qty + "',creator_code_2='" + creator_code + "',last_update_datetime_2='now()' where tag_code = '" + tag_code + "';");
            }
            _insert.append("UPDATE stc_tag_card\n"
                    + "SET "
                    + "    diff_qty = qty_1 - qty_2,\n"
                    + "    status = CASE\n"
                    + "        WHEN qty_1 = 0 AND qty_2 = 0 AND (creator_code_1 IS NULL OR creator_code_1 = '') AND (creator_code_2 IS NULL OR creator_code_2 = '') THEN 0\n"
                    + "        WHEN ((creator_code_1 IS NOT NULL AND creator_code_1 <> '') AND (creator_code_2 IS NULL OR creator_code_2 = '')) OR ((creator_code_1 IS NULL OR creator_code_1 = '') AND (creator_code_2 IS NOT NULL AND creator_code_2 <> '')) THEN 1\n"
                    + "        WHEN (creator_code_1 IS NOT NULL AND creator_code_1 <> '') AND (creator_code_2 IS NOT NULL AND creator_code_2 <> '')  and qty_1 - qty_2 != 0 THEN 2\n"
                    + "        WHEN (creator_code_1 IS NOT NULL AND creator_code_1 <> '') AND (creator_code_2 IS NOT NULL AND creator_code_2 <> '')  and qty_1 - qty_2 = 0 THEN 3\n"
                    + "    END \n"
                    + "where tag_code = '" + tag_code + "';");
            System.out.println(_insert.toString());
            PreparedStatement __stmt = __conn.prepareStatement(_insert.toString());
            int rowsAffected = __stmt.executeUpdate(); // Use executeUpdate() instead of executeQuery()
            __stmt.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "Inserted rows: " + rowsAffected);

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveImportRecount")
    public Response saveImportRecount(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String tag_code = "";
        String qty = "";
        String mode = "1";
        String locationcode = "";
        String remark = "";
        String creator_code = "";

        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            creator_code = objJSData.has("creator_code") ? objJSData.getString("creator_code") : "";
            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            StringBuilder _insert = new StringBuilder();

            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                _insert.append("update stc_tag_card set is_recount=1,qty_1 = '" + objJSDataItem.getString("qty_1") + "',creator_code_1='" + creator_code + "',last_update_datetime_1='now()',qty_2 = '" + objJSDataItem.getString("qty_2") + "',creator_code_2='" + creator_code + "',last_update_datetime_2='now()' where tag_code = '" + objJSDataItem.getString("tag_code") + "';");

            }
            _insert.append("UPDATE stc_tag_card\n"
                    + "SET "
                    + "    diff_qty = qty_1 - qty_2,\n"
                    + "    status = CASE\n"
                    + "        WHEN qty_1 = 0 AND qty_2 = 0 AND (creator_code_1 IS NULL OR creator_code_1 = '') AND (creator_code_2 IS NULL OR creator_code_2 = '') THEN 0\n"
                    + "        WHEN (creator_code_1 IS NOT NULL AND creator_code_1 <> '') AND (creator_code_2 IS NULL OR creator_code_2 = '') THEN 1\n"
                    + "        WHEN qty_1 - qty_2 != 0 THEN 2\n"
                    + "        WHEN qty_1 - qty_2 = 0 THEN 3\n"
                    + "    END \n"
                    + "where is_recount = 1 and is_approve = 0;");
            System.out.println(_insert.toString());
            PreparedStatement __stmt = __conn.prepareStatement(_insert.toString());
            int rowsAffected = __stmt.executeUpdate(); // Use executeUpdate() instead of executeQuery()
            __stmt.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "update rows: " + rowsAffected);

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveTagCardDetail")
    public Response saveTagCardDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";

        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            usercode = objJSData.has("creator_code") ? objJSData.getString("creator_code") : "";

            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            StringBuilder _insert = new StringBuilder();
            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";
                System.out.println("" + objJSDataItem);
                _insert.append("insert into stc_tag_card (barcode,item_code,unit_code,branch_code,wh_code,location_code,location,tag_code,qr_code_1,qr_code_2,creator_code) "
                        + "values ('" + objJSDataItem.getString("barcode") + "','" + objJSDataItem.getString("item_code") + "','" + objJSDataItem.getString("unit_code") + "','" + objJSDataItem.getString("branch_code") + "','" + objJSDataItem.getString("wh_code") + "','" + objJSDataItem.getString("location_code") + "','" + location + "','" + objJSDataItem.getString("tag_code") + "','" + objJSDataItem.getString("tag_code") + "-S','" + objJSDataItem.getString("tag_code") + "-E','" + usercode + "');");
            }
            PreparedStatement __stmt = __conn.prepareStatement(_insert.toString());
            int rowsAffected = __stmt.executeUpdate(); // Use executeUpdate() instead of executeQuery()
            __stmt.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "Inserted rows: " + rowsAffected);

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/deleteManyTagCode")
    public Response deleteManyTagCode(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String tagcard = "";

        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            tagcard = objJSData.has("tagcard") ? objJSData.getString("tagcard") : "";

        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));
            StringBuilder _insert = new StringBuilder();

            _insert.append("delete from stc_tag_card where tag_code in (" + tagcard + ")");

            PreparedStatement __stmt = __conn.prepareStatement(_insert.toString());
            int rowsAffected = __stmt.executeUpdate(); // Use executeUpdate() instead of executeQuery()
            __stmt.close();
            __conn.close();
            __objResponse.put("success", true);
            __objResponse.put("data", "delete rows: " + rowsAffected);

        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/saveCartSubDetail")
    public Response saveCartSubDetail(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String branchcode = "";
        String whcode = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        JSONArray items = new JSONArray();
        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            if (objJSData.has("details")) {
                items = objJSData.getJSONArray("details");
            }
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "delete from msc_cart_sub_detail where doc_no='" + docno + "' ";

            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            for (int i = 0; i < items.length(); i++) {
                JSONObject objJSDataItem = items.getJSONObject(i);
                System.out.println("" + objJSDataItem);
                String location = objJSDataItem.has("location") ? objJSDataItem.getString("location") : "";
                String is_no_stock = objJSDataItem.optString("is_no_stock", "0");
                String line_number = objJSDataItem.optString("line_number", "0");
                String __strQUERYz = "insert into msc_cart_sub_detail (doc_no,barcode,item_code,unit_code,wh_code,location_code,qty,balance_qty,diff_qty,is_approve,location,is_no_stock,line_number) "
                        + "values ('" + docno + "','','" + objJSDataItem.getString("item_code") + "','" + objJSDataItem.getString("unit_code") + "','" + whcode + "','" + locationcode + "','" + objJSDataItem.getInt("qty") + "','" + objJSDataItem.get("balance_qty") + "','" + objJSDataItem.getInt("diff_qty") + "','" + objJSDataItem.getInt("is_approve") + "','" + location + "','" + is_no_stock + "','" + line_number + "');";
                System.out.println("" + __strQUERYz);
                Statement __stmtz;
                __stmtz = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtz.executeUpdate(__strQUERYz);
                __stmtz.close();
            }

            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/updateCart")
    public Response updateCart(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            String data) {
        JSONObject __objResponse = new JSONObject();
        String docno = "";
        String whcode = "";
        String docdate = "";
        String doctime = "";
        String locationcode = "";
        String remark = "";
        String usercode = "";
        String branchcode = "";
        String usercount = "";
        String cust_code = "";
        String whto = "";
        String locationto = "";

        if (data != null) {
            JSONObject objJSData = new JSONObject(data);
            docno = objJSData.has("docno") ? objJSData.getString("docno") : "";
            docdate = objJSData.has("docdate") ? objJSData.getString("docdate") : "now()";
            doctime = objJSData.has("doctime") ? objJSData.getString("doctime") : "now()";
            branchcode = objJSData.has("branchcode") ? objJSData.getString("branchcode") : "";
            whcode = objJSData.has("whcode") ? objJSData.getString("whcode") : "";
            locationcode = objJSData.has("locationcode") ? objJSData.getString("locationcode") : "";
            whto = objJSData.has("whto") ? objJSData.getString("whto") : "";
            locationto = objJSData.has("locationto") ? objJSData.getString("locationto") : "";

            remark = objJSData.has("remark") ? objJSData.getString("remark") : "";
            usercode = objJSData.has("usercode") ? objJSData.getString("usercode") : "";
            usercount = objJSData.has("usercount") ? objJSData.getString("usercount") : "";
            cust_code = objJSData.has("custcode") ? objJSData.getString("custcode") : "";
        }

        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "update msc_cart set wh_to='" + whto + "',location_to='" + locationto + "',cust_code='" + cust_code + "', doc_date='" + docdate + "', doc_time='" + doctime + "', wh_code='" + whcode + "',user_code='" + usercount + "',location_code='" + locationcode + "',branch_code='" + branchcode + "',remark='" + remark + "' where doc_no = '" + docno + "'";
            System.out.println("__strQUERY1 " + __strQUERY1);
            Statement __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            __stmt1.executeUpdate(__strQUERY1);

            __objResponse.put("success", true);
            __objResponse.put("data", "success");
            __conn.close();
            __stmt1.close();
        } catch (Exception ex) {
            __objResponse.put("message", ex.getMessage());
            return Response.status(400).entity(String.valueOf(__objResponse)).build();
        }
        return Response.ok(String.valueOf(__objResponse), MediaType.APPLICATION_JSON).build();
    }

    public static JSONArray _convertResultSetIntoJSON(ResultSet resultSet) throws Exception {
        JSONArray jsonArray = new JSONArray();
        while (resultSet.next()) {
            int total_rows = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase();
                Object columnValue = resultSet.getObject(i + 1);
                // if value in DB is null, then we set it to default value
                if (columnValue == null) {
                    columnValue = "null";
                }
                /*
                Next if block is a hack. In case when in db we have values like price and price1 there's a bug in jdbc - 
                both this names are getting stored as price in ResulSet. Therefore when we store second column value,
                we overwrite original value of price. To avoid that, i simply add 1 to be consistent with DB.
                 */
                if (obj.has(columnName)) {
                    columnName += "1";
                }
                obj.put(columnName, columnValue);
            }
            jsonArray.put(obj);
        }
        return jsonArray;
    }

}
