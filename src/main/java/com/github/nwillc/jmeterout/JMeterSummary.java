package com.github.nwillc.jmeterout;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.nwillc.jmeterout.Stats.avg;
import static com.github.nwillc.jmeterout.Stats.percentile;

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

    public static void main(String args[]) {
        try {
            if (args.length < 1) {
                printUsage();
                throw new IllegalArgumentException("Must provide a JMeter output file as an argument.");
            }

            File outputFile = new File(args[0]);
            if (!outputFile.exists()) {
                throw new FileNotFoundException("File '" + outputFile + "' does not exist.");
            }

            JMeterSummary instance = new JMeterSummary(outputFile);
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

    private static void printUsage() {
        System.out.println("Usage: " + JMeterSummary.class.getName() + " <JMeter Ouput File> [Millis Per Bucket]");
        System.out.println("  (By default hits are grouped in " + DEFAULT_MILLIS_BUCKET + " millis/bucket.)");
    }

    private JMeterSummary(File inJmeterOutput) {
        super();
        _jmeterOutput = inJmeterOutput;
    }

    private void run() throws IOException {
        Map<String, UrlEntry> urlMap = new HashMap<>();

        Pattern p = Pattern.compile(createRegex());

        try (BufferedReader inStream = new BufferedReader(new FileReader(_jmeterOutput))) {
            String line = inStream.readLine();
            while (line != null) {
                Matcher m = p.matcher(line);

                if (m.find()) {
                    String url = m.group(Group.LB.ordinal());
                    UrlEntry urlEntry = urlMap.get(url);
                    if (urlEntry == null) {
                        urlEntry = new UrlEntry(url);
                        urlMap.put(url, urlEntry);
                    }
                    add(m, urlEntry);
                }

                line = inStream.readLine();
            }

        }

        if (urlMap.isEmpty()) {
            System.out.println("No results found!");
            return;
        }

        System.out.println("request, cnt, min, max, avg, 95th, failures");

        urlMap.values().forEach(System.out::println);
    }

    private void add(Matcher inM, UrlEntry inTotal) {
        int time = Integer.parseInt(inM.group(Group.T.ordinal()));
        inTotal.times.add(time);
        if (!inM.group(Group.S.ordinal()).equalsIgnoreCase("true")) {
            inTotal.failures++;
        }
    }



}
