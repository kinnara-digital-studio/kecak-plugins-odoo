import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.common.rpc.SearchFilter;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooAuthorizationException;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class OdooTest {
    public final static String PROPERTIES_FILE = "test.properties";
    private final String baseUrl;
    private final String database;

    private final String user;

    private final String apiKey;

    private final OdooRpc rpc;

    public OdooTest() {
        final Properties properties = getProperties(PROPERTIES_FILE);

        baseUrl = properties.get("baseUrl").toString();
        database = properties.get("database").toString();
        user = properties.get("user").toString();
        apiKey = properties.get("apiKey").toString();

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

        final Collection<Integer> records = new HashSet<>();
        String model = "product.template";

        SearchFilter[] filters = SearchFilter.single("purchase_ok", 1);
        for (Map<String, Object> record : rpc.searchRead(model, filters, "id", null, null)) {
            System.out.println(record.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining(" | ")));
        }
    }

    @org.junit.Test
    public void testRead() throws OdooCallMethodException {

        String model = "hr.department";
        int recordId = rpc.search(model, SearchFilter.single("id", 1028), null, null, 1)[0];
        final Map<String, Object> record = rpc.read(model, recordId)
                .orElseThrow(() -> new OdooCallMethodException("record not found"));

        record.forEach((s, o) -> System.out.println(s + "->" + o));
    }

    @org.junit.Test
    public void testFieldsGet() throws OdooCallMethodException {
        final Map<String, Map<String, Object>> fields = rpc.fieldsGet("hr.employee");

        assert !fields.isEmpty();

        fields.forEach((k, v) -> {
            System.out.println("[" + k + "]");
            v.forEach((k2, v2) -> System.out.println(k2 + "->" + v2));
        });
    }

    @org.junit.Test
    public void testWrite() throws OdooCallMethodException {
        String model = "fleet.vehicle";
        int recordId = rpc.search(model, null, null, null, 4)[0];

        rpc.write(model, recordId, new HashMap<String, Object>() {{
            put("state_id", "4");
        }});
    }

    @org.junit.Test
    public void testCreate() throws OdooCallMethodException {

        String model = "room.booking";
        int roomId = 2;
        int organizerId = 2;
        final Map<String, Object> record = new HashMap<>() {{
            put("name", "Training Kecak");
            put("room_id", 8);
            put("organizer_id", 2);
            put("start_datetime", "2025-09-12T08:00:00+07:00");
            put("stop_datetime", "2025-09-12T09:00:00+07:00");
        }};

        int recordId = rpc.create(model, record);

        System.out.println(recordId);
    }

    @org.junit.Test
    public void testDelete() throws OdooCallMethodException {

        String model = "product.template";
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

    @Test
    public void test() {
        System.out.println(new BigDecimal("11_095_704_500_000_000"));
    }
}
