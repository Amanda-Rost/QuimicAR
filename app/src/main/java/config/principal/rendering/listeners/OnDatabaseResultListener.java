package config.principal.rendering.listeners;

public interface OnDatabaseResultListener {
    void onSuccess(String object3DPath, String config3DPath, String texturaPath);
    void onError(Exception e);
}
