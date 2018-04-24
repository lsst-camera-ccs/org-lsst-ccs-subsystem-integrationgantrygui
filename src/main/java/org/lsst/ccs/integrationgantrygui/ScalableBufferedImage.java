package org.lsst.ccs.integrationgantrygui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.util.Vector;

/**
 *
 * @author tonyj
 */
class ScalableBufferedImage extends BufferedImage {

    private final int min;
    private final int max;
    private final WritableRaster data;

    public enum Scaling {
        LINEAR, LOG
    }
    Scaling scaling = Scaling.LOG;
    BufferedImage delegate;

    ScalableBufferedImage(int min, int max, WritableRaster raster) {
        super(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
        this.min = min;
        this.max = max;
        this.data = raster;
        delegate = new BufferedImage(buildColorModel(min, max), raster, false, null);
    }

    final IndexColorModel buildColorModel(int min, int max) {
        switch (scaling) {
            case LOG:
                byte[] log = new byte[max];
                for (int i = 0; i < max - min; i++) {
                    log[min + i] = (byte) (Math.log(i + 1) * 255 / Math.log(max - min));
                }
                return new IndexColorModel(8, max, log, log, log);
                
            case LINEAR:
                byte[] lin = new byte[max];
                for (int i = 0; i < max - min; i++) {
                    lin[min + i] = (byte) (i * 255 /(max - min));
                }
                return new IndexColorModel(8, max, lin, lin, lin);       
            default:
                throw new IllegalArgumentException("Unknown scaling");
        }

    }

    void setScaling(Scaling scaling) {
        if (this.scaling != scaling) {
           this.scaling = scaling;
           delegate = new BufferedImage(buildColorModel(min, max), data, false, null);
        }
    }

    Scaling getScaling() {
        return scaling;
    }

    @Override
    public Image getScaledInstance(int width, int height, int hints) {
        return delegate.getScaledInstance(width, height, hints);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        return delegate.getCapabilities(gc);
    }

    @Override
    public void setAccelerationPriority(float priority) {
        delegate.setAccelerationPriority(priority);
    }

    @Override
    public float getAccelerationPriority() {
        return delegate.getAccelerationPriority();
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return delegate.getWidth();
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return delegate.getHeight(observer);
    }

    @Override
    public ImageProducer getSource() {
        return delegate.getSource();
    }

    @Override
    public Graphics getGraphics() {
        return delegate.getGraphics();
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return delegate.getProperty(name, observer);
    }

    @Override
    public int getType() {
        return delegate.getType();
    }

    @Override
    public ColorModel getColorModel() {
        return delegate.getColorModel();
    }

    @Override
    public WritableRaster getRaster() {
        return delegate.getRaster();
    }

    @Override
    public WritableRaster getAlphaRaster() {
        return delegate.getAlphaRaster();
    }

    @Override
    public int getRGB(int x, int y) {
        return delegate.getRGB(x, y);
    }

    @Override
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        return delegate.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    @Override
    public synchronized void setRGB(int x, int y, int rgb) {
        delegate.setRGB(x, y, rgb);
    }

    @Override
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        delegate.setRGB(startX, startY, w, h, rgbArray, offset, scansize);
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    @Override
    public Graphics2D createGraphics() {
        return delegate.createGraphics();
    }

    @Override
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        return delegate.getSubimage(x, y, w, h);
    }

    @Override
    public boolean isAlphaPremultiplied() {
        return delegate.isAlphaPremultiplied();
    }

    @Override
    public void coerceData(boolean isAlphaPremultiplied) {
        delegate.coerceData(isAlphaPremultiplied);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public Vector<RenderedImage> getSources() {
        return delegate.getSources();
    }

    @Override
    public String[] getPropertyNames() {
        return delegate.getPropertyNames();
    }

    @Override
    public int getMinX() {
        return delegate.getMinX();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    @Override
    public SampleModel getSampleModel() {
        return delegate.getSampleModel();
    }

    @Override
    public int getNumXTiles() {
        return delegate.getNumXTiles();
    }

    @Override
    public int getNumYTiles() {
        return delegate.getNumYTiles();
    }

    @Override
    public int getMinTileX() {
        return delegate.getMinTileX();
    }

    @Override
    public int getMinTileY() {
        return delegate.getMinTileY();
    }

    @Override
    public int getTileWidth() {
        return delegate.getTileWidth();
    }

    @Override
    public int getTileHeight() {
        return delegate.getTileHeight();
    }

    @Override
    public int getTileGridXOffset() {
        return delegate.getTileGridXOffset();
    }

    @Override
    public int getTileGridYOffset() {
        return delegate.getTileGridYOffset();
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        return delegate.getTile(tileX, tileY);
    }

    @Override
    public Raster getData() {
        return delegate.getData();
    }

    @Override
    public Raster getData(Rectangle rect) {
        return delegate.getData(rect);
    }

    @Override
    public WritableRaster copyData(WritableRaster outRaster) {
        return delegate.copyData(outRaster);
    }

    @Override
    public void setData(Raster r) {
        delegate.setData(r);
    }

    @Override
    public void addTileObserver(TileObserver to) {
        delegate.addTileObserver(to);
    }

    @Override
    public void removeTileObserver(TileObserver to) {
        delegate.removeTileObserver(to);
    }

    @Override
    public boolean isTileWritable(int tileX, int tileY) {
        return delegate.isTileWritable(tileX, tileY);
    }

    @Override
    public Point[] getWritableTileIndices() {
        return delegate.getWritableTileIndices();
    }

    @Override
    public boolean hasTileWriters() {
        return delegate.hasTileWriters();
    }

    @Override
    public WritableRaster getWritableTile(int tileX, int tileY) {
        return delegate.getWritableTile(tileX, tileY);
    }

    @Override
    public void releaseWritableTile(int tileX, int tileY) {
        delegate.releaseWritableTile(tileX, tileY);
    }

    @Override
    public int getTransparency() {
        return delegate.getTransparency();
    }
}
