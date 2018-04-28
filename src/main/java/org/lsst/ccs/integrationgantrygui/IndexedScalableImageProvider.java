package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

/**
 * An implementation of ScalableImageProvider which works by using an IndexColorModel
 * and applies scaling by adjusting the color map.
 * @author tonyj
 */
class IndexedScalableImageProvider extends ScalableImageProvider {
    
    IndexedScalableImageProvider(int bitpix, int bZero, int bScale, int[] counts, WritableRaster rawRaster) {
        super(bitpix,bZero,bScale,counts,rawRaster);
    }


    @Override
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
