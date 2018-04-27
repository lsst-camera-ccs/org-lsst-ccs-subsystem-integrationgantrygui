package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

/**
 *
 * @author tonyj
 */
class ScalableImageProvider {

    private int min;
    private int max;
    private final WritableRaster rawRaster;
    private final int bitpix;
    private final int[] counts;
    private final int bZero;
    private final int bScale;
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

    private void computeMinMax() {
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
        System.out.printf("min=%d max=%d\n",min,max);
    }
    
    private int[] computeCDF() {
        int[] cdf = new int[counts.length];
        int cum = 0;
        for (int i=min; i <= max; i++) {
            cum += counts[i];
            cdf[i] = cum;
        }
        return cdf;
    }

    BufferedImage createScaledImage(Scaling scaling) {
        return new BufferedImage(buildColorModel(scaling), rawRaster, false, null);
    }

    final IndexColorModel buildColorModel(Scaling scaling) {
        int nBins = 2<<8-1;
        
        switch (scaling) {
            case LOG:
                byte[] log = new byte[max];
                for (int i = 0; i < max - min; i++) {
                    log[min + i] = (byte) (i==0 ? 0 : Math.log(i) * nBins / Math.log(max - min));
                    System.out.printf("log %d: %d\n",min+i,log[min+i]&0xff);
                }
                return new IndexColorModel(bitpix, max, log, log, log);

            case LINEAR:
                byte[] lin = new byte[max];
                double linearScaleFactor = ((double) nBins) / (max - min);
                for (int i = 0; i < max - min; i++) {
                    lin[min + i] = (byte) (i * linearScaleFactor);
                    System.out.printf("lin %d: %d\n",min+i,lin[min+i]&0xff);
                }
                return new IndexColorModel(bitpix, max, lin, lin, lin);
                
            case HIST:
                int[] cdf = computeCDF();
                int range = cdf[max] - cdf[min];
                byte[] hist = new byte[max];
                for (int i = 0; i < max - min; i++) {
                    hist[min + i] = (byte) ((cdf[min+i] - cdf[min])*nBins/range);
                    System.out.printf("hist %d: %d\n",min+i,hist[min+i]&0xff);
                }
                return new IndexColorModel(bitpix, max, hist, hist, hist);               
            default:
                throw new IllegalArgumentException("Unknown scaling");
        }
    }
}
