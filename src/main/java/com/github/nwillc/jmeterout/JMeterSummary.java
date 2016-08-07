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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andres.Galeano@Versatile.com
 */
public class JMeterSummary {

	private static final String REG_EX =
			"<httpSample\\s*" + // Start element
					"t=\"([^\"]*)\"\\s*" + // GROUP_T
					"lt=\"([^\"]*)\"\\s*" + // GROUP_LT
					"ts=\"([^\"]*)\"\\s*" + // GROUP_TS
					"s=\"([^\"]*)\"\\s*" + // GROUP_S
					"lb=\"([^\"]*)\"\\s*" + // GROUP_LB
					"rc=\"([^\"]*)\"\\s*" + // GROUP_RC
					"rm=\"([^\"]*)\"\\s*" + // GROUP_RM
					"tn=\"([^\"]*)\"\\s*" + // GROUP_TN
					"dt=\"([^\"]*)\"\\s*" + // GROUP_DT
					"/>"; // Finish element

	private static final int GROUP_ALL = 0;
	private static final int GROUP_T = 1;
	private static final int GROUP_LT = 2;
	private static final int GROUP_TS = 3;
	private static final int GROUP_S = 4;
	private static final int GROUP_LB = 5;
	private static final int GROUP_RC = 6;
	private static final int GROUP_RM = 7;
	private static final int GROUP_TN = 8;
	private static final int GROUP_DT = 9;

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

					String url = m.group(GROUP_LB);
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
	} // end [run()]

	/**
	 */
	private void add(Matcher inM, Totals inTotal) {
		inTotal.count++;
		long timeStamp = Long.parseLong(inM.group(GROUP_TS));
		inTotal.last_ts = Math.max(inTotal.last_ts, timeStamp);
		inTotal.first_ts = Math.min(inTotal.first_ts, timeStamp);

		int time = Integer.parseInt(inM.group(GROUP_T));
		inTotal.total_t += time;
		inTotal.max_t = Math.max(inTotal.max_t, time);
		inTotal.min_t = Math.min(inTotal.min_t, time);

		int conn = time - Integer.parseInt(inM.group(GROUP_LT));
		inTotal.total_conn += conn;
		inTotal.max_conn = Math.max(inTotal.max_conn, conn);
		inTotal.min_conn = Math.min(inTotal.min_conn, conn);

		String rc = inM.group(GROUP_RC);
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

		if (!inM.group(GROUP_S).equalsIgnoreCase("true")) {
			inTotal.failures++;
		}
	} // end [add(Matcher, Totals)]

	/**
	 * @author Andres.Galeano@Versatile.com
	 */
	private class Totals {
		private static final String DECIMAL_PATTERN = "#,##0.0##";
		private static final double MILLIS_PER_SECOND = 1000.0;

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

		} // end [Totals.toString()]

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

	} // end [class Totals]

} // end [class JMeterSummary]