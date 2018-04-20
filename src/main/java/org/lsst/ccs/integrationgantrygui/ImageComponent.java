package org.lsst.ccs.integrationgantrygui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Simple component for displaying a buffered image
 *
 * @author tonyj
 */
public class ImageComponent extends JComponent {

    private BufferedImage image;

    public ImageComponent() {
        image = null;
    }

    public ImageComponent(BufferedImage image) {
        setImage(image);
    }

    final void setImage(BufferedImage image) {
//        if (image.getType() != BufferedImage.TYPE_USHORT_GRAY) {
//            convertImage(image);
//        } else {
            this.image = image;
//        }
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    private void convertImage(BufferedImage image1) {
        long start = System.currentTimeMillis();
        this.image = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
        Graphics g = this.image.getGraphics();
        g.drawImage(image1, 0, 0, null);
        g.dispose();
        long stop = System.currentTimeMillis();
        System.out.printf("Coversion from %d took %dms\n", image1.getType(), stop - start);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.scale(1, -1);
            g2.translate(0,-getHeight());
            long start = System.currentTimeMillis();
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            long stop = System.currentTimeMillis();
            System.out.printf("Paint image of type %d and size %dx%d took %dms\n", image.getType(), image.getWidth(), image.getHeight(), stop - start);
        }
    }
}
