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

import java.io.*;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JMeterSummary {
    // This enumeration correlates to the XML httpSample attributes in JMeters output.
    private enum Group {
        ALL,
        t,
        it,
        lt,
        ts,
        s,
        lb,
        rc,
        rm,
        tn,
        dt,
        by,
        ng,
        na
    }

    private final File input;

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

    // Create a regex to match an httpSample tag from JMeter's output
    private static String createRegex() {
        StringBuilder stringBuilder = new StringBuilder("<httpSample\\s*");

        for (Group group : Group.values()) {
            if (group.equals(Group.ALL)) {
                continue;
            }

            stringBuilder.append(group.name());
            stringBuilder.append("=\"([^\"]*)\"\\s*");
        }

        stringBuilder.append("/>");
        return stringBuilder.toString();
    }

    private static void printUsage() {
        String jarName = null;
        try {
            CodeSource src = JMeterSummary.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                String parts[] = jar.getFile().split("/");
                jarName = parts[parts.length - 1];
            }
        } catch (Exception e) {
            jarName = "jmeter-out-all.jar";
        }
        System.out.println("Usage: java -jar " + jarName + " <JMeter Ouput File>");
    }

    private JMeterSummary(File inJmeterOutput) {
        input = inJmeterOutput;
    }

    private void run() throws IOException {
        Map<String, RequestEntry> requestMap = new HashMap<>();

        Pattern p = Pattern.compile(createRegex());

        try (BufferedReader inStream = new BufferedReader(new FileReader(input))) {
            String line = inStream.readLine();
            while (line != null) {
                Matcher matched = p.matcher(line);

                if (matched.find()) {
                    String request = matched.group(Group.lb.ordinal());
                    RequestEntry requestEntry = requestMap.get(request);
                    if (requestEntry == null) {
                        requestEntry = new RequestEntry(request);
                        requestMap.put(request, requestEntry);
                    }
                    add(matched, requestEntry);
                }

                line = inStream.readLine();
            }

        }

        if (requestMap.isEmpty()) {
            System.out.println("No results found!");
            return;
        }

        System.out.println("Request, Threads, Tot Req, Min, Max, Avg, Std, 95th, Errors");

        requestMap.values().forEach(System.out::println);
    }

    // Update data for a request based on pattern match data from a log line
    private void add(Matcher matched, RequestEntry requestEntry) {
        requestEntry.times.add(Double.parseDouble(matched.group(Group.t.ordinal())));
        requestEntry.threads = Math.max(Integer.parseInt(matched.group(Group.ng.ordinal())), requestEntry.threads);
        if (!matched.group(Group.s.ordinal()).equalsIgnoreCase("true")) {
            requestEntry.failures++;
        }
    }


}
