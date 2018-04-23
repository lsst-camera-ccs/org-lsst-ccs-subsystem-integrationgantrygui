package org.lsst.ccs.integrationgantrygui;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.LookupOp;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import nom.tam.fits.Header;
import nom.tam.fits.TruncatedFileException;
import nom.tam.fits.header.Standard;
import nom.tam.util.BufferedFile;

/**
 *
 * @author tonyj
 */
public class FitsFast {

    static BufferedImage readFits(File in) throws IOException, TruncatedFileException {

        long start = System.currentTimeMillis();
        BufferedFile file = new BufferedFile(in);
        Header header = new Header(file);
        long size = header.getDataSize();
        int nAxis1 = header.getIntValue(Standard.NAXIS1);
        int nAxis2 = header.getIntValue(Standard.NAXIS2);
        int bitpix = header.getIntValue(Standard.BITPIX);
        int bZero = header.getIntValue(Standard.BZERO);
        int bScale = header.getIntValue(Standard.BSCALE, 1);

        System.out.printf("%d %d %d\n", size, file.getFilePointer(), nAxis1 * nAxis2);
        ByteBuffer bb = ByteBuffer.allocateDirect(nAxis1 * nAxis2 * bitpix / 8);
        FileChannel channel = file.getChannel();
        channel.position(file.getFilePointer());
        while (bb.hasRemaining()) {
            int l = channel.read(bb);
            if (l < 0) {
                break;
            }
        }
        bb.flip();
        System.out.println(bb.remaining());
        long stop = System.currentTimeMillis();
        System.out.printf("Read image of type %d size %dx%d in %dms\n", bitpix, nAxis1, nAxis2, stop - start);

        start = System.currentTimeMillis();
        int min = Byte.MAX_VALUE;
        int max = Byte.MIN_VALUE;
        while (bb.hasRemaining()) {
            byte b = bb.get();
            if (b > max) {
                max = b;
            }
            if (b < min) {
                min = b;
            }
        }
        stop = System.currentTimeMillis();
        bb.rewind();
        System.out.println(bb.remaining());

        int nBins = 2 << 8 - 1;
        int offset = bZero - min;
        System.out.printf("Histogram: Min=%d Max=%d (binning took %dms)\n", min, max, stop - start);

        double log10 = Math.log(10.0);
        double logScaleFactor = nBins / (Math.log(nBins) / log10);
        int linearScaleFactor = nBins / (max - min);
        int width = nAxis1;
        int height = nAxis2;

        ColorModel cm
                = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        false, false, Transparency.OPAQUE,
                        DataBuffer.TYPE_BYTE);
        SampleModel sm = cm.createCompatibleSampleModel(width, height);

        DataBuffer db = new DataBufferByte(width * height);

        start = System.currentTimeMillis();

        for (int i = 0; i < width * height; i++) {
            int val = offset + bScale * bb.get();
            db.setElem(i, val * linearScaleFactor);
        }
        stop = System.currentTimeMillis();

        System.out.printf("Filling data buffer took %dms\n", stop - start);

        WritableRaster r = Raster.createWritableRaster(sm, db, null);

        BufferedImage image =  new BufferedImage(cm, r, false, null);
        
        short[] log = new short[256];
        for (int i=0; i<256; i++) {
            log[i] = (short) (Math.log(i+1) * 65535 / Math.log(256));
        }
        
//        return image;
        ShortLookupTable lut = new ShortLookupTable(0, log);
        LookupOp op = new LookupOp(lut, null);
        return op.filter(image, null);
    }

}
