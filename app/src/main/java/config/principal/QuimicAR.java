package config.principal;

import com.google.ar.core.AugmentedImage;
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
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
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
import config.common.helpers.*;
import config.common.rendering.BackgroundRenderer;
import config.principal.databinding.ActivityMainBinding;
import config.principal.rendering.AugmentedImageRenderer;
import config.principal.rendering.MongoDBHelper;
import config.principal.rendering.TextRecognitionHelper;
import config.principal.rendering.listeners.TextRecognitionListener;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
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
    private TextRecognitionHelper textRecognitionHelper;
    private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();
    private MongoDBHelper mongoDBHelper;
    private Frame lastFrame;
    private Anchor currentAnchor;
    private boolean isObjectFound = false; // true quando achou objeto e pausa extração
    private int touchCount = 0;            // conta os toques na tela
    private long lastTouchTime = 0;        // para detectar toques consecutivos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mongoDBHelper = new MongoDBHelper();
        mongoDBHelper.checkConnection(isConnected -> {
            Log.d(TAG, "QuimicAR: OnConnectionCheckListener - onConnectionChecked. isConnected: " + isConnected);
            if (isConnected) {
                Toast.makeText(QuimicAR.this, "Conectado ao MongoDB!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Conexão com o banco de dados estabelecida.");
            } else {
                Toast.makeText(QuimicAR.this, "Erro na conexão com o MongoDB!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Falha na conexão com o banco de dados.");
            }
        });


        // Initialize TextRecognitionHelper
        textRecognitionHelper = new TextRecognitionHelper(new TextRecognitionListener() {
            @Override
            public void onObjectFound(String object3DPath, String config3DPath, String texturaPath) {
                  if (isObjectFound) {
                    return; // evita executar novamente
                }
                Log.d(TAG, "Objeto 3D encontrado!");

                // Verificação para evitar crash
                if (object3DPath == null || texturaPath == null) {
                    Log.e(TAG, "Caminho do modelo ou textura está NULL!");
                    return;
                }
                 Log.d(TAG, "Renderizando Objeto3D!");
                Log.d(TAG, "object3DPath: " + object3DPath);
                Log.d(TAG, "texturaPath: " + texturaPath);

                isObjectFound = true;

                surfaceView.queueEvent(() -> {
                    try {

                        augmentedImageRenderer.loadModelOnGlThread(QuimicAR.this, object3DPath, texturaPath);

                        if (lastFrame != null) {
                            Pose pose = lastFrame.getCamera().getPose()
                                    .compose(Pose.makeTranslation(0, 0, -1f));
                            currentAnchor = session.createAnchor(pose);
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(QuimicAR.this, "Carregando modelo 3D... Mantenha-se parado", Toast.LENGTH_LONG).show();
                            fitToScanView.setVisibility(View.GONE);
                        });

                    } catch (IOException e) {
                        Log.e(TAG, "Erro ao carregar modelo 3D", e);
                    }
                });
            }

            @Override
            public void onTextRecognitionFailed(Exception e) {
                Log.e(TAG, "Falha no reconhecimento de texto: " + e.getMessage());
            }
        }, mongoDBHelper);

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

        try {
            backgroundRenderer.createOnGlThread(/*context=*/ this);
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

            session.setCameraTextureName(backgroundRenderer.getTextureId());
            backgroundRenderer.draw(frame);
            lastFrame = frame;

            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

              if (camera.getTrackingState() == TrackingState.TRACKING) {
                Image image = null;
                try {
                    image = frame.acquireCameraImage();
                    if (isObjectFound) { // só extrai se ainda não encontrou objeto
                    } else{
                    if (image != null) {
                        Bitmap bitmap = convertYUVToBitmap(image);
                        textRecognitionHelper.extractTextFromImage(bitmap);
                    }}
                } catch (NotYetAvailableException e) {
                    Log.w(TAG, "Imagem da câmera ainda não disponível: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Falha ao capturar a imagem da câmera: " + e.getMessage());
                } finally {
                    if (image != null) {
                        image.close();
                    }

            if (currentAnchor != null) {
                augmentedImageRenderer.draw(
                        viewmtx,
                        projmtx,
                        currentAnchor,
                        colorCorrectionRgba
                );
              }
           }
          }
        } catch (Throwable t) {
            Log.e(TAG, "Erro na thread OpenGL", t);
        }
    }

    private Bitmap convertYUVToBitmap(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Formato de imagem não suportado: " + image.getFormat());
        }

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

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
        session.configure(config);
    }

    private void drawAugmentedImages(
            Frame frame, float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    String text = "Imagem " + augmentedImage.getIndex() + " detectada";
                    messageSnackbarHelper.showMessage(this, text);
                    session.getConfig().setAugmentedImageDatabase(null);
                    break;

                case TRACKING:
                    this.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    fitToScanView.setVisibility(View.GONE);
                                }
                            });

                   if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
                        Anchor centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());

                         // Salva no mapa
                        augmentedImageMap.put(augmentedImage.getIndex(), new Pair<>(augmentedImage, centerPoseAnchor));

                        // Se for um único modelo, também atualiza o currentAnchor
                        currentAnchor = centerPoseAnchor;

                        Log.d(TAG, "Anchor criado para imagem index: " + augmentedImage.getIndex());
                    }
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage.getIndex());
                    break;

                default:
                    break;
            }
        }

       for (Pair<AugmentedImage, Anchor> pair : augmentedImageMap.values()) {
         AugmentedImage augmentedImage = pair.first;
         Anchor centerAnchor = pair.second;

        if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
            augmentedImageRenderer.draw(
                viewmtx,
                projmtx,
                centerAnchor,
                colorCorrectionRgba
        );
     }
    }
    }

    private Bitmap loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("default.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "Exceção IO ao carregar bitmap da imagem.", e);
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTouchTime < 500) { // 2º toque em < 0,5s
            touchCount++;
        } else {
            touchCount = 1; // reset se passou >0,5s
        }
        lastTouchTime = currentTime;

        if (touchCount == 2) {
            textRecognitionHelper.setCanSearchDatabase(true);
            isObjectFound = false; // permite nova extração de texto
            touchCount = 0;        // reset contador
            currentAnchor = null;
            Toast.makeText(this, "Extração de texto reiniciada!", Toast.LENGTH_SHORT).show();
            runOnUiThread(() -> fitToScanView.setVisibility(View.VISIBLE));
        }
    }
    return super.onTouchEvent(event);
}

}