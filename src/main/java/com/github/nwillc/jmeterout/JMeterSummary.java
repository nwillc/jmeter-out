package com.github.nwillc.jmeterout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
		DT
	}
	private static final String REG_EX =
			"<httpSample\\s*" +
					"t=\"([^\"]*)\"\\s*" + // Group.T
					"lt=\"([^\"]*)\"\\s*" + // Group.LT
					"ts=\"([^\"]*)\"\\s*" + // Group.TS
					"s=\"([^\"]*)\"\\s*" + // Group.S
					"lb=\"([^\"]*)\"\\s*" + // Group.LB
					"rc=\"([^\"]*)\"\\s*" + // Group.RC
					"rm=\"([^\"]*)\"\\s*" + // Group.RM
					"tn=\"([^\"]*)\"\\s*" + // Group.TN
					"dt=\"([^\"]*)\"\\s*" + // Group.DT
					"/>";

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
	} // end [main(String[])]

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
		Totals totalAll = new Totals();
		Map<String, Totals> totalUrlMap = new HashMap<>(); // key = url, value = total

		Pattern p = Pattern.compile(REG_EX);


		try (BufferedReader inStream = new BufferedReader(new FileReader(_jmeterOutput))) {
			String line = inStream.readLine();
			while (line != null) {
				Matcher m = p.matcher(line);

				if (m.find()) {
					add(m, totalAll);

					String url = m.group(Group.LB.ordinal());
					Totals urlTotals = totalUrlMap.get(url);
					if (urlTotals == null) {
						urlTotals = new Totals();
						totalUrlMap.put(url, urlTotals);
					}
					add(m, urlTotals);
				}

				line = inStream.readLine();
			}

		}

		if (totalAll.count == 0) {
			System.out.println("No results found!");
			return;
		}

		System.out.println("All Urls:");
		System.out.println(totalAll.toBasicString());
		System.out.println(totalAll.toAdvancedString());
		System.out.println("");

		for (Object o : totalUrlMap.entrySet()) {

			Map.Entry entry = (Map.Entry) o;
			String url = (String) entry.getKey();
			Totals totals = (Totals) entry.getValue();

			System.out.println("URL: " + url);
			System.out.println(totals.toBasicString());
			System.out.println("");
		}
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

		Integer bucket = time / _millisPerBucket;
		count = inTotal.millisMap.get(bucket);
		if (count == null) {
			count = 0;
		}
		inTotal.millisMap.put(bucket, count + 1);

		if (!inM.group(Group.S.ordinal()).equalsIgnoreCase("true")) {
			inTotal.failures++;
		}
	}

	/**
	 * @author Andres.Galeano@Versatile.com
	 */
	private class Totals {
		private final String DECIMAL_PATTERN = "#,##0.0##";
		private final double MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);

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
		final Map<Integer, Integer> millisMap = new TreeMap<>(); // key bucket Integer, value count

		Totals() {
		}

		String toBasicString() {

			DecimalFormat df = new DecimalFormat(DECIMAL_PATTERN);

			List<String> millisStr = new LinkedList<>();

			for (Object o : millisMap.entrySet()) {
				Map.Entry millisEntry = (Map.Entry) o;
				Integer bucket = (Integer) millisEntry.getKey();
				Integer bucketCount = (Integer) millisEntry.getValue();

				int minMillis = bucket * _millisPerBucket;
				int maxMillis = (bucket + 1) * _millisPerBucket;

				millisStr.add(
						df.format(minMillis / MILLIS_PER_SECOND) + " s " +
								"- " +
								df.format(maxMillis / MILLIS_PER_SECOND) + " s " +
								"= " + bucketCount);
			}

			return
					"cnt: " + count + ", " +
							"avg t: " + (total_t / count) + " ms, " +
							"max t: " + max_t + " ms, " +
							"min t: " + min_t + " ms, " +
							"result codes: " + rcMap + ", " +
							"failures: " + failures + ", " +
							"cnt by time: " + millisStr + "";

		}

		String toAdvancedString() {
			double secondsElaspsed = (last_ts - first_ts) / MILLIS_PER_SECOND;
			long countPerSecond = Math.round(count / secondsElaspsed);

			return
					"avg conn: " + (total_conn / count) + " ms, " +
							"max conn: " + max_conn + " ms, " +
							"min conn: " + min_conn + " ms, " +
							"elapsed seconds: " + Math.round(secondsElaspsed) + " s, " +
							"cnt per second: " + countPerSecond;
		}

	}

}
