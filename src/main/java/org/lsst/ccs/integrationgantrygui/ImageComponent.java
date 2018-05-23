package org.lsst.ccs.integrationgantrygui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

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
    private Color edgeColor = Color.RED;
    private boolean preserveAspectRatio = true;
    private boolean showGrid = true;
    private final Color gridColor = Color.BLACK;
    private final Stroke gridStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[]{10.0f, 20.0f}, 0.0f);
    private int gridSize = 400;
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

        VolatileImage newImage = createVolatileImage(this.getWidth(), this.getHeight());        
        renderOffscreen(image,newImage);
        SwingUtilities.invokeLater(()->{
            this.originalImage = image;
            this.volatileImage = newImage;
            repaint();
        });
    }

    BufferedImage getImage() {
        return originalImage;
    }

    public void setShowROI(boolean showROI) {
        this.showROI = showROI;
        ImageComponent.this.renderOffscreen();
        repaint();
    }

    void setShowEdges(boolean show) {
        this.showEdges = show;
        ImageComponent.this.renderOffscreen();
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
        renderOffscreen();
        repaint();
    }

    void renderOffscreen() {
        if (volatileImage == null || volatileImage.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
            // old vImg doesn't work with new GraphicsConfig; re-create it
            volatileImage = createVolatileImage(this.getWidth(), this.getHeight());
        }
        renderOffscreen(originalImage, volatileImage);
    }
    void renderOffscreen(BufferedImage sourceImage, VolatileImage destinationImage) {
         
        if (sourceImage != null && destinationImage != null) {
            Graphics2D g2 = destinationImage.createGraphics();
            if (preserveAspectRatio) {
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            if (zoomToROI && zoomRegion != null) {
                double hScale = this.getWidth() / zoomRegion.getWidth();
                double vScale = this.getHeight() / zoomRegion.getHeight();
                if (preserveAspectRatio) {
                    vScale = hScale = Math.min(hScale, vScale);
                }
                g2.scale(hScale, -vScale);
                g2.translate(-zoomRegion.x, -zoomRegion.y);
                g2.translate(0, -zoomRegion.getHeight());
            } else {
                double hScale = ((double) getWidth()) / sourceImage.getWidth();
                double vScale = ((double) getHeight()) / sourceImage.getHeight();
                if (preserveAspectRatio) {
                    vScale = hScale = Math.min(hScale, vScale);
                }
                g2.scale(hScale, -vScale);
                g2.translate(0, -sourceImage.getHeight());
            }
            Timed.execute(() -> g2.drawImage(sourceImage, 0, 0, null),
                    "Offscreen paint image of type %d and size %dx%d took %dms", sourceImage.getType(), sourceImage.getWidth(), sourceImage.getHeight()
            );
            if (showROI) {
                if (horizontalROI != null) {
                    g2.setColor(horizontalColor);
                    g2.fill(horizontalROI);
                    if (showEdges) {
                        g2.setColor(edgeColor);
                        Line2D.Double line1 = new Line2D.Double(
                                horizontalROI.x, horizontalROI.y + hEdge1,
                                horizontalROI.x + horizontalROI.width, horizontalROI.y + hEdge1);
                        g2.draw(line1);
                        Line2D.Double line2 = new Line2D.Double(
                                horizontalROI.x, horizontalROI.y + hEdge2,
                                horizontalROI.x + horizontalROI.width, horizontalROI.y + hEdge2);
                        g2.draw(line2);
                    }
                }
                if (verticalROI != null) {
                    g2.setColor(verticalColor);
                    g2.fill(verticalROI);
                    if (showEdges) {
                        g2.setColor(edgeColor);
                        Line2D.Double line1 = new Line2D.Double(
                                verticalROI.x + vEdge1, verticalROI.y,
                                verticalROI.x + vEdge1, verticalROI.y + verticalROI.height);
                        g2.draw(line1);
                        Line2D.Double line2 = new Line2D.Double(
                                verticalROI.x + vEdge2, verticalROI.y,
                                verticalROI.x + vEdge2, verticalROI.y + verticalROI.height);
                        g2.draw(line2);
                    }
                }
            }
            if (showGrid) {
                Path2D.Double grid = new Path2D.Double();
                for (double x = 0; x < sourceImage.getWidth(); x += gridSize) {
                    grid.moveTo(x, 0);
                    grid.lineTo(x, sourceImage.getHeight());
                }
                for (double y = 0; y < sourceImage.getHeight(); y += gridSize) {
                    grid.moveTo(0, y);
                    grid.lineTo(sourceImage.getWidth(), y);
                }
                g2.setColor(gridColor);
                g2.setXORMode(getBackground());
                g2.setStroke(gridStroke);
                g2.draw(grid);
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
                ImageComponent.this.renderOffscreen();      // restore contents
            } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                // old vImg doesn't work with new GraphicsConfig; re-create it
                volatileImage = createVolatileImage(this.getWidth(), this.getHeight());
                ImageComponent.this.renderOffscreen();
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

    public Color getEdgeColor() {
        return edgeColor;
    }

    public void setEdgeColor(Color edgeColor) {
        this.edgeColor = edgeColor;
        ImageComponent.this.renderOffscreen();
        repaint();
    }

    public boolean isPreserveAspectRatio() {
        return preserveAspectRatio;
    }

    public void setPreserveAspectRatio(boolean preserveAspectRatio) {
        this.preserveAspectRatio = preserveAspectRatio;
        ImageComponent.this.renderOffscreen();
        repaint();
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        ImageComponent.this.renderOffscreen();
        repaint();
    }

    private static Color deriveAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
    }

    void setZoomToROI(boolean zoom) {
        this.zoomToROI = zoom;
        ImageComponent.this.renderOffscreen();
        repaint();
    }

    void setGridSize(int size) {
        this.gridSize = size;
        ImageComponent.this.renderOffscreen();
        repaint();        
    }
}
