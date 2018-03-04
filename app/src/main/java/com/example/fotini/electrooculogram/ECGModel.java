package com.example.fotini.electrooculogram;

import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.XYSeries;

import java.lang.ref.WeakReference;

/**
 * Primitive simulation of some kind of signal.  For this example,
 * we'll pretend its an ecg.  This class represents the data as a circular buffer;
 * data is added sequentially from left to right.  When the end of the buffer is reached,
 * i is reset back to 0 and simulated sampling continues.
 */
public class ECGModel implements XYSeries {

    private final Number[] data;
    private final long delayMs;
    private final Thread thread;
    private boolean keepRunning;
    private int latestIndex;

    private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

    /**
     *
     * @param size Sample size contained within this model
     * @param updateFreqHz Frequency at which new samples are added to the model
     */
    public ECGModel(int size, int updateFreqHz) {
        data = new Number[size];
        for(int i = 0; i < data.length; i++) {
            data[i] = 0;
        }

        // translate hz into delay (ms):
        delayMs = 1000 / updateFreqHz;

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (keepRunning) {
                        if (latestIndex >= data.length) {
                            latestIndex = 0;
                        }

                        data[latestIndex] = MainActivity.sendData();

                        if(latestIndex < data.length - 1) {
                            // null out the point immediately following i, to disable
                            // connecting i and i+1 with a line:
                            data[latestIndex +1] = null;
                        }

                        if(rendererRef.get() != null) {
                            rendererRef.get().setLatestIndex(latestIndex);
                            Thread.sleep(delayMs);
                        } else {
                            keepRunning = false;
                        }
                        latestIndex++;
                    }
                } catch (InterruptedException e) {
                    keepRunning = false;
                }
            }
        });
    }

    public void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
        this.rendererRef = rendererRef;
        keepRunning = true;
        thread.start();
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public Number getX(int index) {
        return index;
    }

    @Override
    public Number getY(int index) {
        return data[index];
    }

    @Override
    public String getTitle() {
        return "Signal";
    }
}
