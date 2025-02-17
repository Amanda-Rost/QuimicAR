package com.google.ar.core.examples.java.principal.rendering;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import java.io.IOException;
import java.util.Map;

public class AugmentedImageRenderer {

  private final ObjectRenderer molecule = new ObjectRenderer();
  private String modelFile = "models/default_model.obj"; // Modelo padrão

  private static final float SCALE_FACTOR = 0.25f;
  private static final float OFFSET_Y = -0.4f;

  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {
    try {
      molecule.createOnGlThread(context, modelFile, "models/pretobranco.png");
      molecule.setMaterialProperties(0.0f, 1.0f, 1.0f, 6.0f);
      molecule.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
    } catch (IOException e) {
      Log.e("AugmentedImageRenderer", "Erro ao carregar modelo 3D.", e);
      throw e;
    }
  }

  public void setModelFile(String object3D) {
    if (object3D != null && !object3D.isEmpty()) {
      modelFile = "models/" + object3D;
    } else {
      modelFile = "models/default_model.obj";
    }
  }

  public void drawAugmentedImages(Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap,
                                  float[] viewmtx, float[] projmtx, float[] colorCorrectionRgba) {
    for (Map.Entry<Integer, Pair<AugmentedImage, Anchor>> entry : augmentedImageMap.entrySet()) {
      AugmentedImage augmentedImage = entry.getValue().first;
      Anchor anchor = entry.getValue().second;

      if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
        draw(viewmtx, projmtx, augmentedImage, anchor, colorCorrectionRgba);
      }
    }
  }

  public void draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          AugmentedImage augmentedImage,
          Anchor centerAnchor,
          float[] colorCorrectionRgba) {

    float[] modelMatrix = new float[16];
    float[] translationMatrix = new float[16];
    float[] scaleMatrix = new float[16];

    Pose anchorPose = centerAnchor.getPose();
    anchorPose.toMatrix(modelMatrix, 0);

    android.opengl.Matrix.setIdentityM(translationMatrix, 0);
    android.opengl.Matrix.translateM(translationMatrix, 0, 0.0f, OFFSET_Y, 0.0f);
    android.opengl.Matrix.multiplyMM(modelMatrix, 0, translationMatrix, 0, modelMatrix, 0);

    android.opengl.Matrix.setIdentityM(scaleMatrix, 0);
    android.opengl.Matrix.scaleM(scaleMatrix, 0, SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);
    android.opengl.Matrix.multiplyMM(modelMatrix, 0, scaleMatrix, 0, modelMatrix, 0);

    molecule.updateModelMatrix(modelMatrix, SCALE_FACTOR);
    molecule.draw(viewMatrix, projectionMatrix, colorCorrectionRgba);
  }
}
