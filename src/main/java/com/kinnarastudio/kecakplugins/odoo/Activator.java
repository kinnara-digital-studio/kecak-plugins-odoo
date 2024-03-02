package com.kinnarastudio.kecakplugins.odoo;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListBinder;
import com.kinnarastudio.kecakplugins.odoo.datalist.OdooDataListFilter;
import com.kinnarastudio.kecakplugins.odoo.form.OdooFormLoadBinder;
import com.kinnarastudio.kecakplugins.odoo.form.OdooFormStoreBinder;
import com.kinnarastudio.kecakplugins.odoo.form.OdooOptionsBinder;
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
        registrationList.add(context.registerService(OdooFormLoadBinder.class.getName(), new OdooFormLoadBinder(), null));
        registrationList.add(context.registerService(OdooFormStoreBinder.class.getName(), new OdooFormStoreBinder(), null));
        registrationList.add(context.registerService(OdooOptionsBinder.class.getName(), new OdooOptionsBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}