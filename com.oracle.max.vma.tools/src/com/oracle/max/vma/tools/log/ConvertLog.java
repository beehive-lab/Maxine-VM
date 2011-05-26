/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.max.vma.tools.log;

import static com.oracle.max.vm.ext.vma.log.txt.TextVMAdviceHandlerLog.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.log.txt.*;

/**
 * Rewrites a log file making various transformations:
 * <ul>
 * <li>-abstime convert relative times to absolute
 * <li>-reltime convert absolute times to relative
 * <li>-batch convert to per-thread batches of records (i.e., non-time-ordered)
 * <li>-unbatch convert unordered (i.e. per thread batches) to time-ordered
 * </ul>
 *
 *
 * @author Mick Jordan
 *
 */
public class ConvertLog {

    private static boolean toAbsTime;
    private static boolean toRelTime;

    public static void main(String[] args) throws Exception {
        String logFileIn = null;
        String logFileOut = null;
        Command command = new CloneCommand();
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-f")) {
                logFileIn = args[++i];
            } else if (arg.equals("-o")) {
                logFileOut = args[++i];
            } else if (arg.equals("-abstime")) {
                toAbsTime = true;
            } else if (arg.equals("-reltime")) {
                toRelTime = true;
            } else if (arg.equals("-batch")) {
                command = new BatchCommand();
            } else if (arg.equals("-unbatch")) {
                command = new UnBatchCommand();
            }
        }
        // Checkstyle: resume modified control variable check

        if (logFileIn == null || command == null) {
            usage();
        }
        processLogFile(logFileIn, logFileOut, command);
    }

    private static void usage() {
        System.err.println("usage: -f logfileIn [-o logFileOut] [-batch | -unbatch] [-abstime] [-reltime]");
        System.exit(1);
    }

    private static void processLogFile(String inFile, String outFile, Command command) throws IOException {
        BufferedReader r = null;
        PrintStream ps = null;
        int lineCount = 1;
        try {
            r = new BufferedReader(new FileReader(inFile));
            ps = outFile == null ? System.out : new PrintStream(
                    new FileOutputStream(outFile));

            while (true) {
                final String line = r.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }

                command.visitLine(line, ps);
                lineCount++;
            }
            command.finish(ps);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                }
            }
            if (outFile != null && ps != null) {
                ps.close();
            }
        }
    }

    private static String concat(String[] lineParts) {
        final StringBuilder sb = new StringBuilder(lineParts[0]);
        for (int i = 1; i < lineParts.length; i++) {
            sb.append(' ');
            sb.append(lineParts[i]);
        }
        return sb.toString();
    }

    private static abstract class Command {
        boolean logUsesAbsTime; // constant once assigned
        long lineTime; // time field of last line visited
        long lineAbsTime; // absolute time of last line visited
        char command;
        String[] lineParts;

        void visitLine(String line, PrintStream ps) {
            setCommand(line);
            if (TextVMAdviceHandlerLog.hasTime(command)) {
                lineTime = Long.parseLong(lineParts[1]);
                lineAbsTime = logUsesAbsTime ? lineTime : lineAbsTime + lineTime;
            } else if (command == INITIALIZE_ID || command == RESET_TIME_ID || command == FINALIZE_ID) {
                checkTimeFormat();
            }
        }

        void finish(PrintStream ps) {

        }

        void setCommand(String line) {
            command = line.charAt(0);
            lineParts = line.split(" ");
        }

        void checkTimeFormat() {
            lineTime = Long.parseLong(lineParts[1]);
            lineAbsTime = lineTime;
            if (command == INITIALIZE_ID) {
                logUsesAbsTime = lineParts[2].equals("true");
            }
        }
    }

    /**
     * Command to transform a batched (non-time-ordered) log into a time-ordered log.
     */

    private static class UnBatchCommand extends Command {
        private static class TimedLine implements Comparable<TimedLine> {
            long time;
            String[] lineParts;

            TimedLine(long time, String[] lineParts) {
                this.time = time;
                this.lineParts = lineParts;
            }

            public int compareTo(TimedLine t) {
                if (time < t.time) {
                    return -1;
                } else if (time > t.time) {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public String toString() {
                return "time: " + time + " [" + concat(lineParts) + "]";
            }

        }

        private ArrayList<TimedLine> lines = new ArrayList<TimedLine>();

        @Override
        void visitLine(String line, PrintStream ps) {
            super.visitLine(line, ps);
            if (command == RESET_TIME_ID) {
                // drop these records
                return;
            }
            lines.add(new TimedLine(lineAbsTime, lineParts));
        }

        @Override
        void finish(PrintStream ps) {
            TimedLine[] linesArray = lines.toArray(new TimedLine[lines.size()]);
            Arrays.sort(linesArray);
            long lastTime = linesArray[0].time;
            for (int i = 0; i < linesArray.length; i++) {
                String line;
                String[] lineParts = linesArray[i].lineParts;
                if (TextVMAdviceHandlerLog.hasTime(lineParts[0].charAt(0))) {
                    line = fixupTime(linesArray[i], lastTime);
                    lastTime = linesArray[i].time;
                } else {
                    line = concat(linesArray[i].lineParts);
                }
                ps.println(line);
            }

        }

        private String fixupTime(TimedLine timedLine, long absTime) {
            final StringBuilder sb = new StringBuilder(timedLine.lineParts[0]);
            sb.append(' ');
            if (logUsesAbsTime) {
                sb.append(absTime);
            } else {
                sb.append(timedLine.time - absTime);
            }
            for (int i = 2; i < timedLine.lineParts.length; i++) {
                sb.append(' ');
                sb.append(timedLine.lineParts[i]);
            }
            return sb.toString();
        }


    }
    /**
     *
     * Command to transform into batch (pre-thread) style.
     * Sanity checking for unbatch command.
     *
     */
    private static class BatchCommand extends Command {
        private static class BatchData {
            ArrayList<String> lines = new ArrayList<String>();
            long lastTime;

            BatchData(long startTime) {
                lastTime = startTime;
                lines.add(new StringBuilder().append(RESET_TIME_ID).append(' ').append(startTime).toString());
            }
        }

        Map<String, BatchData> batchMap = new HashMap<String, BatchData>();
        String initialize;
        String finalize;
        BatchData currentBatch;
        BatchData initialBatch;

        @Override
        void visitLine(String line, PrintStream ps) {
            super.visitLine(line, ps);
            if (TextVMAdviceHandlerLog.hasTime(command)) {
                if (TextVMAdviceHandlerLog.hasTimeAndThread(command)) {
                    // thread is in lineParts[2]
                    BatchData batch = batchMap.get(lineParts[2]);
                    if (batch == null) {
                        // new thread
                        batch = new BatchData(lineAbsTime);
                        batchMap.put(lineParts[2], batch);
                    }
                    currentBatch = batch;
                }
                currentBatch.lines.add(fixupTime(currentBatch, lineParts, lineAbsTime));
            } else if (command == INITIALIZE_ID) {
                initialize = line;
                initialBatch = new BatchData(lineAbsTime);
                currentBatch = initialBatch;
            } else if (command == RESET_TIME_ID) {
                // ignore existing resets
            } else if (command == FINALIZE_ID) {
                finalize = line;
            } else {
                // no time/thread component
                currentBatch.lines.add(line);
            }

        }

        @Override
        void finish(PrintStream ps) {
            ps.println(initialize);
            // output batches
            for (String batchLine : initialBatch.lines) {
                ps.println(batchLine);
            }
            for (BatchData batch : batchMap.values()) {
                for (String batchLine : batch.lines) {
                    ps.println(batchLine);
                }
            }
            ps.println(finalize);

        }

        private String fixupTime(BatchData batch, String[] lineParts,
                long absTime) {
            final StringBuilder sb = new StringBuilder(lineParts[0]);
            sb.append(' ');
            if (logUsesAbsTime) {
                sb.append(absTime);
            } else {
                sb.append(absTime - batch.lastTime);
                batch.lastTime = absTime;
            }
            for (int i = 2; i < lineParts.length; i++) {
                sb.append(' ');
                sb.append(lineParts[i]);
            }
            return sb.toString();
        }
    }

    /**
     * Clones a log. Used to convert from rel/abs time and vice-versa.
     *
     */
    private static class CloneCommand extends Command {
        @Override
        void visitLine(String line, PrintStream ps) {
            super.visitLine(line, ps);
            if ((logUsesAbsTime && toAbsTime) || (!logUsesAbsTime && toRelTime)) {
                // no change
                ps.println(line);
            } else {
                if (TextVMAdviceHandlerLog.hasTime(command)) {
                    if (logUsesAbsTime) {
                        // to relative

                    } else {
                        // to absolute
                        final StringBuilder sb = new StringBuilder(lineParts[0]);
                        sb.append(' ');
                        sb.append(lineAbsTime);
                        for (int i = 2; i < lineParts.length; i++) {
                            sb.append(' ');
                            sb.append(lineParts[i]);
                        }
                        line = sb.toString();
                    }
                } else if (command == INITIALIZE_ID) {
                    line = lineParts[0] + " " + lineAbsTime + " " + !logUsesAbsTime;
                }
                ps.println(line);
            }
        }
    }
}
