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
import com.google.ar.core.exceptions.*;
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
    private TextRecognitionHelper textRecognitionHelper;
    private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();
    private MongoDBHelper mongoDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mongoDBHelper = new MongoDBHelper();
        mongoDBHelper.checkConnection(new MongoDBHelper.OnConnectionCheckListener() {
            @Override
            public void onConnectionChecked(boolean isConnected) {
                if (isConnected) {
                    Toast.makeText(QuimicAR.this, "Conectado ao MongoDB!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Conexão com o banco de dados estabelecida.");
                    // Prossiga com a lógica do aplicativo que depende da conexão
                } else {
                    Toast.makeText(QuimicAR.this, "Erro na conexão com o MongoDB!", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Falha na conexão com o banco de dados.");
                    // Lógica para lidar com a falha de conexão
                }
            }
        });

        // Initialize TextRecognitionHelper
        textRecognitionHelper = new TextRecognitionHelper(new TextRecognitionHelper.TextRecognitionListener() {
            @Override
            public void onObjectFound(String object3DPath, String config3DPath, String texturePath) {
                // Load and render the 3D model using the file paths
                Log.d(TAG, "Objeto 3D encontrado e pronto para renderizar!");
                render3DModel(object3DPath, config3DPath, texturePath);
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

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                Image image = null;
                try {
                    image = frame.acquireCameraImage();
                    if (image != null) {
                        Bitmap bitmap = convertYUVToBitmap(image);
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

    private void render3DModel(String object3DPath, String config3DPath, String texturePath) {
        // Implement the logic to load and render the 3D model using the file paths
        // This may involve creating OpenGL buffers and setting up shaders
        Log.d(TAG, "Rendering 3D model from paths: " + object3DPath + ", " + config3DPath + ", " + texturePath);
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
            Anchor centerAnchor = augmentedImageMap.get(augmentedImage.getIndex()).second;
            switch (augmentedImage.getTrackingState()) {
                case TRACKING:
                    break;
                default:
                    break;
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
}