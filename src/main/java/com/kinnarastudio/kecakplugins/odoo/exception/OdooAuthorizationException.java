package com.kinnarastudio.kecakplugins.odoo.exception;

public class OdooAuthorizationException extends Exception {
    public OdooAuthorizationException(String message) {
        super(message);
    }

    public OdooAuthorizationException(Throwable cause) {
        super(cause);
    }
}
