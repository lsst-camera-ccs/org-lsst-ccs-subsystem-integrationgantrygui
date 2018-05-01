package org.lsst.ccs.integrationgantrygui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import nom.tam.fits.TruncatedFileException;

/**
 *
 * @author tonyj
 */
public class SimpleTest {

    public static void main(String args[]) throws IOException, TruncatedFileException {
        File file = new File("/home/tonyj/Data/11_Flat_0000_20180309071524.fits");
        ScalableImageProvider sbi = FitsFast.readFits(file);
        JPanel content = new JPanel(new BorderLayout());
        ImageComponent ic = new ImageComponent(sbi.createScaledImage(ScalableImageProvider.Scaling.LOG));
        content.add(ic, BorderLayout.CENTER);
        JComboBox<ScalableImageProvider.Scaling> scaleCombo = new JComboBox<>(ScalableImageProvider.Scaling.values());
        scaleCombo.setSelectedItem(ScalableImageProvider.Scaling.LOG);
        scaleCombo.addActionListener((ActionEvent e) -> {
            ic.setImage(sbi.createScaledImage(scaleCombo.getItemAt(scaleCombo.getSelectedIndex())));
        });
        content.add(scaleCombo, BorderLayout.SOUTH);
        JFrame frame = new JFrame();
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(600, 600));
        frame.setVisible(true);
    }
}
