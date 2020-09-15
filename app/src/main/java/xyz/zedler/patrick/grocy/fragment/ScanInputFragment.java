package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Animatable;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentScanInputBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class ScanInputFragment extends BaseFragment {

    private final static String TAG = ScanInputFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentScanInputBinding binding;
    private SharedPreferences sharedPrefs;
    private Camera camera;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentScanInputBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        binding.frameBack.setOnClickListener(v -> activity.onBackPressed());

        binding.previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException ignored) {}
        }, ContextCompat.getMainExecutor(activity));

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setHideOnScroll(false);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_scan_input,
                animated,
                this::setUpBottomMenu
        );
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector;
        if(sharedPrefs.getBoolean(Constants.PREF.USE_FRONT_CAM, false)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        }

        preview.setSurfaceProvider(binding.previewView.createSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8)
                        .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(activity), imageProxy -> {
            @SuppressLint("UnsafeExperimentalUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            Bitmap bitmap = toBitmap(mediaImage);

            Rect surfaceRect = new Rect(0, 0, binding.previewView.getWidth(), binding.previewView.getHeight());
            int horizontalMargin = Math.max(0, (surfaceRect.width() - UnitUtil.getDp(activity, 270)) / 2);
            int verticalMargin = Math.max(0, (surfaceRect.height() - UnitUtil.getDp(activity, 270)) / 2);
            surfaceRect.inset(horizontalMargin, verticalMargin);



            InputImage inputImage = InputImage.fromBitmap(
                    bitmap,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(inputImage).addOnSuccessListener(barcodes -> {
                imageProxy.close();
                if(barcodes.isEmpty() || binding == null) return;

                binding.barcodeOverlay.drawRectangle(
                        barcodes.get(0),
                        inputImage,
                        binding.previewView
                );

                /*new VibratorUtil(activity).tick();
                imageAnalysis.clearAnalyzer();
                setForPreviousFragment(Constants.ARGUMENT.BARCODE, barcodes.get(0).getRawValue());
                activity.navigateUp();*/
            }).addOnFailureListener(e -> imageProxy.close());
        });

        camera = cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                cameraSelector,
                imageAnalysis,
                preview
        );
        camera.getCameraInfo().getTorchState().observe(getViewLifecycleOwner(), state -> {
            MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
            if(menuItem == null) return;
            menuItem.setIcon(state == TorchState.ON
                    ? R.drawable.ic_round_flash_on_to_off
                    : R.drawable.ic_round_flash_off_to_on);
            if(menuItem.getIcon() instanceof Animatable) ((Animatable) menuItem.getIcon()).start();
        });
    }

    private Bitmap toBitmap(Image image) {
        // source: https://stackoverflow.com/a/58568495
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public void setUpBottomMenu() {
        MenuItem menuItemTorch;
        menuItemTorch = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        if(menuItemTorch == null) return;

        if(!hasFlash()) menuItemTorch.setVisible(false);
        if(hasFlash()) menuItemTorch.setOnMenuItemClickListener(item -> {
            toggleTorch();
            return true;
        });
    }

    private void toggleTorch() {
        if(camera == null) return;
        if(camera.getCameraInfo().getTorchState().getValue() == null) return;
        int state = camera.getCameraInfo().getTorchState().getValue();
        setTorch(state == TorchState.OFF);
    }

    private void setTorch(boolean enabled) {
        if(camera == null) return;
        if(camera.getCameraInfo().getTorchState().getValue() == null) return;
        int state = camera.getCameraInfo().getTorchState().getValue();
        if(enabled && state == TorchState.ON || !enabled && state == TorchState.OFF) return;
        camera.getCameraControl().enableTorch(enabled);
    }

    private boolean hasFlash() {
        return activity.getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
