package com.kinnarastudio.kecakplugins.odoo.hashVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.commons.util.LogUtil;

import com.kinnarastudio.kecakplugins.odoo.common.rpc.OdooRpc;
import com.kinnarastudio.kecakplugins.odoo.exception.OdooCallMethodException;

public class OdooReadHashVariable extends DefaultHashVariablePlugin{

    @Override
    public String getPrefix() {
        return "odooRead";
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> list = new ArrayList();
        list.add(this.getPrefix() + ".baseUrl=BASEURL&user=USER&apiKey=APIKEY&database=DATABASE&model=MODEL&id=ID&column=COLUMN");
        return list;
    }

    @Override
    public String processHashVariable(String params) {
        String[] parts = params.split("&");
        String[][] keyValue = new String[parts.length][2];
        for (int i = 0; i < parts.length; i++)
        {
            keyValue [i]= parts[i].split("=");
        }
        
        // LogUtil.info(getClassName(), "Parts Length: " + parts.length);
        // LogUtil.info(getClassName(), "Parts: " + Arrays.toString(parts));

        // for (int i = 0; i < parts.length; i++)
        // {
        //     LogUtil.info(getClassName(), "Value "+ i +": " + Arrays.toString(keyValue[i]));
        // }

        String baseUrl = "";
        String database = "";
        String user = "";
        String apiKey = "";
        String model = "";
        int id = 0;
        String column = "";

        for (int i = 0; i < parts.length; i++)
        {
            if (keyValue[i][0].equals("baseUrl"))
            {
                baseUrl = keyValue[i][1];
            }
            else if(keyValue[i][0].equals("user"))
            {
                user = keyValue[i][1];
            }
            else if(keyValue[i][0].equals("apiKey"))
            {
                apiKey = keyValue[i][1];
            }
            else if(keyValue[i][0].equals("database"))
            {
                database = keyValue[i][1];
            }
            else if(keyValue[i][0].equals("model"))
            {
                model = keyValue[i][1];
            }
            else if(keyValue[i][0].equals("id"))
            {
                id = Integer.parseInt(keyValue[i][1]);
            }
            else if(keyValue[i][0].equals("column"))
            {
                column = keyValue[i][1];
            }
        }

        // LogUtil.info(getClassName(), "baseUrl: " + baseUrl);
        // LogUtil.info(getClassName(), "user: " + user);
        // LogUtil.info(getClassName(), "apiKey: " + apiKey);

        String value = "";

        OdooRpc odooRpc = new OdooRpc(baseUrl, database, user, apiKey);
        try {
            Optional<Map<String, Object>> readRpc = odooRpc.read(model, id);
            if(readRpc.isPresent()) 
            {
                // LogUtil.info(getClassName(), "Read RPC" + readRpc.get().get(column));
                value = (String) readRpc.get().get(column);
            }
        } catch (OdooCallMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public String getClassName() {
        return OdooReadHashVariable.class.getName();
    }

    @Override
    public String getLabel() {
        return "Odoo Hash Variable";
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getName() {
        return "Odoo Read";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
    
}
