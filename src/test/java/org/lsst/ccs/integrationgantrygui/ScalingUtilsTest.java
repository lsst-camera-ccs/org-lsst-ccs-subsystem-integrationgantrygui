package org.lsst.ccs.integrationgantrygui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.lsst.ccs.integrationgantrygui.ScalingUtils.ByteScalingUtils;
import org.lsst.ccs.integrationgantrygui.ScalingUtils.ShortScalingUtils;

/**
 *
 * @author tonyj
 */
public class ScalingUtilsTest {

    private int[] counts = {0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0};

    @Test
    public void linearByteTest() {

        ByteScalingUtils utils = new ByteScalingUtils(counts);
        assertEquals(3, utils.getMin());
        assertEquals(21, utils.getMax());
        
        byte[] buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.LINEAR);
        assertEquals(128, mean(buildArray,utils.getMin(),utils.getMax()), 1.0);

        buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.LOG);
        assertEquals(179, mean(buildArray,utils.getMin(),utils.getMax()), 1.0);

        buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.HIST);
        assertEquals(132.6, mean(buildArray,utils.getMin(),utils.getMax()), 1.0);
    }

    @Test
    public void linearShortTest() {
        ShortScalingUtils utils = new ShortScalingUtils(counts);
        assertEquals(3, utils.getMin());
        assertEquals(21, utils.getMax());
        
        short[] buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.LINEAR);
        assertEquals(128*256, mean(buildArray,utils.getMin(),utils.getMax()), 1.0);

        buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.LOG);
        assertEquals(46083, mean(buildArray,utils.getMin(),utils.getMax()), 256.0);

        buildArray = utils.buildArray(0, ScalableImageProvider.Scaling.HIST);
        assertEquals(132.6*256, mean(buildArray,utils.getMin(),utils.getMax()), 256.0);
    }
    
    double mean(byte[] array, int first, int last) {
        double sum = 0;
        int n = 0;
        for (int i=first; i<=last; i++) {
           sum += array[i] & 0xff;
           n++;
        }
        return sum/n;
    }
    
    double mean(short[] array, int first, int last) {
        double sum = 0;
        int n = 0;
        for (int i=first; i<=last; i++) {
           sum += array[i] & 0xffff;
           n++;
        }
        return sum/n;
    }
}
