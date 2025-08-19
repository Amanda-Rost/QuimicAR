package config.principal.rendering;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import config.common.rendering.ObjectRenderer;
import java.io.IOException;

public class AugmentedImageRenderer {

  private final ObjectRenderer molecule = new ObjectRenderer();

  // Constantes para configuração
  private static final float SCALE_FACTOR = 0.25f; // Fator de escala para o modelo 3D
  private static final float OFFSET_Y = -0.4f;     // Deslocamento no eixo Y para ajustar posição

  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {
    try {
      molecule.createOnGlThread(context, "models/metano.obj", "models/pretobranco.png");
      molecule.setMaterialProperties(0.0f, 1.0f, 1.0f, 6.0f);
      molecule.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
    } catch (IOException e) {
      Log.e("AugmentedImageRenderer", "Erro ao carregar modelo 3D.", e);
      throw e;
    }
  }

  /**
   * Desenha o modelo 3D usando apenas o Anchor para posicionamento.
   */
  public void draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          Anchor centerAnchor,
          float[] colorCorrectionRgba) {

    if (centerAnchor == null) return;

    // Matriz de transformação
    float[] modelMatrix = new float[16];
    float[] translationMatrix = new float[16];
    float[] scaleMatrix = new float[16];

    // Pose do Anchor
    Pose anchorPose = centerAnchor.getPose();
    anchorPose.toMatrix(modelMatrix, 0);

    // Aplicar deslocamento vertical
    Matrix.setIdentityM(translationMatrix, 0);
    Matrix.translateM(translationMatrix, 0, 0.0f, OFFSET_Y, 0.0f);
    Matrix.multiplyMM(modelMatrix, 0, translationMatrix, 0, modelMatrix, 0);

    // Aplicar escala
    Matrix.setIdentityM(scaleMatrix, 0);
    Matrix.scaleM(scaleMatrix, 0, SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);
    Matrix.multiplyMM(modelMatrix, 0, scaleMatrix, 0, modelMatrix, 0);

    // Atualizar e desenhar o modelo
    molecule.updateModelMatrix(modelMatrix, SCALE_FACTOR);
    molecule.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, new float[]{1f, 1f, 1f, 1f});
  }

  public void loadModelOnGlThread(Context context, String objectPath, String texturePath) throws IOException {
    molecule.createOnGlThread(context, objectPath, texturePath);
    molecule.setMaterialProperties(0.0f, 1.0f, 1.0f, 6.0f);
    molecule.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
  }
}
