package xyz.zedler.patrick.grocy.view;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class BarcodeOverlay extends View {

    private final static String TAG = BarcodeOverlay.class.getSimpleName();

    private Context context;
    private Paint framingPaint = new Paint();
    private Paint barcodePaint = new Paint();
    private List<Barcode> barcodes;
    private InputImage inputImage;
    private PreviewView previewView;
    private ArrayList<Path> quadranglePaths;
    private PreviewView.ScaleType scaleType;
    private Rect framingRect;
    private int inputImageHeight;
    private int inputImageWidth;
    private int previewViewHeight;
    private int previewViewWidth;
    private boolean portraitOrientation;

    public BarcodeOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        framingPaint.setStyle(Paint.Style.FILL);
        framingPaint.setColor(getColor(R.color.black));
        framingPaint.setAntiAlias(false);
        framingPaint.setAlpha(170);

        barcodePaint.setStyle(Paint.Style.STROKE);
        barcodePaint.setColor(getColor(R.color.white));
        barcodePaint.setAntiAlias(true);
        barcodePaint.setStrokeWidth(UnitUtil.getDp(context, 6));
        barcodePaint.setPathEffect(new CornerPathEffect(8));

        quadranglePaths = new ArrayList<>();
    }

    public void drawRectangles(List<Barcode> barcodes, InputImage inputImage, PreviewView previewView) {
        this.barcodes = barcodes;
        this.inputImage = inputImage;
        this.previewView = previewView;

        if(inputImage.getRotationDegrees() == 90 || inputImage.getRotationDegrees() == 270) {
            inputImageHeight = inputImage.getWidth();
            inputImageWidth = inputImage.getHeight();
            portraitOrientation = true;
        } else {
            inputImageHeight = inputImage.getHeight();
            inputImageWidth = inputImage.getWidth();
            portraitOrientation = false;
        }
        previewViewHeight = previewView.getHeight();
        previewViewWidth = previewView.getWidth();

        scaleType = previewView.getScaleType();

        Log.i(TAG, "drawRectangles: inputImage: " + inputImage.getHeight() + "x" + inputImage.getWidth());
        Log.i(TAG, "drawRectangles: previewView: " + previewView.getHeight() + "x" + previewView.getWidth());
        Log.i(TAG, "drawRectangles: rotation: " + inputImage.getRotationDegrees());
        Log.i(TAG, "drawRectangles: corners: " + barcodes.get(0).getCornerPoints()[0] + " " + barcodes.get(0).getCornerPoints()[1] + " " + barcodes.get(0).getCornerPoints()[2] + " " + barcodes.get(0).getCornerPoints()[3]);

        quadranglePaths.clear();
        for(Barcode barcode : barcodes) {
            if(barcode.getCornerPoints() == null) return;
            Point[] cornerPoints = barcode.getCornerPoints();
            Path path = new Path();
            path.moveTo(calcXCoordinate(cornerPoints[0]), calcYCoordinate(cornerPoints[0]));
            path.lineTo(calcXCoordinate(cornerPoints[1]), calcYCoordinate(cornerPoints[1]));
            path.lineTo(calcXCoordinate(cornerPoints[2]), calcYCoordinate(cornerPoints[2]));
            path.lineTo(calcXCoordinate(cornerPoints[3]), calcYCoordinate(cornerPoints[3]));
            path.lineTo(calcXCoordinate(cornerPoints[0]), calcYCoordinate(cornerPoints[0]));
            path.close();
            quadranglePaths.add(path);
        }

        Rect surfaceRect = new Rect(0, 0, previewViewWidth, previewViewHeight);
        framingRect = calculateFramingRect(surfaceRect);

        Rect frameInPreview = new Rect(framingRect);
        frameInPreview.offset(-surfaceRect.left, -surfaceRect.top);

        Rect previewFramingRect = new Rect(frameInPreview.left * previewViewWidth / surfaceRect.width(),
                frameInPreview.top * previewViewHeight / surfaceRect.height(),
                frameInPreview.right * previewViewWidth / surfaceRect.width(),
                frameInPreview.bottom * previewViewHeight / surfaceRect.height());

        invalidate();
    }

    public void drawRectangle(Barcode barcode, InputImage inputImage, PreviewView previewView) {
        drawRectangles(Collections.singletonList(barcode), inputImage, previewView);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(framingRect != null) {
            canvas.drawRect(0, 0, getWidth(), framingRect.top, framingPaint);
            canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom + 1, framingPaint);
            canvas.drawRect(framingRect.right + 1, framingRect.top, getWidth(), framingRect.bottom + 1, framingPaint);
            canvas.drawRect(0, framingRect.bottom + 1, getWidth(), getHeight(), framingPaint);
        }

        for(Path path : quadranglePaths) {
            canvas.drawPath(path, barcodePaint);
        }
    }

    private float calcXCoordinate(Point cornerPoint) {
        float value;
        if(!portraitOrientation && scaleType == PreviewView.ScaleType.FIT_CENTER) {
            value = (-previewViewHeight * inputImageWidth) / ((float) 2 * inputImageHeight) + previewViewWidth / (float) 2;
            return value + cornerPoint.x / (float) inputImageWidth * (previewViewWidth - 2 * value);
        } else if(portraitOrientation && scaleType == PreviewView.ScaleType.FILL_CENTER) {
            value = (previewViewWidth * inputImageHeight) / ((float) 2 * inputImageWidth) - previewViewHeight / (float) 2;
            return value + cornerPoint.x / (float) inputImageWidth * (previewViewWidth - 2 * value);
        }

        return cornerPoint.x / (float) inputImageWidth * previewViewWidth;
    }

    private float calcYCoordinate(Point cornerPoint) {
        float value;
        if(portraitOrientation && scaleType == PreviewView.ScaleType.FIT_CENTER) {
            value = (-previewViewWidth * inputImageHeight) / ((float) 2 * inputImageWidth) + previewViewHeight / (float) 2;
            return value + cornerPoint.y / (float) inputImageHeight * (previewViewHeight - 2 * value);
        } else if(!portraitOrientation && scaleType == PreviewView.ScaleType.FILL_CENTER) {
            value = (previewViewHeight * inputImageWidth) / ((float) 2 * inputImageHeight) - previewViewWidth / (float) 2;
            Log.i(TAG, "calcYCoordinate: " + value);
            return value + cornerPoint.y / (float) inputImageHeight * (previewViewHeight - 2 * value);
        }

        return cornerPoint.y / (float) inputImageHeight * previewViewHeight;
    }

    private float getCropFactor() {
        return ((previewViewHeight * inputImageWidth)
                / (float) inputImageHeight)
                / (float) previewViewWidth;
    }

    /**
     * Calculate framing rectangle, relative to the preview frame.
     *
     * Note that the SurfaceView may be larger than the container.
     *
     * Override this for more control over the framing rect calculations.
     *
     * @param surface   the SurfaceView, relative to this container
     * @return the framing rect, relative to this container
     */
    protected Rect calculateFramingRect(Rect surface) {
        Rect intersection = new Rect(surface);
        int horizontalMargin = Math.max(0, (intersection.width() - UnitUtil.getDp(getContext(), 270)) / 2);
        int verticalMargin = Math.max(0, (intersection.height() - UnitUtil.getDp(getContext(), 270)) / 2);
        intersection.inset(horizontalMargin, verticalMargin);
        return intersection;
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}
