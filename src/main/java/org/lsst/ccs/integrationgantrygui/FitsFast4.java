package org.lsst.ccs.integrationgantrygui;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
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
public class FitsFast4 {

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

            //System.out.printf("%d %d %d\n", size, file.getFilePointer(), imageSize);
            ByteBuffer bb = ByteBuffer.allocateDirect(nAxis1 * nAxis2 * bitpix / 8);
            FileChannel channel = file.getChannel();
            channel.position(file.getFilePointer());
            long start = System.currentTimeMillis();
            while (bb.hasRemaining()) {
                int l = channel.read(bb);
                if (l < 0) {
                    break;
                }
            }
            long stop = System.currentTimeMillis();

            bb.flip();
            //System.out.println(bb.remaining());
            //System.out.printf("Read image of type %d size %dx%d in %dms\n", bitpix, nAxis1, nAxis2, stop - start);

            WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, nAxis1, nAxis2, 1, new Point(0,0));
            DataBuffer db = raster.getDataBuffer();
            start = System.currentTimeMillis();

            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < imageSize; i++) {
                int pixel = bb.get();
                if (pixel<0) pixel += 256;
                if (pixel > max) {
                    max = pixel;
                }
                if (pixel < min) {
                    min = pixel;
                }
                db.setElem(i, pixel);
            }
            stop = System.currentTimeMillis();
            //System.out.printf("Write image of type %d size %dx%d range %d-%d in %dms\n", bitpix, nAxis1, nAxis2, min, max, stop - start);

            // Rescale
            byte[] log = new byte[max];
            for (int i=0; i<max-min; i++) {
                log[min+i] = (byte) (Math.log(i+1) * 255 / Math.log(max-min));
            }
            IndexColorModel cm2 = new IndexColorModel(8, max, log, log, log);
            return  new BufferedImage(cm2, raster, false, null);
        }
    }
}
