package org.lsst.ccs.integrationgantrygui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
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
    private Rectangle.Double horizontalROI;
    private Rectangle.Double verticalROI;
    private boolean showROI = true;
    private boolean zoomToROI = false;
    private Color verticalColor = new Color(1f, 0f, 0f, 0.5f);
    private Color horizontalColor = new Color(1f, 0f, 0f, 0.5f);
    private VolatileImage volatileImage;
    private Rectangle.Double zoomRegion;
    private boolean showEdges = true;
    private double hEdge1;
    private double hEdge2;
    private double vEdge1;
    private double vEdge2;

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

    void setShowEdges(boolean show) {
        this.showEdges = show;
        renderOffscreen();
        repaint();
    }

    void setEdges(double hEdge1, double hEdge2, double vEdge1, double vEdge2) {
        this.hEdge1 = hEdge1;
        this.hEdge2 = hEdge2;
        this.vEdge1 = vEdge1;
        this.vEdge2 = vEdge2;
    }

    void setROI(boolean horizontal, List<Number> roi) {
        Rectangle.Double rect = new Rectangle.Double(
                roi.get(0).doubleValue(),
                roi.get(1).doubleValue(),
                roi.get(2).doubleValue() - roi.get(0).doubleValue(),
                roi.get(3).doubleValue() - roi.get(1).doubleValue());
        if (horizontal) {
            horizontalROI = rect;
        } else {
            verticalROI = rect;
        }
        Rectangle.Double zoom = null;
        if (horizontalROI != null && verticalROI != null) {
            zoom = new Rectangle.Double();
            Rectangle2D.union(horizontalROI, verticalROI, zoom);
        } else if (horizontalROI != null) {
            zoom = horizontalROI;
        } else if (verticalROI != null) {
            zoom = verticalROI;
        }
        if (zoom != null) {
            Point.Double border = new Point.Double(0.10 * zoom.width, 0.10 * zoom.height);
            zoomRegion = new Rectangle.Double(zoom.x - border.x, zoom.y - border.y,
                    zoom.width + 2 * border.x, zoom.height + 2 * border.y);
        } else {
            zoomRegion = null;
        }
        System.out.println(zoomRegion);
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
            if (zoomToROI && zoomRegion != null) {
                g2.scale(this.getWidth() / zoomRegion.getWidth(), -this.getHeight() / zoomRegion.getHeight());
                g2.translate(-zoomRegion.x, -zoomRegion.y);
                g2.translate(0, -zoomRegion.getHeight());
            } else {
                g2.scale(((double) this.getWidth()) / originalImage.getWidth(), -((double) this.getHeight()) / originalImage.getHeight());
                g2.translate(0, -originalImage.getHeight());
            }
            Timed.execute(() -> g2.drawImage(originalImage, 0, 0, null),
                    "Offscreen paint image of type %d and size %dx%d took %dms", originalImage.getType(), originalImage.getWidth(), originalImage.getHeight()
            );
            if (showROI) {
                if (horizontalROI != null) {
                    g2.setColor(horizontalColor);
                    g2.fill(horizontalROI);
                    if (showEdges) {
                        Line2D.Double line1 = new Line2D.Double(
                                horizontalROI.x + hEdge1, horizontalROI.y,
                                horizontalROI.x + hEdge1, horizontalROI.y + horizontalROI.height);
                        g2.draw(line1);
                        Line2D.Double line2 = new Line2D.Double(
                                horizontalROI.x + hEdge2, horizontalROI.y,
                                horizontalROI.x + hEdge2, horizontalROI.y + horizontalROI.height);
                        g2.draw(line2);
                    }
                }
                if (verticalROI != null) {
                    g2.setColor(verticalColor);
                    g2.fill(verticalROI);
                    if (showEdges) {
                        Line2D.Double line1 = new Line2D.Double(
                                verticalROI.x, verticalROI.y + vEdge1,
                                verticalROI.x + verticalROI.width, verticalROI.y + vEdge1);
                        g2.draw(line1);
                        Line2D.Double line2 = new Line2D.Double(
                                verticalROI.x , verticalROI.y + vEdge2,
                                verticalROI.x + verticalROI.width, verticalROI.y + vEdge2);
                        g2.draw(line2);
                    }
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
            Timed.execute(() -> g.drawImage(volatileImage, 0, 0, this),
                    "paint image of size %dx%d took %dms", volatileImage.getWidth(), volatileImage.getHeight()
            );

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

    void setZoomToROI(boolean zoom) {
        this.zoomToROI = zoom;
        renderOffscreen();
        repaint();
    }
}
