package com.google.ar.core.examples.java.principal.rendering;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import org.bson.Document;

public class TextRecognitionHelper {

    private TextRecognitionListener listener;
    private final TextRecognizer recognizer;
    private MongoDBHelper mongoDBHelper;

    public TextRecognitionHelper(TextRecognitionListener listener, MongoDBHelper dbHelper) {
        this.listener = listener;
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.mongoDBHelper = dbHelper;
    }

    public void extractTextFromImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        String recognizedText = visionText.getText();
                        if (!recognizedText.isEmpty()) {
                            Log.d("TextRecognition", "Texto reconhecido: " + recognizedText);

                            // Buscar o objeto 3D no banco de dados
                            Document result = mongoDBHelper.findObject3D(recognizedText);
                            if (result != null) {
                                String object3D = result.getString("objeto3D");
                                listener.onObjectFound(object3D);
                            } else {
                                Log.e("TextRecognition", "Nenhum modelo 3D correspondente encontrado.");
                            }
                        } else {
                            Log.e("TextRecognition", "Nenhum texto reconhecido.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onTextRecognitionFailed(e);
                    }
                });
    }

    public interface TextRecognitionListener {
        void onObjectFound(String object3D);
        void onTextRecognitionFailed(Exception e);
    }
}
