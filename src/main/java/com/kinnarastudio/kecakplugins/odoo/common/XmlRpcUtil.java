package com.kinnarastudio.kecakplugins.odoo.common;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

public class XmlRpcUtil {
    @Nullable
    public static Object execute(String url, String method, Object[] params) throws MalformedURLException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        Object ret = client.execute(method, params);
        return ret;
    }
}
