package MobileStockServiceApi;

import java.io.File;
import java.sql.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Read-only diagnostics; credentials are read from the local provider XML, never printed. */
public class ProductInfoReportLiveTest {
    static String value(Element node, String tag) {
        return node.getElementsByTagName(tag).item(0).getTextContent();
    }

    static Connection connect(String file, String database) throws Exception {
        Element node = (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(file)).getElementsByTagName("node").item(0);
        String port = value(node, "port");
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection("jdbc:postgresql://" + value(node, "server")
                + ":" + (port.isEmpty() ? "5432" : port) + "/" + database
                + "?connectTimeout=10&socketTimeout=90", value(node, "user"), value(node, "password"));
        conn.setReadOnly(true);
        return conn;
    }

    static void query(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    if (i > 1) line.append(" | ");
                    line.append(meta.getColumnLabel(i)).append('=').append(rs.getString(i));
                }
                System.out.println(line);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = connect(args[0], args[1])) {
            for (int i = 2; i < args.length; i++) query(conn, args[i]);
        }
    }
}
