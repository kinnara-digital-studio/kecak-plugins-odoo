import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooAuthorizationException;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.jsoup.select.Evaluator;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class OdooTest {
    public final static String PROPERTIES_FILE = "test.properties";
    private final String baseUrl;
    private final String database;

    private final String user;

    private final String apiKey;
    private final String model;

    private final OdooRpc rpc;

    public OdooTest() {
        final Properties properties = getProperties(PROPERTIES_FILE);

        baseUrl = properties.get("baseUrl").toString();
        database = properties.get("database").toString();
        user = properties.get("user").toString();
        apiKey = properties.get("apiKey").toString();
        model = "product.template";

        rpc = new OdooRpc(baseUrl, database, user, apiKey);
    }

    @org.junit.Test
    public void testLogin() throws OdooAuthorizationException {
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);
        int uid = rpc.login();
        assert uid == 2;
    }

    @org.junit.Test
    public void testSearch() throws OdooCallMethodException {
        final OdooRpc rpc = new OdooRpc(baseUrl, database, user, apiKey);

        for (Map<String, Object> record : rpc.searchRead(model, new SearchFilter[]{ new SearchFilter("purchase_ok", "=", "null")}, "id", null, null)) {
            System.out.println(record.get("id") + "|" + record.get("name") + "|" + Arrays.stream((Object[])record.get("categ_id")).map(String::valueOf).collect(Collectors.joining(";")) + "|" + Optional.ofNullable(record.get("categ_id")).map(Object::getClass).map(Class::getName).orElse(Integer.class.getName()) +"|"+Object.class.getName());
        }
    }

    @org.junit.Test
    public void testRead() throws OdooCallMethodException {
        int recordId = rpc.search(model, null, null, null, 1)[0];
        final Map<String, Object> record = rpc.read(model, recordId)
                .orElseThrow(() -> new OdooCallMethodException("record not found"));

        record.forEach((k, v) -> System.out.println(k + "->" + v));
    }

    @org.junit.Test
    public void testFieldsGet() throws OdooCallMethodException {
        final Map<String, Map<String, Object>> fields = rpc.fieldsGet("product.category");

        assert !fields.isEmpty();

        fields.forEach((k, v) -> {
            System.out.println("[" + k + "]");
            v.forEach((k2, v2) -> System.out.println(k2 + "->" + v2));
        });
    }

    @org.junit.Test
    public void testWrite() throws OdooCallMethodException {
        final Map<String, Object> record = new HashMap<String, Object>() {{
            put("city", "Bandung");
        }};

        rpc.write(model, 47, record);
    }

    @org.junit.Test
    public void testCreate() throws OdooCallMethodException {
        final Map<String, Object> record = new HashMap<>() {{
            put("name", "Anita Peterson");
        }};

        int recordId = rpc.create(model, record);

        System.out.println(recordId);
    }

    @org.junit.Test
    public void testDelete() throws OdooCallMethodException {
        rpc.unlink(model, 62);
    }

    protected Properties getProperties(String file) {
        Properties prop = new Properties();
        try (InputStream inputStream = OdooTest.class.getResourceAsStream(file)) {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return prop;
    }
}
