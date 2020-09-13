package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import java.util.concurrent.ExecutionException;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentScanInputBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.VibratorUtil;

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
        super.onViewCreated(view, savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        binding.frameBack.setOnClickListener(v -> activity.onBackPressed());

        binding.previewView.setScaleType(PreviewView.ScaleType.FILL_START);

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

            InputImage inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(inputImage).addOnSuccessListener(barcodes -> {
                imageProxy.close();
                if(barcodes.isEmpty()) return;

                binding.barcodeOverlay.drawRectangle(
                        barcodes.get(0),
                        inputImage,
                        binding.previewView
                );

                new VibratorUtil(activity).tick();
                imageAnalysis.clearAnalyzer();
                setForPreviousFragment(Constants.ARGUMENT.BARCODE, barcodes.get(0).getRawValue());
                activity.navigateUp();
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
