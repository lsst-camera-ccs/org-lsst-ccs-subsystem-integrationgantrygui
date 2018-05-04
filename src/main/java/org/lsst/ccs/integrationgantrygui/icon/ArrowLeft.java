package org.lsst.ccs.integrationgantrygui.icon;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import static java.awt.Color.*;

/**
 * This class has been automatically generated using
 * <a href="http://ebourg.github.io/flamingo-svg-transcoder/">Flamingo SVG
 * transcoder</a>.
 */
public class ArrowLeft implements javax.swing.Icon {

    private static final int SIZE = 24;
    /**
     * The width of this icon.
     */
    private int width;

    /**
     * The height of this icon.
     */
    private int height;

    /**
     * The rendered image.
     */
    private BufferedImage image;

    /**
     * Creates a new transcoded SVG image.
     */
    public ArrowLeft() {
        this(SIZE, SIZE);
    }

    /**
     * Creates a new transcoded SVG image.
     */
    public ArrowLeft(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (image == null) {
            image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            double coef = Math.min((double) width / (double) SIZE, (double) height / (double) SIZE);

            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.scale(coef, coef);
            paint(g2d);
            g2d.dispose();
        }

        g.drawImage(image, x, y, null);
    }

    /**
     * Paints the transcoded SVG image on the specified graphics context.
     *
     * @param g Graphics context.
     */
    private static void paint(Graphics2D g) {
        // 
        // _0
        // _0_0
        Shape shape = new Line2D.Float(19.000000f, 12.000000f, 5.000000f, 12.000000f);
        g.setPaint(BLACK);
        g.setStroke(new BasicStroke(2, 1, 1, 4));
        g.draw(shape);

        // _0_1
        shape = new GeneralPath();
        ((GeneralPath) shape).moveTo(12.0, 19.0);
        ((GeneralPath) shape).lineTo(5.0, 12.0);
        ((GeneralPath) shape).lineTo(12.0, 5.0);

        g.draw(shape);

    }
}
