package org.lsst.ccs.integrationgantrygui;

import edu.jhu.pha.sdss.fits.FITSImage;
import edu.jhu.pha.sdss.fits.ScaleUtils;
import edu.jhu.pha.sdss.fits.imageio.FITSReaderSpi;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import nom.tam.fits.FitsException;

/**
 *
 * @author tonyj
 */
public class FitsTest {

    public static void main(String[] args) throws FitsException, FITSImage.DataTypeNotSupportedException, FITSImage.NoImageDataFoundException, IOException {
        IIORegistry.getDefaultInstance().registerServiceProvider(new FITSReaderSpi());

        //Fits fits = new Fits("/home/tonyj/Data/arasmus/BF_FF001378_3893847934_20000.fits");
        File in = new File("/home/tonyj/Data/arasmus/BF_FF001378_3893847934_20000.fits");

        long start = System.currentTimeMillis();
//        FITSImage fitsImage = new FITSImage(fits, ScaleUtils.HIST_EQ);
        FITSImage fitsImage = (FITSImage) ImageIO.read(in);
        BufferedImage image = fitsImage;
        long stop = System.currentTimeMillis();
        System.out.printf("Read image of type %d size %dx%d in %dms\n", fitsImage.getType(), fitsImage.getWidth(), fitsImage.getHeight(), stop - start);

        ImageComponent imageComponent = new ImageComponent(image);

        imageComponent.setPreferredSize(new Dimension(1200, 1200));
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(imageComponent, BorderLayout.CENTER);

        JComboBox combo = new JComboBox(ScaleUtils.getScaleNames());
        combo.setSelectedIndex(fitsImage.getScaleMethod());
        combo.addActionListener((ActionEvent e) -> {
            fitsImage.setScaleMethod(combo.getSelectedIndex());
            imageComponent.setImage(fitsImage);
        });
        panel.add(combo, BorderLayout.SOUTH);

        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
