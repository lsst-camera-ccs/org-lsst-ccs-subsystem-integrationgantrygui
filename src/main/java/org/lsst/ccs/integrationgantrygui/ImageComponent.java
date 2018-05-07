package org.lsst.ccs.integrationgantrygui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JComponent;

/**
 * Simple component for displaying a buffered image
 *
 * @author tonyj
 */
public class ImageComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    private BufferedImage image;
    private Rectangle horizontalROI;
    private Rectangle verticalROI;
    private boolean showROI = true;

    public ImageComponent() {
        image = null;
    }

    ImageComponent(BufferedImage image) {
        setImage(image);
    }

    final void setImage(BufferedImage image) {

        this.image = image;
        repaint();
    }

    BufferedImage getImage() {
        return image;
    }

    public void setShowROI(boolean showROI) {
        this.showROI = showROI;
        repaint();
    }

    void setROI(boolean horizontal, List<Integer> roi) {
        Rectangle rect = new Rectangle(roi.get(0), roi.get(1), roi.get(2) - roi.get(0), roi.get(3) - roi.get(1));
        if (horizontal) {
            horizontalROI = rect;
        } else {
            verticalROI = rect;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.scale(((double) this.getWidth()) / image.getWidth(), -((double) this.getHeight()) / image.getHeight());
            g2.translate(0, -image.getHeight());
            Timed.execute(
                    () -> g.drawImage(image, 0, 0, ImageComponent.this),
                    "Paint image of type %d and size %dx%d took %dms", image.getType(), image.getWidth(), image.getHeight()
            );
            if (showROI) {
                g2.setColor(ROI_COLOR);
                if (horizontalROI != null) {
                    g2.fill(horizontalROI);
                }
                if (verticalROI != null) {
                    g2.fill(verticalROI);
                }
            }
        }
    }
    private static final Color ROI_COLOR = new Color(1f, 0f, 0f, 0.5f);
}
