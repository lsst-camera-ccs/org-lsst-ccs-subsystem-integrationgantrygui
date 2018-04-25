package org.lsst.ccs.integrationgantrygui;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import nom.tam.fits.TruncatedFileException;

/**
 *
 * @author tonyj
 */
public class SimpleTest {
    public static void main(String args[]) throws IOException, TruncatedFileException {
        File file = new File("/home/tonyj/Data/BF3_rng0015_100000.fits");
        ScalableBufferedImage sbi = FitsFast4.readFits(file);
        ImageComponent ic = new ImageComponent(sbi);
        JFrame frame = new JFrame();
        frame.setContentPane(ic);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(600,600));
        frame.setVisible(true);
    }
}
