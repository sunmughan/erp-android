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
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class BarcodeOverlay extends View {

    private Context context;
    private Paint paintRect = new Paint();
    private List<Barcode> barcodes;
    private InputImage inputImage;
    private PreviewView previewView;

    public BarcodeOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        paintRect.setStyle(Paint.Style.STROKE);
        paintRect.setColor(getColor(R.color.retro_green));
        paintRect.setAntiAlias(true);
        paintRect.setStrokeWidth(UnitUtil.getDp(context, 8));
    }

    public void drawRectangles(List<Barcode> barcodes, InputImage inputImage, PreviewView previewView) {
        this.barcodes = barcodes;
        this.inputImage = inputImage;
        this.previewView = previewView;
        invalidate();
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Pass it a list of RectF (rectBounds)
        //rectBounds.forEach { canvas.drawRect(it, paint) }
        if(barcodes == null || inputImage == null || previewView == null) return;
        for(Barcode barcode : barcodes) {
            /*if(barcode.getBoundingBox() == null) return;
            canvas.drawRect(barcode.getBoundingBox(), paintRect);*/
            if(barcode.getCornerPoints() == null) return;
            for(Point point : barcode.getCornerPoints()) {
                canvas.drawPoint(point.x, point.y, paintRect);
            }
        }
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}
