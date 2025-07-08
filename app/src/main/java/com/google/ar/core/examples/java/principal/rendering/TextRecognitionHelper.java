package com.google.ar.core.examples.java.principal.rendering;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.ar.core.examples.java.principal.rendering.validation.OnDatabaseResultListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Classe responsável por realizar o reconhecimento de texto a partir de uma imagem
 * e buscar o objeto 3D correspondente no banco de dados.
 */
public class TextRecognitionHelper {

    private static final String TAG = "TextRecognitionHelper";
    private TextRecognitionListener listener;
    private final MongoDBHelper mongoDBHelper;

    public TextRecognitionHelper(TextRecognitionListener listener, MongoDBHelper dbHelper) {
        this.listener = listener;
        this.mongoDBHelper = dbHelper;
    }

    public void extractTextFromImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener(visionText -> {
                String recognizedText = visionText.getText().trim();
                if (recognizedText.isEmpty()) {
                    Log.e(TAG, "Texto reconhecido está vazio.");
                    listener.onTextRecognitionFailed(new Exception("Texto reconhecido está vazio."));
                    return;
                }
                Log.d(TAG, "Texto reconhecido: " + recognizedText);
    
                // Busca o modelo no banco por formato
                mongoDBHelper.buscarCompostoPorFormato(recognizedText, new OnDatabaseResultListener() {
                    @Override
                    public void onSuccess(String object3DPath, String config3DPath, String texturaPath) {
                        listener.onObjectFound(object3DPath, config3DPath, texturaPath);
                    }
    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Falha ao buscar por formato: " + recognizedText, e);
                        // Se a busca por formato falhar, tenta buscar por nomenclatura
                        mongoDBHelper.buscarCompostoPorNomenclatura(recognizedText, new OnDatabaseResultListener() {
                            @Override
                            public void onSuccess(String object3DPath, String config3DPath, String texturaPath) {
                                listener.onObjectFound(object3DPath, config3DPath, texturaPath);
                            }
    
                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Falha ao buscar por nomenclatura: " + recognizedText, e);
                                listener.onTextRecognitionFailed(new Exception("Falha ao buscar por formato e nomenclatura: " + recognizedText, e));
                            }
                        });
                    }
                });
            })
            .addOnFailureListener(e -> listener.onTextRecognitionFailed(e));
    }

    public interface TextRecognitionListener {
        void onObjectFound(String object3DPath, String config3DPath, String texturePath);
        void onTextRecognitionFailed(Exception e);
    }
}