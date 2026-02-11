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
            String __strQUERY1 = "SELECT code as user_code, name_1 as user_name FROM erp_user WHERE code=upper('" + strUserCode + "') AND password='" + strPassword + "' ORDER BY code";

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

            String __strQUERY1 = "select ic_code,unit_code,price_0,price_1,price_2,price_3,price_4,price_5,price_6,price_7,price_8,price_9,sale_type from ic_inventory_price_formula where ic_code = '" + strItemCode + "' ";
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

            String __strQUERY1 = "select balance_qty,ic_unit_code,\n"
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

            String __strQUERY1 = "SELECT code,name_1 FROM ic_warehouse  where branch_code = '" + strBranchcode.toUpperCase() + "' ORDER BY code";
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

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) like '%" + strSearch.toUpperCase() + "%' or upper(name_1) like '%" + strSearch.toUpperCase() + "%' ";
            }

            String __strQUERY1 = "select code,name_1,coalesce((select doc_list from stc_permission where user_code = code),0) as doc_list, "
                    + "coalesce((select doc_approve_list from stc_permission where user_code = code),0) as doc_approve_list, "
                    + "coalesce((select doc_history_list from stc_permission where user_code = code),0) as doc_history_list, "
                    + "coalesce((select cart_list from stc_permission where user_code = code),0) as cart_list, "
                    + "coalesce((select cart_recount_list from stc_permission where user_code = code),0) as cart_recount_list, "
                    + "coalesce((select report_stock_zero from stc_permission where user_code = code),0) as report_stock_zero, "
                    + "coalesce((select report_stock_not_check from stc_permission where user_code = code),0) as report_stock_not_check, "
                    + "coalesce((select report_stock_diff from stc_permission where user_code = code),0) as report_stock_diff, "
                    + "coalesce((select report_stock_adjust from stc_permission where user_code = code),0) as report_stock_adjust, "
                    + "coalesce((select tagcard_list from stc_permission where user_code = code),0) as tagcard_list, "
                    + "coalesce((select tagcard_count from stc_permission where user_code = code),0) as tagcard_count "
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
                obj.put("doc_list", __rsHead.getString("doc_list"));
                obj.put("doc_approve_list", __rsHead.getString("doc_approve_list"));
                obj.put("doc_history_list", __rsHead.getString("doc_history_list"));
                obj.put("cart_list", __rsHead.getString("cart_list"));
                obj.put("cart_recount_list", __rsHead.getString("cart_recount_list"));
                obj.put("report_stock_zero", __rsHead.getString("report_stock_zero"));
                obj.put("report_stock_not_check", __rsHead.getString("report_stock_not_check"));
                obj.put("report_stock_diff", __rsHead.getString("report_stock_diff"));
                obj.put("report_stock_adjust", __rsHead.getString("report_stock_adjust"));
                obj.put("tagcard_list", __rsHead.getString("tagcard_list"));
                obj.put("tagcard_count", __rsHead.getString("tagcard_count"));
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

    @GET
    @Path("/getErpUserPermissionLogin")
    public Response getErpUserPermissionLogin(
            @QueryParam("provider") String strProvider,
            @QueryParam("dbname") String strDatabaseName,
            @QueryParam("search") String strSearch) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String _where = "";
            if (!strSearch.trim().equals("")) {
                _where = " where upper(code) = '" + strSearch.toUpperCase() + "' ";
            }

            String __strQUERY1 = "select code,name_1,coalesce((select doc_list from stc_permission where user_code = code),0) as doc_list, "
                    + "coalesce((select doc_approve_list from stc_permission where user_code = code),0) as doc_approve_list, "
                    + "coalesce((select doc_history_list from stc_permission where user_code = code),0) as doc_history_list, "
                    + "coalesce((select cart_list from stc_permission where user_code = code),0) as cart_list, "
                    + "coalesce((select cart_recount_list from stc_permission where user_code = code),0) as cart_recount_list, "
                    + "coalesce((select report_stock_zero from stc_permission where user_code = code),0) as report_stock_zero, "
                    + "coalesce((select report_stock_not_check from stc_permission where user_code = code),0) as report_stock_not_check, "
                    + "coalesce((select report_stock_diff from stc_permission where user_code = code),0) as report_stock_diff, "
                    + "coalesce((select report_stock_adjust from stc_permission where user_code = code),0) as report_stock_adjust, "
                    + "coalesce((select tagcard_list from stc_permission where user_code = code),0) as tagcard_list, "
                    + "coalesce((select tagcard_count from stc_permission where user_code = code),0) as tagcard_count "
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
                obj.put("doc_list", __rsHead.getString("doc_list"));
                obj.put("doc_approve_list", __rsHead.getString("doc_approve_list"));
                obj.put("doc_history_list", __rsHead.getString("doc_history_list"));
                obj.put("cart_list", __rsHead.getString("cart_list"));
                obj.put("cart_recount_list", __rsHead.getString("cart_recount_list"));

                obj.put("report_stock_zero", __rsHead.getString("report_stock_zero"));
                obj.put("report_stock_not_check", __rsHead.getString("report_stock_not_check"));
                obj.put("report_stock_diff", __rsHead.getString("report_stock_diff"));
                obj.put("report_stock_adjust", __rsHead.getString("report_stock_adjust"));
                obj.put("tagcard_list", __rsHead.getString("tagcard_list"));
                obj.put("tagcard_count", __rsHead.getString("tagcard_count"));
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
            @QueryParam("doc_list") String doc_list,
            @QueryParam("doc_approve_list") String doc_approve_list,
            @QueryParam("doc_history_list") String doc_history_list,
            @QueryParam("cart_list") String cart_list,
            @QueryParam("cart_recount_list") String cart_recount_list,
            @QueryParam("report_stock_zero") String report_stock_zero,
            @QueryParam("report_stock_not_check") String report_stock_not_check,
            @QueryParam("report_stock_diff") String report_stock_diff,
            @QueryParam("report_stock_adjust") String report_stock_adjust,
            @QueryParam("tagcard_list") String tagcard_list,
            @QueryParam("tagcard_count") String tagcard_count
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

            Statement __stmt1;

            __stmt1 = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet __rsHead = __stmt1.executeQuery("select user_code from stc_permission where user_code = '" + user_code + "'");

            __rsHead.next();

            int row = __rsHead.getRow();

            if (row > 0) {
                Statement __stmtdelete = __conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                __stmtdelete.executeUpdate("delete from stc_permission where user_code = '" + user_code + "';");
            }

            __query_builder.append("INSERT INTO stc_permission (user_code,doc_list,doc_approve_list,doc_history_list,cart_list,cart_recount_list,report_stock_zero,report_stock_not_check,report_stock_diff,report_stock_adjust,tagcard_list,tagcard_count) values ('" + user_code + "','" + doc_list + "','" + doc_approve_list + "','" + doc_history_list + "','" + cart_list + "','" + cart_recount_list + "','" + report_stock_zero + "','" + report_stock_not_check + "','" + report_stock_diff + "','" + report_stock_adjust + "','" + tagcard_list + "','" + tagcard_count + "');");

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
                    + "VALUES (76, 3, ?, ?, ?, ?, ?, 0, '', ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlTrans)) {
                ps.setString(1, docref);
                ps.setDate(2, java.sql.Date.valueOf(docdate));
                ps.setString(3, doctime);
                ps.setString(4, docref);
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
                    + "wh_code, shelf_code, price, sum_amount, branch_code"
                    + ") VALUES ("
                    + "76, 3, 1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?"
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
                    + "wh_code, shelf_code, price, sum_amount, branch_code,wh_code_2, shelf_code_2"
                    + ") VALUES ("
                    + "124, 3, 0, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?"
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
                    + "wh_code, shelf_code, price, sum_amount, branch_code , wh_code_2, shelf_code_2"
                    + ") VALUES ("
                    + "72, 3, -1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?,?,?"
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
                    + "wh_code, shelf_code, price, sum_amount, branch_code"
                    + ") VALUES ("
                    + "310, 1, 1, ?, ?, ?, ?, ?, 0, ?, "
                    + "(SELECT ratio FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT stand_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "(SELECT divide_value FROM ic_unit_use WHERE code=? AND ic_code=? LIMIT 1), "
                    + "?, ?, ?, ?, ?, ?, 0, 0, ?"
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
            @QueryParam("transflag") String strTransFlag) {
        JSONObject __objResponse = new JSONObject();
        __objResponse.put("success", false);
        try {

            _routine __routine = new _routine();
            Connection __conn = __routine._connect(strDatabaseName.toLowerCase(), _global.FILE_CONFIG(strProvider));

            String __strQUERY1 = "SELECT *,(select name_1 from ap_supplier where code=cust_code limit 1 ) as cust_name,(select name_1 from erp_user where code=creator_code limit 1 ) as creator_name,(select name_1 from ic_warehouse where code=wh_to limit 1 ) as to_wh_name,(select name_1 from ic_shelf where code=location_to and whcode=wh_to limit 1 ) as to_location_name,(select name_1 from ic_warehouse where code=wh_code limit 1 ) as wh_name,(select name_1 from ic_shelf where code=location_code and whcode=wh_code limit 1 ) as location_name,(select count(doc_no) from msc_cart_detail where msc_cart_detail.doc_no=msc_cart.doc_no) as item_count,carts FROM msc_cart where  is_merge = 0 and branch_code='" + strBranchcode + "' and trans_flag = '" + strTransFlag + "' ORDER BY create_datetime desc limit 200";

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
                    + "select '" + docno + "',barcode,item_code,unit_code,'" + whcode + "','" + locationcode + "',sum(qty) as qty,MAX(balance_qty) AS balance_qty,MAX(balance_qty) - SUM(qty) AS diff_qty,0,STRING_AGG(COALESCE(NULLIF(location, ''), NULL), ', ') AS location,min(is_no_stock),MAX(line_number) from msc_cart_detail where doc_no in (" + docNoIn + ") group by item_code,unit_code ";
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
