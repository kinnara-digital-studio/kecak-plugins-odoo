import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooAuthorizationException;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class Test {
    public final static String PROPERTIES_FILE = "test.properties";
    private final String baseUrl;
    private final String database;

    private final String user;

    private final String apiKey;
    private final String model;

    private final OdooRpc rpc;

    public Test(){
        final Properties properties = getProperties();
        baseUrl = properties.get("baseUrl").toString();
        database = properties.get("database").toString();
        user = properties.get("user").toString();;
        apiKey = properties.get("apiKey").toString();
        model = "res.partner";

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

        final int count = rpc.searchCount(model, null);
        System.out.println(count);

        for (Object partner : rpc.searchRead(model, null, null, 1)) {
            System.out.println(partner.getClass());
            System.out.println(partner);
        }
    }
    @org.junit.Test
    public void testRead() throws OdooCallMethodException {
        int recordId = rpc.search(model, null, null, 1)[0];
        final Map<String, Object> record = rpc.read(model, recordId)
                .orElseThrow(() -> new RuntimeException("record not found"));

        record.forEach((k, v) -> System.out.println(k + "->" + v));
    }

    @org.junit.Test
    public void testFieldsGet () throws OdooCallMethodException {
        final Map<String, Map<String, Object>> fields = rpc.fieldsGet(model);

        assert !fields.isEmpty();

        fields.forEach((k, v) -> {
            System.out.println("====" + k);
            v.forEach((k2, v2) -> System.out.println(k2 + "->" + v2));
        });
    }

    protected Properties getProperties() {
        Properties prop = new Properties();
        try (InputStream inputStream = Test.class.getResourceAsStream(PROPERTIES_FILE)) {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        return prop;
    }
}
