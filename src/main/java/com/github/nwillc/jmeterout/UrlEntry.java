/*
 * foo
 */

package com.github.nwillc.jmeterout;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.github.nwillc.jmeterout.Stats.avg;
import static com.github.nwillc.jmeterout.Stats.percentile;

class UrlEntry {
    private final String url;
    int failures = 0;
    List<Integer> times = new LinkedList<>();

    UrlEntry(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        Collections.sort(times);
        return url + ", " +
                times.size() + ", " +
                times.get(0) + ", " +
                times.get(times.size() - 1) + ", " +
                avg(times) + ", " +
                percentile(times, 95) + ", " +
                failures;
    }
}
