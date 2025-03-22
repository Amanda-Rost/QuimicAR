package com.google.ar.core.examples.java.principal;

import static java.awt.font.TextAttribute.TRACKING;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Anchor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.ar.core.*;
import com.google.ar.core.examples.java.common.helpers.*;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.principal.databinding.ActivityMainBinding;
import com.google.ar.core.examples.java.principal.rendering.AugmentedImageRenderer;
import com.google.ar.core.examples.java.principal.rendering.MongoDBHelper;
import com.google.ar.core.examples.java.principal.rendering.TextRecognitionHelper;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.bson.Document;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.graphics.ImageFormat;

public class QuimicAR extends AppCompatActivity implements GLSurfaceView.Renderer {
  private static final String TAG = "QuimicAR";
  private static final int CAMERA_PERMISSION_CODE = 100;
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
  // Augmented image configuration and rendering.
  // Load a single image (true) or a pre-generated image database (false).
  private final boolean useSingleImage = false;
  // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
  // the
  // database.
  private  TextRecognitionHelper textRecognitionHelper;
  private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();
  private MongoDBHelper mongoDBHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    mongoDBHelper = new MongoDBHelper();
    if (mongoDBHelper.getDatabase() != null) {
      Toast.makeText(this, "Conectado ao MongoDB!", Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(this, "Erro na conexão com o MongoDB!", Toast.LENGTH_LONG).show();
    }

    // Inicializa o TextRecognitionHelper
    textRecognitionHelper = new TextRecognitionHelper(new TextRecognitionHelper.TextRecognitionListener() {
      @Override
      public void onObjectFound(byte[] object3D, byte[] config3D, byte[] texture) {
        // Faça algo com o objeto encontrado (ex: renderize o modelo 3D)
        Log.d(TAG, "Objeto 3D encontrado e pronto para renderizar!");
      }

      @Override
      public void onTextRecognitionFailed(Exception e) {
        Log.e(TAG, "Falha no reconhecimento de texto: " + e.getMessage());
      }
    }, mongoDBHelper);
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

    // Verifica se já tem permissão para usar a câmera

    jsmolWebView = findViewById(R.id.jsmolWebView);
    WebSettings webSettings = jsmolWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setAllowContentAccess(true);
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
    surfaceView.setWillNotDraw(false);

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

  private void iniciarSessaoAR() {
    try {
      if (session == null) {
        session = new Session(this);
        Config config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        session.configure(config);
      }
      shouldConfigureSession = true;
      surfaceView.onResume();
    } catch (UnavailableArcoreNotInstalledException |
             UnavailableApkTooOldException |
             UnavailableSdkTooOldException |
             UnavailableDeviceNotCompatibleException e) {
      Log.e(TAG, "Erro ao iniciar ARCore: " + e.getMessage());
      Toast.makeText(this, "Erro ao iniciar ARCore", Toast.LENGTH_LONG).show();
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
  protected void onResume() {
    super.onResume();

      if (CameraPermissionHelper.hasCameraPermission(this)) {
          if (session == null) {
              Exception exception = null;
              String message = null;
              try {
                  switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                      case INSTALL_REQUESTED:
                          installRequested = true;
                          return;
                      case INSTALLED:
                          break;
                  }

                  session = new Session(/* context = */ this);
              } catch (UnavailableArcoreNotInstalledException
                       | UnavailableUserDeclinedInstallationException e) {
                  message = "Por favor, instale o ARCore";
                  exception = e;
              } catch (UnavailableApkTooOldException e) {
                  message = "Por favor, atualize o ARCore";
                  exception = e;
              } catch (UnavailableSdkTooOldException e) {
                  message = "Por favor, atualize este aplicativo";
                  exception = e;
              } catch (Exception e) {
                  message = "Este dispositivo não suporta o ARCore";
                  exception = e;
              }

              if (message != null) {
                  messageSnackbarHelper.showError(this, message);
                  Log.e(TAG, "Exceção ao criar sessão", exception);
                  return;
              }

              shouldConfigureSession = true;
          }

          if (shouldConfigureSession) {
              configureSession();
              shouldConfigureSession = false;
          }

          try {
              session.resume();
          } catch (CameraNotAvailableException e) {
              messageSnackbarHelper.showError(this, "Câmera indisponível. Tente reiniciar o aplicativo.");
              session = null;
              return;
          }
          surfaceView.onResume();
          displayRotationHelper.onResume();

          fitToScanView.setVisibility(View.VISIBLE);
      } else {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
      }

  }
  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(
                      this, "É necessário permitir o acesso à câmera!", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);
     // augmentedImageRenderer.createOnGlThread(/*context=*/ this);
    } catch (IOException e) {
      Log.e(TAG, "Falha ao ler arquivo", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
public void onDrawFrame(GL10 gl) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (session == null) {
        return;
    }

    displayRotationHelper.updateSessionIfNeeded(session);

    try {
        Frame frame = session.update();
        Camera camera = frame.getCamera();

        // Atualizar a textura da câmera
        session.setCameraTextureName(backgroundRenderer.getTextureId());
        backgroundRenderer.draw(frame);

        // Passar o frame para o reconhecimento de texto
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            Image image = null;
            try {
                // Captura a imagem do frame como Image
                image = frame.acquireCameraImage();
                if (image != null) {
                    Bitmap bitmap = convertYUVToBitmap(image);
                    // Envia o Bitmap para o TextRecognitionHelper
                    textRecognitionHelper.extractTextFromImage(bitmap);
                }
            } catch (NotYetAvailableException e) {
                Log.w(TAG, "Imagem da câmera ainda não disponível: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Falha ao capturar a imagem da câmera: " + e.getMessage());
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    } catch (Throwable t) {
        Log.e(TAG, "Exceção na thread OpenGL", t);
    }
}

private Bitmap convertYUVToBitmap(Image image) {
    // Verifique se o formato da imagem é YUV_420_888
    if (image.getFormat() != ImageFormat.YUV_420_888) {
        throw new IllegalArgumentException("Formato de imagem não suportado: " + image.getFormat());
    }

    // Converta a imagem YUV para um Bitmap RGB
    Image.Plane[] planes = image.getPlanes();
    ByteBuffer yBuffer = planes[0].getBuffer();
    ByteBuffer uBuffer = planes[1].getBuffer();
    ByteBuffer vBuffer = planes[2].getBuffer();

    int ySize = yBuffer.remaining();
    int uSize = uBuffer.remaining();
    int vSize = vBuffer.remaining();

    byte[] nv21 = new byte[ySize + uSize + vSize];

    // U e V são intercambiados
    yBuffer.get(nv21, 0, ySize);
    vBuffer.get(nv21, ySize, vSize);
    uBuffer.get(nv21, ySize + vSize, uSize);

    YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
    byte[] imageBytes = out.toByteArray();
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
}
  private void configureSession() {
    Config config = new Config(session);
    config.setFocusMode(Config.FocusMode.AUTO);
//    if (!setupAugmentedImageDatabase(config)) {
//messageSnackbarHelper.showError(this, "N&atilde;o foi poss&iacute;vel estabelecer o banco de dados");
//    }
    session.configure(config);
  }

  private void drawAugmentedImages(
          Frame frame, float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {
    Collection<AugmentedImage> updatedAugmentedImages =
            frame.getUpdatedTrackables(AugmentedImage.class);

    // Iterate to update augmentedImageMap, remove elements we cannot draw.
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = "Imagem "+ augmentedImage.getIndex()+ " detectada";
          messageSnackbarHelper.showMessage(this, text);
          session.getConfig().setAugmentedImageDatabase(null);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          this.runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      fitToScanView.setVisibility(View.GONE);
                    }
                  });

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            Anchor centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            // augmentedImageMap.put(

            //augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage.getIndex());
          break;

        default:
          break;
      }
    }

