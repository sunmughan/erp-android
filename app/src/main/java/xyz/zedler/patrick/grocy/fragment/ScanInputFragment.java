package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
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
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavBackStackEntry;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.journeyapps.barcodescanner.BarcodeResult;

import java.util.concurrent.ExecutionException;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentScanInputBinding;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;

public class ScanInputFragment extends BaseFragment
        implements ScanInputCaptureManager.BarcodeListener {

    private final static String TAG = ScanInputFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentScanInputBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;

    private boolean isTorchOn;

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

        // GET PREFERENCES
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        binding.frameBack.setOnClickListener(v -> activity.onBackPressed());

        cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException ignored) {}
        }, ContextCompat.getMainExecutor(activity));

        isTorchOn = false;

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

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

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
                for (Barcode barcode : barcodes) {
                    Log.i(TAG, "decode: " + barcode.getRawValue());
                }
                if (barcodes.isEmpty()) Log.i(TAG, "decode: []");
                binding.barcodeOverlay.drawRectangles(barcodes);
                imageProxy.close();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "onPreview: " + e);
                imageProxy.close();
            });
        });

        camera = cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                cameraSelector,
                imageAnalysis,
                preview
        );
        camera.getCameraControl().enableTorch(isTorchOn);
    }


    public void setUpBottomMenu() {
        MenuItem menuItemTorch;
        menuItemTorch = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        if(menuItemTorch == null) return;

        if(!hasFlash()) menuItemTorch.setVisible(false);
        if(hasFlash()) menuItemTorch.setOnMenuItemClickListener(item -> {
            switchTorch(item);
            return true;
        });
    }

    private void switchTorch(MenuItem menuItem) {
        if(camera == null) return;
        if(isTorchOn) {
            camera.getCameraControl().enableTorch(false);
            menuItem.setIcon(R.drawable.ic_round_flash_off_to_on);
            if(menuItem.getIcon() instanceof Animatable) ((Animatable) menuItem.getIcon()).start();
            isTorchOn = false;
        } else {
            camera.getCameraControl().enableTorch(true);
            menuItem.setIcon(R.drawable.ic_round_flash_on_to_off);
            if(menuItem.getIcon() instanceof Animatable) ((Animatable) menuItem.getIcon()).start();
            isTorchOn = true;
        }
    }

    private boolean hasFlash() {
        return activity.getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseScan();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        //barcodeScannerView.setTorchOff();
        //capture.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) resumeScan();
        //barcodeRipple.pauseAnimation();
        NavBackStackEntry backStackEntry = findNavController().getPreviousBackStackEntry();
        assert backStackEntry != null;
        backStackEntry.getSavedStateHandle().set(Constants.ARGUMENT.BARCODE, result.getText());
        activity.navigateUp();
    }

    @Override
    public void pauseScan() {
        //barcodeRipple.pauseAnimation();
        //capture.onPause();
    }

    @Override
    public void resumeScan() {
        //barcodeRipple.resumeAnimation();
        //capture.onResume();
    }
}
