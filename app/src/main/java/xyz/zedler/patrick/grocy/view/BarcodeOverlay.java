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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;

public class BarcodeOverlay extends View {

    private Context context;
    private Paint paintRect = new Paint();
    private List<Barcode> barcodes;

    public BarcodeOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        paintRect.setStyle(Paint.Style.STROKE);
        paintRect.setColor(getColor(R.color.retro_green));
        paintRect.setAntiAlias(true);

        barcodes = new ArrayList<>();
    }

    public void drawRectangles(List<Barcode> barcodes) {
        this.barcodes = barcodes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Pass it a list of RectF (rectBounds)
        //rectBounds.forEach { canvas.drawRect(it, paint) }
        for(Barcode barcode : barcodes) {
            if(barcode.getBoundingBox() == null) return;
            canvas.drawRect(barcode.getBoundingBox(), paintRect);
        }
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}
