/*
 * foo
 */

package com.github.nwillc.jmeterout;

import java.util.List;

public final class Stats {
    private Stats() {};

    public static Integer avg(List<Integer> values) {
        long total = 0;
        for (Integer time : values) {
            total += time;
        }
        return (int)(total / values.size());
    }

    public static Integer percentile(List<Integer> values, int percent) {
        double index = (percent / 100.0f) * values.size();

        int pos = (int)index;
        if (index == Math.ceil(index)) {
            return (values.get(pos - 1) + values.get(pos))/2;
        } else {
            return values.get(pos);
        }
    }
}
