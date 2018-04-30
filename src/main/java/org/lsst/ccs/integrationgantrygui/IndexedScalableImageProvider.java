package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

/**
 * An implementation of ScalableImageProvider which works by using an
 * IndexColorModel and applies scaling by adjusting the color map.
 *
 * @author tonyj
 */
class IndexedScalableImageProvider extends ScalableImageProvider<byte[]> {

    IndexedScalableImageProvider(int bitpix, int bZero, int bScale, int[] counts, WritableRaster rawRaster) {
        super(bitpix, bZero, bScale, new ScalingUtils.ByteScalingUtils(counts), rawRaster);
    }

    @Override
    BufferedImage createScaledImage(Scaling scaling) {
        return new BufferedImage(buildColorModel(scaling), getRawRaster(), false, null);
    }

    final IndexColorModel buildColorModel(Scaling scaling) {
        ScalingUtils<byte[]> utils = getScalingUtils();
        byte[] data = utils.buildArray(0, scaling);
        return new IndexColorModel(getBitpix(), utils.getMax(), data, data, data);
    }
}