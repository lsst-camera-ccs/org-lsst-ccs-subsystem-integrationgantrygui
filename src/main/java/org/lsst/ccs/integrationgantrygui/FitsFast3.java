package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
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
public class FitsFast3 {

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
            int max = 72;
            byte[] r = new byte[72];
            for (int  i=0;i<72;i++) { r[i] = (byte) (i*256/72); }
            IndexColorModel cm = new IndexColorModel(8, max, r, r, r);
            BufferedImage image = new BufferedImage(nAxis1, nAxis2, BufferedImage.TYPE_BYTE_INDEXED, cm);
            DataBuffer db = image.getRaster().getDataBuffer();
            System.out.println(db.getClass());
            long start = System.currentTimeMillis();
            for (int i = 0; i < imageSize; i++) {
                db.setElem(i, file.readByte());
            }
            long stop = System.currentTimeMillis();
            System.out.printf("Read image of type %d size %dx%d in %dms\n", bitpix, nAxis1, nAxis2, stop - start);
            return image;
        }
    }
}
