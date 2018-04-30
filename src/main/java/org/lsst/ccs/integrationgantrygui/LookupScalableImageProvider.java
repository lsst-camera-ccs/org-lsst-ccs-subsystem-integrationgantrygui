package org.lsst.ccs.integrationgantrygui;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;
import org.lsst.ccs.integrationgantrygui.ScalingUtils.ShortScalingUtils;

/**
 * An implementation of ScalableImageProvider which works by using a lookupOp to
 * convert the raw image.
 *
 * @author tonyj
 */
class LookupScalableImageProvider extends ScalableImageProvider<short[]> {

    private final BufferedImage rawImage;

    LookupScalableImageProvider(int bitpix, int bZero, int bScale, int[] counts, WritableRaster rawRaster) {
        super(bitpix, bZero, bScale, new ShortScalingUtils(counts), rawRaster);
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
        ScalingUtils<short[]> scalingUtils = getScalingUtils();
        short[] data = scalingUtils.buildArray(scaling);
        return new ShortLookupTable(0, data);
    }
}
