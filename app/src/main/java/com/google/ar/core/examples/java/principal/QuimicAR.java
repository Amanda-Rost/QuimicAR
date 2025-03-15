package com.google.ar.core.examples.java.principal;

import com.google.ar.core.Anchor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.ar.core.*;
import com.google.ar.core.examples.java.common.helpers.*;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.principal.databinding.ActivityMainBinding;
import com.google.ar.core.examples.java.principal.rendering.AugmentedImageRenderer;
import com.google.ar.core.examples.java.principal.rendering.MongoDBHelper;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.bson.Document;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class QuimicAR extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = "QuimicAR";
  private WebView jsmolWebView;
  private GLSurfaceView surfaceView;
  private ImageView fitToScanView;
  private RequestManager glideRequestManager;
  private ActivityMainBinding binding;
  private boolean installRequested;
  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final AugmentedImageRenderer augmentedImageRenderer = new AugmentedImageRenderer();
  private boolean shouldConfigureSession = false;
  private final Map<Integer, Pair<AugmentedImageRenderer, Anchor>> augmentedImageMap = new HashMap<>();
  private MongoDBHelper mongoDBHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    mongoDBHelper = new MongoDBHelper();
    if (mongoDBHelper.getDatabase() !=null) {
      Toast.makeText(this, "Conectado ao MongoDB!", Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(this, "Erro na conexão com o MongoDB!", Toast.LENGTH_LONG).show();
    }
//Se precisar inserir, é só tirar o comentario do trecho abaixo,
//Se não funcionar pode ser que sua internet esteja bloquando,
// então use o comando base64 -w 0 arquivo.obj > arquivo.obj.base64
// para converter o tipo de arquivo e insera manualmente no banco

//    try {
//      byte[] objeto3D = loadAssetFile("models/metano.obj");
//      byte[] config3D = loadAssetFile("models/metano.mtl");
//      byte[] textura = loadAssetFile("models/pretobranco.png");
//
//      if (objeto3D == null || config3D == null || textura == null) {
//        System.err.println("Erro ao carregar arquivos dos assets.");
//        return;
//      }
//
//      String molec = "    H \n" +
//              "    | \n" +
//              "H - C - H \n" +
//              "    | \n" +
//              "    H";
//
//      mongoDBHelper.inserirComposto(molec, objeto3D, config3D, textura);
//      System.out.println("Dados inseridos com sucesso no MongoDB!");
//    } catch (Exception e) {
//      System.err.println("Erro ao carregar arquivos: " + e.getMessage());
//      e.printStackTrace();
//    }

    jsmolWebView = findViewById(R.id.jsmolWebView);
    WebSettings webSettings = jsmolWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    jsmolWebView.setWebViewClient(new WebViewClient());
    jsmolWebView.setBackgroundColor(Color.TRANSPARENT);

    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(this);
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    glideRequestManager = Glide.with(this);
    glideRequestManager.load(Uri.parse("file:///android_asset/fit_to_scan.png")).into(fitToScanView);

    installRequested = false;
    binding.Boton.setOnClickListener(view -> {
      Log.d(TAG, "Botão clicado");
      Intent intent = new Intent(QuimicAR.this, Regras.class);
      intent.putExtra("FILENAME", "Regras.txt");
      startActivity(intent);
    });
  }

//Usado para inserir no MongoDB
  private byte[] loadAssetFile(String fileName) {
    try {
      InputStream inputStream = getAssets().open(fileName);
      ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
      int bufferSize = 1024;
      byte[] buffer = new byte[bufferSize];
      int len;
      while ((len = inputStream.read(buffer)) != -1) {
        byteBuffer.write(buffer, 0, len);
      }
      inputStream.close();
      return byteBuffer.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
    session.close();
      session = null;
    }
//    mongoDBHelper.close();
    super.onDestroy();
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
    try {
      backgroundRenderer.createOnGlThread(this);
    } catch (Exception e) {
      Log.e(TAG, "Erro ao inicializar o renderizador de fundo", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    if (session == null) return;

    displayRotationHelper.updateSessionIfNeeded(session);
    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());
      Frame frame = session.update();
      Camera camera = frame.getCamera();
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());
      backgroundRenderer.draw(frame);

      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

      float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);
      //augmentedImageRenderer.drawAugmentedImages(augmentedImageMap, viewmtx, projmtx, colorCorrectionRgba);
    } catch (Throwable t) {
      Log.e(TAG, "Exceção na thread OpenGL", t);
    }
  }
}