    // Draw all images in augmentedImageMap
    for (Pair<AugmentedImage, Anchor> pair : augmentedImageMap.values()) {
      AugmentedImage augmentedImage = pair.first;
      Anchor centerAnchor = augmentedImageMap.get(augmentedImage.getIndex()).second;
      switch (augmentedImage.getTrackingState()) {
        case TRACKING:
          // Carregar o arquivo HTML inicial
//          runOnUiThread(new Runnable() {
//            @Override
//            public void run() { jsmolWebView.loadUrl("file:///android_asset/index.html");}
//          });
        //augmentedImageRenderer.draw(
          //        viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba);
          break;
        default:
          break;
      }
    }
  }

  private boolean setupAugmentedImageDatabase(Config config) {
    AugmentedImageDatabase augmentedImageDatabase;

    // There are two ways to configure an AugmentedImageDatabase:
    // 1. Add Bitmap to DB directly
    // 2. Load a pre-built AugmentedImageDatabase
    // Option 2) has
    // * shorter setup time
    // * doesn't require images to be packaged in apk.
    if (useSingleImage) {
      Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
      if (augmentedImageBitmap == null) {
        return false;
      }

      augmentedImageDatabase = new AugmentedImageDatabase(session);
      augmentedImageDatabase.addImage("image_name", augmentedImageBitmap,0.4f);
      // If the physical size of the image is known, you can instead use:
      //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
      // This will improve the initial detection speed. ARCore will still actively estimate the
      // physical size of the image as it is viewed from multiple viewpoints.
    } else {
      // This is an alternative way to initialize an AugmentedImageDatabase instance    ,
      // load a pre-existing augmented image database.
      try (InputStream is = getAssets().open("app.imgdb")) {
        augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
      } catch (IOException e) {
        Log.e(TAG, "Exceção IO ao carregar o banco de dados.", e);
        return false;
      }
    }

    config.setAugmentedImageDatabase(augmentedImageDatabase);
    return true;
  }

  private Bitmap loadAugmentedImageBitmap() {
    try (InputStream is = getAssets().open("default.jpg")) {
      return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      Log.e(TAG, "Exceção IO ao carregar bitmap da imagem.", e);
    }
    return null;
  }
}