package org.lsst.ccs.integrationgantrygui;

import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.LookupOp;
import java.awt.image.RasterOp;
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

            int max = 72;
            byte[] lin = new byte[max];
            for (int i = 0; i < max; i++) {
                lin[i] = (byte) (i * 255 / max);
            }
            byte[] log = new byte[72];
            for (int i = 0; i < max; i++) {
                log[i] = (byte) (Math.log(i+1) * 255 / Math.log(max));
            }
            IndexColorModel cm = new IndexColorModel(8, max, log, log, log);
            BufferedImage image = new BufferedImage(nAxis1, nAxis2, BufferedImage.TYPE_BYTE_INDEXED, cm);
            DataBuffer db = image.getRaster().getDataBuffer();
            start = System.currentTimeMillis();
            for (int i = 0; i < imageSize; i++) {
                db.setElem(i, bb.get());
            }
            stop = System.currentTimeMillis();
            //System.out.printf("Write image of type %d size %dx%d in %dms\n", bitpix, nAxis1, nAxis2, stop - start);
            
            return image;
        }
    }
}
