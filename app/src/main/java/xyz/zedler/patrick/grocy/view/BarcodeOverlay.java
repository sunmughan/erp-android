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
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class BarcodeOverlay extends View {

    private final static String TAG = BarcodeOverlay.class.getSimpleName();

    private Context context;
    private Paint paintRect = new Paint();
    private List<Barcode> barcodes;
    private InputImage inputImage;
    private PreviewView previewView;
    private ArrayList<Path> quadranglePaths;
    private int inputImageHeight;
    private int inputImageWidth;
    private int previewViewHeight;
    private int previewViewWidth;

    public BarcodeOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        paintRect.setStyle(Paint.Style.STROKE);
        paintRect.setColor(getColor(R.color.white));
        paintRect.setAntiAlias(true);
        paintRect.setStrokeWidth(UnitUtil.getDp(context, 6));
        paintRect.setPathEffect(new CornerPathEffect(8));

        quadranglePaths = new ArrayList<>();
    }

    public void drawRectangles(List<Barcode> barcodes, InputImage inputImage, PreviewView previewView) {
        this.barcodes = barcodes;
        this.inputImage = inputImage;
        this.previewView = previewView;

        if(inputImage.getRotationDegrees() == 90 || inputImage.getRotationDegrees() == 270) {
            inputImageHeight = inputImage.getWidth();
            inputImageWidth = inputImage.getHeight();
        } else {
            inputImageHeight = inputImage.getHeight();
            inputImageWidth = inputImage.getWidth();
        }
        previewViewHeight = previewView.getHeight();
        previewViewWidth = previewView.getWidth();

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
        invalidate();
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        /*if(barcodes == null || inputImage == null || previewView == null) return;
        for(Barcode barcode : barcodes) {
            if(barcode.getCornerPoints() == null) return;

            for(Point point : barcode.getCornerPoints()) {
                canvas.drawPoint(
                        point.x / (float) inputImageWidth * getCropFactor() * previewViewWidth,
                        point.y / (float) inputImageHeight * previewViewHeight,
                        paintRect
                );
            }
        }*/

        for(Path path : quadranglePaths) {
            canvas.drawPath(path, paintRect);
        }
    }

    private float calcXCoordinate(Point cornerPoint) {
        return cornerPoint.x / (float) inputImageWidth * getCropFactor() * previewViewWidth;
    }

    private float calcYCoordinate(Point cornerPoint) {
        return cornerPoint.y / (float) inputImageHeight * previewViewHeight;
    }

    private float getCropFactor() {
        return ((previewViewHeight * inputImageWidth)
                / (float) inputImageHeight)
                / (float) previewViewWidth;
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}
