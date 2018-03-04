package com.example.fotini.electrooculogram;

import android.graphics.Paint;

import com.androidplot.xy.AdvancedLineAndPointRenderer;

public class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

    private int trailSize;

    public MyFadeFormatter(int trailSize) {
        this.trailSize = trailSize;
    }

    @Override
    public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
        // offset from the latest index:
        int offset;
        if(thisIndex > latestIndex) {
            offset = latestIndex + (seriesSize - thisIndex);
        } else {
            offset =  latestIndex - thisIndex;
        }

        float scale = 255f / trailSize;
        int alpha = (int) (255 - (offset * scale));
        getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
        return getLinePaint();
    }
}