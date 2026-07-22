import com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException;
import com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException;
import com.kinnarastudio.odooxmlrpc.model.Field;
import com.kinnarastudio.odooxmlrpc.model.SearchFilter;
import com.kinnarastudio.odooxmlrpc.rpc.OdooRpc;
import model.HrEmployee;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Please copy and rename this file to test.properties
 * Author: @aristosh
 */

public class OdooTest {
    public final static String PROPERTIES_FILE = "test.properties";
    private final String baseUrl;
    private final String database;

    private final String user;

    private final String apiKey;

    private final OdooRpc rpc;

    public OdooTest() throws com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException {
        final Properties properties = getProperties(PROPERTIES_FILE);
        baseUrl = properties.get("baseUrl").toString();
        database = properties.get("database").toString();
        user = properties.get("user").toString();
        apiKey = properties.get("apiKey").toString();

        rpc = new OdooRpc(baseUrl, database, user, apiKey);
    }

    @org.junit.Test
    public void testLogin() throws com.kinnarastudio.odooxmlrpc.exception.OdooAuthorizationException {
        int uid = rpc.login();
        assert uid == 2;
    }

    @org.junit.Test
    public void testSearch() throws OdooCallMethodException {
        String model = "hr.employee";

        // SearchFilter[] filters = SearchFilter.single("movement_id", 9);
        SearchFilter[] filters = new SearchFilter[] { new SearchFilter("active", false)};
        String[] fields = new String[] {"id", "name", "company_id"};
        for (Map<String, Object> record : rpc.searchRead(model, fields, filters, "id", null, 1)) {
            System.out.println(record.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue())
                    .collect(Collectors.joining(" | ")));
        }
    }

    @org.junit.Test
    public void testRead() throws com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException {

        String model = "room.room";
        SearchFilter[] filter = null;
        int recordId = rpc.search(model, filter, null, null, 4)[0];
        final Map<String, Object> record = rpc.read(model, recordId)
                .orElseThrow(() -> new com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException("record not found"));

        record.forEach((s, o) -> System.out.println(s + "->" + o));
    }

    @org.junit.Test
    public void testFieldsGet() throws com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException {
        final Collection<Field> fields = rpc.fieldsGet("hr.employee");

        assert !fields.isEmpty();

        fields.forEach((f) -> {
            System.out.println("[" + f + "]");
            f.getMetadata().forEach((k2, v2) -> System.out.println(k2 + "->" + v2));
        });
    }

    @org.junit.Test
    public void testWrite() throws OdooCallMethodException {
        String model = "item.request";
        int recordId = 429;

        rpc.write(model, recordId, new HashMap<>() {{
            put("api_approver_ids", new Integer[]{313});
        }});
    }

    @org.junit.Test
    public void testCreate() throws com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException {

        String model = "product.template";
        final Map<String, Object> record = new HashMap<>() {
            {
                put("name", "Testing Product Hqhr");
                put("customer_id", 1416);
                put("categ_id", 595);
                put("list_price", 1.0);
                put("size_fw", 80);
                put("size_sw", 0.0);
                put("size_pitch", 190);
                put("spec_length", 1000);
                put("spec_thickness", 35);
                put("material_film_id", 6750);
                put("uom_id", 27);
                put("uom_po_id", 27);
                put("purchase_line_warn", "no-message");
                put("sale_line_warn", "no-message");
                put("tracking", "none");
                put("detailed_type", "product");
            }
        };

        int recordId = rpc.create(model, record);

        System.out.println(recordId);
    }

    @org.junit.Test
    public void testDelete() throws com.kinnarastudio.odooxmlrpc.exception.OdooCallMethodException {

        String model = "stock.movements.lines";

        rpc.unlink(model, 27);
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
        System.out.println(Arrays.stream(new String[0]).anyMatch(String::isEmpty));
    }

    @Test
    public void testBus() throws OdooCallMethodException {
        int messageId = rpc.messagePost("purchase.order", 54, "Sending from kecak [" + new Date() + "]");
    }

    @Test
    public void testSearchReadWithComplexFilters() throws OdooCallMethodException {
        // Here we test hitting the actual Odoo instance to ensure our generated
        // Prefix Array works exactly as Odoo expects.
        String model = "product.template"; // Feel free to change to any existing model in your DB

        // Example: name = 'John' OR name = 'Jane'
//         SearchFilter[] filters = new SearchFilter[] {
//         new SearchFilter("master_category_name", "=", "Finish Good",
//         SearchFilter.OR),
//         new SearchFilter("master_category_name", "=", "Raw Material"),
//
//         };

//         SearchFilter[] filters = new SearchFilter[] {
//             new SearchFilter("master_category_name", "=", "Finish Good", SearchFilter.AND),
//             new SearchFilter("name", "ilike", "Perfume", SearchFilter.AND),
//             new SearchFilter("id", ">", 20)
//         };

//        SearchFilter[] filters = new SearchFilter[] {
//                new SearchFilter("name", "=", "Sprite", SearchFilter.OR),
//                new SearchFilter("name", "=", "Le Minerale", SearchFilter.OR),
//                new SearchFilter("name", "=", "Perfume RED")
//        };

        // SearchFilter[] filters = new SearchFilter[] {
        // new SearchFilter("name", "=", "Sprite", null),
        // new SearchFilter("master_category_name", "=", "Finish Good", null)
        // };

        SearchFilter[] filters = new SearchFilter[]{
                new SearchFilter("master_category_name", SearchFilter.Operator.EQUAL, "Raw Material", SearchFilter.Join.OR),
                new SearchFilter("master_category_name", SearchFilter.Operator.EQUAL, "Finish Good", SearchFilter.Join.AND),
                new SearchFilter("id", SearchFilter.Operator.LESS, 30)
        };

        System.out.println("Executing OdooRpc searchRead...");
        Map<String, Object>[] result = rpc.searchRead(model, filters, "id", null, 100);
        // System.out.println(Arrays.deepToString(result));

        System.out.println("Result Count: " + result.length);
        for (Map<String, Object> r : result) {
            System.out.println("ID: " + r.get("id") + ", Name: " + r.get("name"));
        }
    }

    @Test
    public void testSearchCount() throws OdooCallMethodException {
        String model = "hr.employee";
        SearchFilter[] filters = new SearchFilter[]{
                new SearchFilter("barcode",  SearchFilter.Operator.NOT_EQUAL, ""),
                new SearchFilter("active", new Object[] {false})
        };
        int count = rpc.searchCount(model, filters);
        System.out.println(count);
    }

    @Test
    public void testSearchReadEmployeeSuccess() throws OdooCallMethodException {
        // Simulasi sukses untuk pemanggilan Util.getJobId()
        // Memakai array fields supaya odoo tidak perlu menghitung "newly_hired"
        String username = "061489"; // Data Hardcode yang dicoba USER
        String model = "hr.employee";

        // Kita coba mencari pegawai dengan barcode 061489
        SearchFilter[] filters = new SearchFilter[]{new SearchFilter("barcode", username)};

        // Membatasi request agar HANYA mengambil field "job_id"
        String[] fields = new String[]{"job_id", "barcode", "name"};

        // Memanggil overloaded method searchRead dengan parameter fields terbatas
        Map<String, Object>[] records = rpc.searchRead(model, fields, filters, null, null, 1);

        System.out.println("Cari barcode " + username + " -> Jumlah record yang ditemukan: " + records.length);

        String jobId = Arrays.stream(records)
                .findFirst()
                .map(m -> m.get("job_id"))
                .map(obj -> {
                    if (obj instanceof Object[]) {
                        Object[] arrayObj = (Object[]) obj;
                        return arrayObj.length > 0 ? String.valueOf(arrayObj[0]) : "";
                    }
                    return obj != null ? String.valueOf(obj) : "";
                })
                .orElse("");

        System.out.println("Job ID (Sukses): " + jobId);
    }

    @Test
    public void testSearchEmployeeByJobId() throws OdooCallMethodException {
        String model = "hr.employee";
        Integer targetJobId = 588;

        SearchFilter[] filters = new SearchFilter[]{new SearchFilter("job_id", targetJobId)};

        String[] fields = new String[]{"job_id", "barcode", "name"};

        Map<String, Object>[] records = rpc.searchRead(model, fields, filters, null, null, null);

        System.out.println("Cari job_id " + targetJobId + " -> Jumlah employee ditemukan: " + records.length);

        for (Map<String, Object> emp : records) {
            Object jobIdRaw = emp.get("job_id");
            String jobIdStr = "";
            if (jobIdRaw instanceof Object[]) {
                Object[] arr = (Object[]) jobIdRaw;
                jobIdStr = arr.length > 0 ? String.valueOf(arr[0]) : "kosong";
            }

            System.out.println("  name   : " + emp.get("name"));
            System.out.println("  barcode: " + emp.get("barcode")); // null = tidak ada barcode di Odoo
            System.out.println("  job_id : " + jobIdStr);
            System.out.println("  ---");
        }
    }

    @Test
    public void testCostCenter() throws OdooCallMethodException {
        String model = "account.analytic.account";
        SearchFilter[] filters = new SearchFilter[]{
                new SearchFilter("company_id", SearchFilter.Operator.IN, 2),
        };
        Map<String, Object>[] records = rpc.searchRead(model, filters, null, null, null);
        for (Map<String, Object> record : records) {
            System.out.println("-----------------------["+record.get("id")+"]---------------------");
            record.forEach((key, value) -> {
                System.out.println("[" + key + "] -> [" + value + "]");
                if(value instanceof Object[]) {
                    Object[] values = (Object[]) value;
                    System.out.println("[" + key + "] -> [" + Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(";")) + "]");
                }
            });

            System.out.println("===== ");
        }
    }


    @Test
    public void testSearchRead() throws OdooCallMethodException {
        SearchFilter[] filter = new SearchFilter[]{
                new SearchFilter(SearchFilter.Join.AND, "user_id", SearchFilter.Operator.NOT_EQUAL, null),
                new SearchFilter(SearchFilter.Join.OR, "department_id.name", SearchFilter.Operator.ILIKE, "%Marketing%"),
        };

        HrEmployee[] records = rpc.searchRead(HrEmployee.class, filter, null, null, null);
        System.out.println(records.length);
        Arrays.stream(records)
                .map(m -> {
                    String id = String.valueOf(m.getId());
                    String name = String.valueOf(m.getName());
                    String barcode = String.valueOf(m.getBarcode());
//                    Object[] job_id = (Object[]) m.get("job_id");
//                    String jobId = Arrays.stream(job_id).map(String::valueOf).collect(Collectors.joining(";"));
                    return String.join(" | ", id, name, barcode);
                })
                .map(String::valueOf)
                .forEach(System.out::println);
//                .forEach(System.out::println);

//        Arrays.stream(records).forEach(System.out::println);
    }
}
