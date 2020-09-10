package xyz.zedler.patrick.grocy.barcode;

import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.LuminanceSource;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.Util;

import java.util.List;

/**
 *
 */
public class DecoderThread {
    private static final String TAG = DecoderThread.class.getSimpleName();

    private xyz.zedler.patrick.grocy.barcode.CameraInstance cameraInstance;
    private HandlerThread thread;
    private Handler handler;
    private Decoder decoder;
    private Handler resultHandler;
    private Rect cropRect;
    private boolean running = false;
    private final Object LOCK = new Object();

    private final Handler.Callback callback = message -> {
        if (message.what == R.id.zxing_decode) {
            decode((SourceData) message.obj);
        } else if (message.what == R.id.zxing_preview_failed) {
            // Error already logged. Try again.
            requestNextPreview();
        }
        return true;
    };

    public DecoderThread(CameraInstance cameraInstance, Decoder decoder, Handler resultHandler) {
        Util.validateMainThread();

        this.cameraInstance = cameraInstance;
        this.decoder = decoder;
        this.resultHandler = resultHandler;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public Rect getCropRect() {
        return cropRect;
    }

    public void setCropRect(Rect cropRect) {
        this.cropRect = cropRect;
    }

    /**
     * Start decoding.
     *
     * This must be called from the UI thread.
     */
    public void start() {
        Util.validateMainThread();

        thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper(), callback);
        running = true;
        requestNextPreview();
    }

    /**
     * Stop decoding.
     *
     * This must be called from the UI thread.
     */
    public void stop() {
        Util.validateMainThread();

        synchronized (LOCK) {
            running = false;
            handler.removeCallbacksAndMessages(null);
            thread.quit();
        }
    }

    private final xyz.zedler.patrick.grocy.barcode.PreviewCallback previewCallback = new xyz.zedler.patrick.grocy.barcode.PreviewCallback() {
        @Override
        public void onPreview(xyz.zedler.patrick.grocy.barcode.SourceData sourceData) {
            // Only post if running, to prevent a warning like this:
            //   java.lang.RuntimeException: Handler (android.os.Handler) sending message to a Handler on a dead thread

            // synchronize to handle cases where this is called concurrently with stop()
            synchronized (LOCK) {
                if (running) {
                    // Post to our thread.
                    handler.obtainMessage(R.id.zxing_decode, sourceData).sendToTarget();


                }
            }
        }

        @Override
        public void onPreviewError(Exception e) {
            synchronized (LOCK) {
                if (running) {
                    // Post to our thread.
                    handler.obtainMessage(R.id.zxing_preview_failed).sendToTarget();
                }
            }
        }
    };

    private void requestNextPreview() {
        cameraInstance.requestPreview(previewCallback);
    }

    protected LuminanceSource createSource(xyz.zedler.patrick.grocy.barcode.SourceData sourceData) {
        if (this.cropRect == null) {
            return null;
        } else {
            return sourceData.createSource();
        }
    }

    private void decode(xyz.zedler.patrick.grocy.barcode.SourceData sourceData) {

        InputImage image = InputImage.fromByteArray(
                sourceData.getData(),
                sourceData.getDataWidth(),
                sourceData.getDataHeight(),
                sourceData.getRotation(),
                InputImage.IMAGE_FORMAT_NV21 // or IMAGE_FORMAT_YV12
        );

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8)
                        .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    // Task completed successfully
                    // ...
                    //Log.i(TAG, "onPreview: " + barcodes);
                    for(Barcode barcode : barcodes) {
                        Log.i(TAG, "decode: " + barcode.getRawValue());
                    }
                    if(barcodes.isEmpty()) Log.i(TAG, "decode: []");
                    requestNextPreview();
                })
                .addOnFailureListener(e -> {
                    // Task failed with an exception
                    // ...
                    //Log.e(TAG, "onPreview: " + e);
                    requestNextPreview();
                });

        /*long start = System.currentTimeMillis();
        Result rawResult = null;
        sourceData.setCropRect(cropRect);
        LuminanceSource source = createSource(sourceData);

        if (source != null) {
            rawResult = decoder.decode(source);
        }*/

        /*if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            if (resultHandler != null) {
                BarcodeResult barcodeResult = new BarcodeResult(rawResult, sourceData);
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, barcodeResult);
                Bundle bundle = new Bundle();
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (resultHandler != null) {
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_failed);
                message.sendToTarget();
            }
        }*/
        /*if (resultHandler != null) {
            List<ResultPoint> resultPoints = BarcodeResult.transformResultPoints(decoder.getPossibleResultPoints(), sourceData);
            Message message = Message.obtain(resultHandler, R.id.zxing_possible_result_points, resultPoints);
            message.sendToTarget();
        }*/
        //requestNextPreview();
    }
}
