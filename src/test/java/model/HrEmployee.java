package model;

import com.kinnarastudio.odooxmlrpc.annotation.OdooField;
import com.kinnarastudio.odooxmlrpc.annotation.OdooModel;

@OdooModel("hr.employee")
public class HrEmployee {
    @OdooField("id")
    private int id;
    @OdooField("name")
    private String name;
    @OdooField("barcode")
    private String barcode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
