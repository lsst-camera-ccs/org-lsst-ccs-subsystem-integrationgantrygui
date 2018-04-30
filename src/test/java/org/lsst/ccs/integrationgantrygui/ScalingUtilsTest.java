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
        byte[] buildArray = utils.buildArray(ScalableImageProvider.Scaling.LINEAR);
        int sum = 0;
        int n = 0;
        for (int i = utils.getMin(); i <= utils.getMax(); i++) {
            sum += buildArray[i] & 0xff;
            n++;
        }
        assertEquals(128, sum / n, 1.0);

        buildArray = utils.buildArray(ScalableImageProvider.Scaling.LOG);
    }

    @Test
    public void linearShortTest() {
        ShortScalingUtils utils = new ShortScalingUtils(counts);
        assertEquals(3, utils.getMin());
        assertEquals(21, utils.getMax());
        short[] buildArray = utils.buildArray(ScalableImageProvider.Scaling.LINEAR);
        int sum = 0;
        int n = 0;
        for (int i = utils.getMin(); i <= utils.getMax(); i++) {
            sum += buildArray[i] & 0xffff;
            n++;
        }
        assertEquals(128 * 256, sum / n, 1.0 * 256);
    }

}
