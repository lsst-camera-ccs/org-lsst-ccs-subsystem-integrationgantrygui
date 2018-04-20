package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import nom.tam.fits.Header;
import nom.tam.fits.TruncatedFileException;
import nom.tam.fits.header.Standard;
import nom.tam.util.BufferedFile;

/**
 *
 * @author tonyj
 */
public class FitsFast2 {

    static BufferedImage readFits(File in) throws IOException, TruncatedFileException {
        try (BufferedFile file = new BufferedFile(in)) {
            Header header = new Header(file);
            long size = header.getDataSize();
            int nAxis1 = header.getIntValue(Standard.NAXIS1);
            int nAxis2 = header.getIntValue(Standard.NAXIS2);
            int bitpix = header.getIntValue(Standard.BITPIX);
            int bZero = header.getIntValue(Standard.BZERO);
            int bScale = header.getIntValue(Standard.BSCALE, 1);
            final int imageSize = nAxis1 * nAxis2;

            System.out.printf("%d %d %d\n", size, file.getFilePointer(), imageSize);
            DataBuffer db = new DataBufferByte(imageSize);
            long start = System.currentTimeMillis();
            for (int i = 0; i < imageSize; i++) {
                db.setElem(i, file.readByte());
            }
            long stop = System.currentTimeMillis();
            System.out.printf("Read image of type %d size %dx%d in %dms\n", bitpix, nAxis1, nAxis2, stop - start);

            byte[] r = {(byte) 0, (byte) 16, (byte) 32, (byte) 48, (byte) 64, (byte) 80, (byte) 96, (byte) 108, (byte) 124, (byte) 140, (byte) 156, (byte) 172, (byte) 188, (byte) 204, (byte) 220, (byte) 236, (byte) 252};
            ColorModel cm = new IndexColorModel(8, 17, r, r, r);

            int width = nAxis1;
            int height = nAxis2;

            SampleModel sm = cm.createCompatibleSampleModel(width, height);
            System.out.println("sm="+sm.getClass());
            WritableRaster rr = Raster.createWritableRaster(sm, db, null);
            System.out.println("rr="+rr.getClass());
            return new BufferedImage(cm, rr, false, null);
        }
    }
}
