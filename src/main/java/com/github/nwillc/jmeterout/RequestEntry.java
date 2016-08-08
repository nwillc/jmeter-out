

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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.github.nwillc.jmeterout.Stats.avg;
import static com.github.nwillc.jmeterout.Stats.percentile;

class RequestEntry {
    private final String url;
    int failures = 0;
    int threads = 0;
    List<Integer> times = new LinkedList<>();

    RequestEntry(String url) {
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
                failures + ", " +
                threads;
    }
}
