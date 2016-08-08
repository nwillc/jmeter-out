/*
 * foo
 */

package com.github.nwillc.jmeterout;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.github.nwillc.jmeterout.Stats.avg;
import static com.github.nwillc.jmeterout.Stats.percentile;
import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 */
public class StatsTest {
    @Test
    public void testAvg() throws Exception {
        assertThat(avg(Arrays.asList(10, 20, 30, 40))).isEqualTo(25);
        assertThat(avg(Arrays.asList(10, 20, 30))).isEqualTo(20);
    }

    @Test
    public void testPercentile() throws Exception {
        List<Integer> values = Arrays.asList(43, 54, 56, 61, 62, 66, 68, 69, 69, 70, 71, 72, 77, 78, 79, 85, 87, 88, 89, 93, 95, 96, 98, 99, 99);
        assertThat(percentile(values, 90)).isEqualTo(98);
        assertThat(percentile(values, 20)).isEqualTo(64);
    }
}