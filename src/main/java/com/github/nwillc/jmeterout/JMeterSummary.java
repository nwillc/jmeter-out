package com.github.nwillc.jmeterout;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
            Copyright (c) 2016, nwillc@gmail.com

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

 */

/**
 * @author Andres.Galeano@Versatile.com
 */
public class JMeterSummary {
    private enum Group {
        ALL,
        T,
        LT,
        TS,
        S,
        LB,
        RC,
        RM,
        TN,
        DT,
        BY
    }

    private static final int DEFAULT_MILLIS_BUCKET = 500;

    private final File _jmeterOutput;
    private final int _millisPerBucket;

    /**
     */
    public static void main(String args[]) {
        try {
            int millisPerBucket;

            int argIndex = 0;

            if (args.length < 1) {
                printUsage();
                throw new IllegalArgumentException("Must provide a JMeter output file as an argument.");
            }

            String arg0 = args[argIndex++];
            if (arg0.contains("help")) {
                printUsage();
                return;
            }

            File outputFile = new File(arg0);
            if (!outputFile.exists()) {
                throw new FileNotFoundException("File '" + outputFile + "' does not exist.");
            }

            if (args.length > argIndex) {
                millisPerBucket = Integer.parseInt(args[argIndex++]);
            } else {
                millisPerBucket = DEFAULT_MILLIS_BUCKET;
            }
            JMeterSummary instance = new JMeterSummary(outputFile, millisPerBucket);
            instance.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String createRegex() {
        StringBuilder stringBuilder = new StringBuilder("<httpSample\\s*");

        for (Group group : Group.values()) {
            if (group.equals(Group.ALL)) {
                continue;
            }

            stringBuilder.append(group.name().toLowerCase());
            stringBuilder.append("=\"([^\"]*)\"\\s*");
        }

        stringBuilder.append("/>");
        return stringBuilder.toString();
    }

    /**
     */
    private static void printUsage() {
        System.out.println("Usage: " + JMeterSummary.class.getName() + " <JMeter Ouput File> [Millis Per Bucket]");
        System.out.println("  (By default hits are grouped in " + DEFAULT_MILLIS_BUCKET + " millis/bucket.)");
    }

    /**
     */
    private JMeterSummary(File inJmeterOutput, int inMillisPerBucket) {
        super();
        _jmeterOutput = inJmeterOutput;
        _millisPerBucket = inMillisPerBucket;
    }

    /**
     */
    private void run() throws IOException {
        Map<String, Totals> totalUrlMap = new HashMap<>(); // key = url, value = total

        Pattern p = Pattern.compile(createRegex());


        try (BufferedReader inStream = new BufferedReader(new FileReader(_jmeterOutput))) {
            String line = inStream.readLine();
            while (line != null) {
                Matcher m = p.matcher(line);

                if (m.find()) {
                    String url = m.group(Group.LB.ordinal());
                    Totals urlTotals = totalUrlMap.get(url);
                    if (urlTotals == null) {
                        urlTotals = new Totals(url);
                        totalUrlMap.put(url, urlTotals);
                    }
                    add(m, urlTotals);
                }

                line = inStream.readLine();
            }

        }

        if (totalUrlMap.isEmpty()) {
            System.out.println("No results found!");
            return;
        }

        System.out.println("url, cnt, avg, max, min, failures");

        totalUrlMap.values().forEach(System.out::println);
    }

    /**
     */
    private void add(Matcher inM, Totals inTotal) {
        inTotal.count++;
        long timeStamp = Long.parseLong(inM.group(Group.TS.ordinal()));
        inTotal.last_ts = Math.max(inTotal.last_ts, timeStamp);
        inTotal.first_ts = Math.min(inTotal.first_ts, timeStamp);

        int time = Integer.parseInt(inM.group(Group.T.ordinal()));
        inTotal.total_t += time;
        inTotal.max_t = Math.max(inTotal.max_t, time);
        inTotal.min_t = Math.min(inTotal.min_t, time);

        int conn = time - Integer.parseInt(inM.group(Group.LT.ordinal()));
        inTotal.total_conn += conn;
        inTotal.max_conn = Math.max(inTotal.max_conn, conn);
        inTotal.min_conn = Math.min(inTotal.min_conn, conn);

        String rc = inM.group(Group.RC.ordinal());
        Integer count = inTotal.rcMap.get(rc);
        if (count == null) {
            count = 0;
        }
        inTotal.rcMap.put(rc, count + 1);

        if (!inM.group(Group.S.ordinal()).equalsIgnoreCase("true")) {
            inTotal.failures++;
        }
    }

    /**
     * @author Andres.Galeano@Versatile.com
     */
    private class Totals {
        final String url;
        int count = 0;
        int total_t = 0;
        int max_t = 0; // will choose largest
        int min_t = Integer.MAX_VALUE; // will choose smallest
        int total_conn = 0;
        int max_conn = 0;  // will choose largest
        int min_conn = Integer.MAX_VALUE; // will choose smallest
        int failures = 0;
        long first_ts = Long.MAX_VALUE; // will choose smallest
        long last_ts = 0;  // will choose largest
        final Map<String, Integer> rcMap = new HashMap<>(); // key rc, value count

        Totals(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return  url + ", "  +
                    count + ", " +
                            (total_t / count) + ", " +
                            max_t + ", " +
                            min_t + ", " +
                            failures;

        }
    }

}
