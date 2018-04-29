package org.lsst.ccs.integrationgantrygui;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;

/**
 * An implementation of ScalableImageProvider which works by using a lookupOp to
 * convert the raw image.
 *
 * @author tonyj
 */
class LookupScalableImageProvider extends ScalableImageProvider {

    private final BufferedImage rawImage;

    LookupScalableImageProvider(int bitpix, int bZero, int bScale, int[] counts, WritableRaster rawRaster) {
        super(bitpix, bZero, bScale, counts, rawRaster);
        ComponentColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                false, false, Transparency.OPAQUE,
                rawRaster.getTransferType());
        rawImage = new BufferedImage(cm, rawRaster, false, null);
    }

    @Override
    BufferedImage createScaledImage(Scaling scaling) {
        return Timed.execute(() -> {        
                LookupOp op = new LookupOp(createLookupTable(scaling), null);
                return op.filter(rawImage, null);
       }, "Scaling took %dms");
    }

    private ShortLookupTable createLookupTable(Scaling scaling) {
        int nBins = 2 << 16 - 1;

        switch (scaling) {

            case LOG:
                short[] log = new short[max - min + 1];
                for (int i = 0; i <= max - min; i++) {
                    log[i] = (short) (i == 0 ? 0 : Math.log(i+1) * nBins / Math.log(max - min + 2));
                    System.out.printf("log %d: %d\n", i, log[i] & 0xffff);
                }
                return new ShortLookupTable(min, log);

            case LINEAR:
                short[] lin = new short[max - min + 1];
                double linearScaleFactor = ((double) nBins) / (max - min + 1);
                for (int i = 0; i <= max - min; i++) {
                    lin[i] = (short) (i * linearScaleFactor);
                    System.out.printf("lin %d: %d\n", i, lin[i] & 0xffff);
                }
                return new ShortLookupTable(min, lin);

            case HIST:
                int[] cdf = computeCDF();
                double range = cdf[max] - cdf[min];
                short[] hist = new short[max - min + 1];
                for (int i = 0; i <= max - min; i++) {
                   hist[i] = (short) ((cdf[min + i] - cdf[min]) / range * nBins);
                    System.out.printf("hist %d: %d\n", i, hist[i] & 0xffff);
                }
                return new ShortLookupTable(min, hist);

            default:
                throw new UnsupportedOperationException("Scaling: " + scaling);
        }
    }
}
