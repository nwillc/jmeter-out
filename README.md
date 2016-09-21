# jmeter-out

Java designed to post process JMeter XML log output. This code is based on an example by Andres.Galeano@Versatile.com, but only the spirit remains.

## Running
Simply build the project and then run against a JMeter XML log.

     $ gradle build
     $ java -jar build/libs/jmeter-out-all.jar log.xml > out.csv
     
The output will be the aggregated data of all the individual entries, with the addition of the 95% statistic:

     $ java -jar build/libs/jmeter-out-all.jar sample/basic.xml
     Request, Threads, Tot Req, Min, Max, Avg, Std, 95th, Errors
     SYMBOL, 5, 50, 69, 16961, 1352, 3443.001, 13390, 0
     TAX SNAPSHOT INCOME, 5, 50, 166, 950, 324, 159.577, 735, 0
     ping, 1, 2, 189, 206, 197, 12.021, 206, 0
     TAX SNAPSHOT ALL, 5, 50, 166, 3868, 607, 647.701, 2007, 0
     ARRANGEMENTS, 5, 50, 77, 498, 98, 59.31, 141, 0
     PORTFOLIO, 5, 50, 75, 124, 86, 11.269, 122, 0
     ACTIVITY, 5, 50, 71, 349, 91, 39.89, 135, 0


This format is very spreadsheet friendly.