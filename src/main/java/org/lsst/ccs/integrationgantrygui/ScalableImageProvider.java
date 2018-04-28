package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 *
 * @author tonyj
 */
public abstract class ScalableImageProvider {

    protected int min;
    protected int max;
    protected final WritableRaster rawRaster;
    protected final int bitpix;
    protected final int[] counts;
    protected final int bZero;
    protected final int bScale;

    public enum Scaling {
        LINEAR, LOG, HIST
    }

    ScalableImageProvider(int bitpix, int bZero, int bScale, int[] counts, WritableRaster rawRaster) {
        this.bitpix = bitpix;
        this.bZero = bZero;
        this.bScale = bScale;
        this.rawRaster = rawRaster;
        this.counts = counts;
        computeMinMax();
    }

    protected final void computeMinMax() {
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = counts.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                max = i;
                break;
            }
        }
        System.out.printf("min=%d max=%d\n", min, max);
    }

    protected int[] computeCDF() {
        int[] cdf = new int[counts.length];
        int cum = 0;
        for (int i = min; i <= max; i++) {
            cum += counts[i];
            cdf[i] = cum;
        }
        return cdf;
    }

    abstract BufferedImage createScaledImage(Scaling scaling);

}
