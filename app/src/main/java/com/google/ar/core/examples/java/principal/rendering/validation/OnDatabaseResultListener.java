package com.google.ar.core.examples.java.principal.rendering.validation;

public interface OnDatabaseResultListener {
    void onSuccess(String object3DPath, String config3DPath, String texturaPath);
    void onError(Exception e);
}
