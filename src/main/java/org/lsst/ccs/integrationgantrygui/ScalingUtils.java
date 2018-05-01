package org.lsst.ccs.integrationgantrygui;

import java.util.logging.Logger;

/**
 *
 * @author tonyj
 * @param <T>
 */
public abstract class ScalingUtils<T> {

    private static final Logger LOG = Logger.getLogger(ScalingUtils.class.getName());

    private int[] counts;
    private int min;
    private int max;

    ScalingUtils(int[] counts) {
        this.counts = counts;
        computeMinMax();
    }

    private void computeMinMax() {
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = counts.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                max = i;
                break;
            }
        }
        LOG.fine(() -> String.format("min=%d max=%d", min, max));
    }

    private int[] computeCDF() {
        int[] cdf = new int[counts.length];
        int cum = 0;
        for (int i = min; i <= max; i++) {
            cum += counts[i];
            cdf[i] = cum;
        }
        return cdf;
    }

    T buildArray(T data, int offset, double maxValue, ScalableImageProvider.Scaling scaling) {

        switch (scaling) {

            case LOG:
                double logScaleFactor = maxValue / Math.log1p(max - min);
                for (int i = 0; i < max - min + 1; i++) {
                    setElement(data, min + i - offset, Math.log1p(i) * logScaleFactor);
                }
                break;

            case LINEAR:
                double linearScaleFactor = maxValue / (max - min);
                for (int i = 0; i < max - min + 1; i++) {
                    setElement(data, min + i - offset, i * linearScaleFactor);
                }
                break;

            case HIST:
                int[] cdf = computeCDF();
                int range = cdf[max] - cdf[min];

                for (int i = 0; i < max - min + 1; i++) {
                    setElement(data, min + i - offset, (cdf[min + i] - cdf[min]) * maxValue / range);
                }
                break;

            default:
                throw new UnsupportedOperationException("Scaling: " + scaling);
        }
        return data;
    }

    int getMax() {
        return max;
    }

    int getMin() {
        return min;
    }

    abstract T buildArray(int offset, ScalableImageProvider.Scaling scaling);

    abstract void setElement(T data, int index, double value);

    static class ByteScalingUtils extends ScalingUtils<byte[]> {

        public ByteScalingUtils(int[] counts) {
            super(counts);
        }

        @Override
        byte[] buildArray(int offset, ScalableImageProvider.Scaling scaling) {
            byte[] result = new byte[getMax() + 1 - offset];
            buildArray(result, offset, 255, scaling);
//            for (int i = 0; i < result.length; i++) {
//                System.out.printf("%s[%d] = %d\n", scaling, i, result[i] & 0xff);
//            }
            return result;
        }

        @Override
        void setElement(byte[] data, int index, double value) {
            data[index] = (byte) value;
        }
    }

    static class ShortScalingUtils extends ScalingUtils<short[]> {

        public ShortScalingUtils(int[] counts) {
            super(counts);
        }

        @Override
        short[] buildArray(int offset, ScalableImageProvider.Scaling scaling) {
            short[] result = new short[getMax() + 1 - offset];
            buildArray(result, offset, 65535, scaling);
//            for (int i = 0; i < result.length; i++) {
//                System.out.printf("%s[%d] = %d\n", scaling, i, result[i] & 0xffff);
//            }
            return result;
        }

        @Override
        void setElement(short[] data, int index, double value) {
            data[index] = (short) value;
        }
    }
}
