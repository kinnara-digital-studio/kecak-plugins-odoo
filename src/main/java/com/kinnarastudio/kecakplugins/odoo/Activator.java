package com.kinnarastudio.kecakplugins.odoo;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.odoo.app.OdooSearchReadHashVariable;
import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListBinder;
import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListFilter;
import com.kinnarastudio.kecakplugins.odoo.datalist.OdooObjectDataListFormatter;
import com.kinnarastudio.kecakplugins.odoo.form.*;
import com.kinnarastudio.kecakplugins.odoo.process.OdooRpcTool;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(OdooDataListBinder.class.getName(), new OdooDataListBinder(), null));
        registrationList.add(context.registerService(OdooDataListFilter.class.getName(), new OdooDataListFilter(), null));
        registrationList.add(context.registerService(OdooFormBinder.class.getName(), new OdooFormBinder(), null));
        registrationList.add(context.registerService(OdooWorkflowFormBinder.class.getName(), new OdooWorkflowFormBinder(), null));
        registrationList.add(context.registerService(OdooOptionsBinder.class.getName(), new OdooOptionsBinder(), null));
        registrationList.add(context.registerService(OdooFormMultirowBinder.class.getName(), new OdooFormMultirowBinder(), null));
        registrationList.add(context.registerService(OdooRpcTool.class.getName(), new OdooRpcTool(), null));
        registrationList.add(context.registerService(OdooObjectDataListFormatter.class.getName(), new OdooObjectDataListFormatter(), null));
        registrationList.add(context.registerService(OdooSearchReadHashVariable.class.getName(), new OdooSearchReadHashVariable(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}