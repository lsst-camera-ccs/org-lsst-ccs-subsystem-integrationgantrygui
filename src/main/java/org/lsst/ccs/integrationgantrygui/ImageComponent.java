package org.lsst.ccs.integrationgantrygui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.List;
import javax.swing.JComponent;

/**
 * Simple component for displaying a buffered image
 *
 * @author tonyj
 */
public class ImageComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    private BufferedImage originalImage;
    private Rectangle horizontalROI;
    private Rectangle verticalROI;
    private boolean showROI = true;
    private Color verticalColor = new Color(1f, 0f, 0f, 0.5f);
    private Color horizontalColor = new Color(1f, 0f, 0f, 0.5f);
    private VolatileImage volatileImage;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ImageComponent() {
        originalImage = null;
        setPreferredSize(new Dimension(300, 300));
    }

    ImageComponent(BufferedImage image) {
        setImage(image);
    }

    final void setImage(BufferedImage image) {

        this.originalImage = image;
        renderOffscreen();
        repaint();
    }

    BufferedImage getImage() {
        return originalImage;
    }

    public void setShowROI(boolean showROI) {
        this.showROI = showROI;
        renderOffscreen();
        repaint();
    }

    void setROI(boolean horizontal, List<Integer> roi) {
        Rectangle rect = new Rectangle(roi.get(0), roi.get(1), roi.get(2) - roi.get(0), roi.get(3) - roi.get(1));
        if (horizontal) {
            horizontalROI = rect;
        } else {
            verticalROI = rect;
        }
        renderOffscreen();
        repaint();
    }

    void renderOffscreen() {
        if (volatileImage == null || volatileImage.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
            // old vImg doesn't work with new GraphicsConfig; re-create it
            volatileImage = createVolatileImage(this.getWidth(), this.getHeight());
        }
        if (originalImage != null && volatileImage != null) {
            Graphics2D g2 = volatileImage.createGraphics();
            g2.scale(((double) this.getWidth()) / originalImage.getWidth(), -((double) this.getHeight()) / originalImage.getHeight());
            g2.translate(0, -originalImage.getHeight());
            Timed.execute(() -> g2.drawImage(originalImage, 0, 0, null),
                    "Offscreen paint image of type %d and size %dx%d took %dms", originalImage.getType(), originalImage.getWidth(), originalImage.getHeight()
            );
            if (showROI) {
                if (horizontalROI != null) {
                    g2.setColor(horizontalColor);
                    g2.fill(horizontalROI);
                }
                if (verticalROI != null) {
                    g2.setColor(verticalColor);
                    g2.fill(verticalROI);
                }
            }
            g2.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
            do {
                int returnCode;
                if (volatileImage == null) {
                    returnCode = VolatileImage.IMAGE_INCOMPATIBLE;
                } else if (volatileImage.getWidth() != this.getWidth() || volatileImage.getHeight() != this.getHeight()) {
                    returnCode = VolatileImage.IMAGE_INCOMPATIBLE;     
                } else {
                    returnCode = volatileImage.validate(getGraphicsConfiguration());
                }

                if (returnCode == VolatileImage.IMAGE_RESTORED) {
                    // Contents need to be restored
                    renderOffscreen();      // restore contents
                } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    // old vImg doesn't work with new GraphicsConfig; re-create it
                    volatileImage = createVolatileImage(this.getWidth(), this.getHeight());
                    renderOffscreen();
                }
                g.drawImage(volatileImage, 0, 0, this);
            } while (volatileImage.contentsLost());
    }

    public Color getVerticalColor() {
        return verticalColor;
    }

    public void setVerticalColor(Color verticalColor) {
        this.verticalColor = deriveAlpha(verticalColor);
    }

    public Color getHorizontalColor() {
        return horizontalColor;
    }

    public void setHorizontalColor(Color horizontalColor) {
        this.horizontalColor = deriveAlpha(horizontalColor);
    }

    private static Color deriveAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }
}
