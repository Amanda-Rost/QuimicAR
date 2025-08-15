package config.principal.rendering.listeners;

public interface TextRecognitionListener {
    void onObjectFound(String object3DPath, String config3DPath, String texturePath);
    void onTextRecognitionFailed(Exception e);

}
