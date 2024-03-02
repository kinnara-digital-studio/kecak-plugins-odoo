package com.kinnarastudio.kecakplugins.odoo;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListBinder;
import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListFilter;
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
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}