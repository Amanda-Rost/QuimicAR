package com.google.ar.core.examples.java.principal.rendering;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.bson.Document;

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
                    String recognizedText = visionText.getText();
                    Log.d(TAG, "Texto reconhecido: " + recognizedText);

                    // Busca o modelo no banco
                    mongoDBHelper.buscarCompostoPorNomenclatura(recognizedText, new MongoDBHelper.OnDatabaseResultListener() {
                        @Override
                        public void onSuccess(byte[] object3D, byte[] config3D, byte[] textura) {
                            listener.onObjectFound(object3D, config3D, textura);
                        }

                        @Override
                        public void onError(Exception e) {
                            listener.onTextRecognitionFailed(e);
                        }
                    });
                })
                .addOnFailureListener(e -> listener.onTextRecognitionFailed(e));
    }

    public interface TextRecognitionListener {
        void onObjectFound(byte[] object3D, byte[] config3D, byte[] texture);
        void onTextRecognitionFailed(Exception e);
    }
}
