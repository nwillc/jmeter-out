

/*
 * Copyright (c) 2016, nwillc@gmail.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package com.github.nwillc.jmeterout;


import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.math3.util.Precision.round;

class RequestEntry {
    private static final String COMMA = ", ";
    private final String url;
    int failures = 0;
    int threads = 0;
    final List<Double> times = new LinkedList<>();
    private Max max = new Max();
    private Min min = new Min();
    private Mean mean = new Mean();
    private Percentile percentile = new Percentile();
    private StandardDeviation std = new StandardDeviation();

    RequestEntry(String url) {
        this.url = url;
    }

    private double[] toArray() {
        final double[] doubles = new double[times.size()];
        for (int i = 0; i < times.size(); i++) {
            doubles[i] = times.get(i);
        }
        return doubles;
    }

    @Override
    public String toString() {
        final double[] doubles = toArray();
        return new StringBuilder().append(url).append(COMMA)
                .append(threads).append(COMMA)
                .append(doubles.length).append(COMMA)
                .append((int) min.evaluate(doubles, 0, doubles.length)).append(COMMA)
                .append((int) max.evaluate(doubles, 0, doubles.length)).append(COMMA)
                .append((int) mean.evaluate(doubles, 0, doubles.length)).append(COMMA)
                .append(round(std.evaluate(doubles, 0, doubles.length), 3)).append(COMMA)
                .append((int) percentile.evaluate(doubles, 95.0)).append(COMMA)
                .append(failures).toString();
    }
}
