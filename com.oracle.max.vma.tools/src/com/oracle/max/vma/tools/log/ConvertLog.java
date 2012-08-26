/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.max.vm.ext.vma.store.txt.CVMATextStore.*;

import java.io.*;
import java.util.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vm.ext.vma.store.*;
import com.oracle.max.vma.tools.qa.*;
import com.sun.max.program.*;

/**
 * Rewrites a log file making various transformations:
 * <ul>
 * <li>-abstime convert relative times to absolute
 * <li>-reltime convert absolute times to relative
 * <li>-batch convert to per-thread batches of records (i.e., non-time-ordered)
 * <li>-unbatch convert unordered (i.e. per thread batches) to time-ordered
 * </ul>
 *
 */
public class ConvertLog {

    private static boolean toAbsTime;
    private static boolean toRelTime;
    private static boolean verbose = false;
    private static PrintStream out;

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
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-abstime")) {
                toAbsTime = true;
            } else if (arg.equals("-reltime")) {
                toRelTime = true;
            } else if (arg.equals("-batch")) {
                command = new BatchCommand();
            } else if (arg.equals("-unbatch")) {
                command = new UnBatchCommand();
            } else if (arg.equals("-ajtrace")) {
                command = new AJcommand();
            } else if (arg.equals("-readable")) {
                command = new ReadableCommand();
            } else if (arg.equals("-merge")) {
                command = new MergeCommand();
            } else {
                usage();
            }
        }
        // Checkstyle: resume modified control variable check

        String logFileDir = null;
        if (logFileIn == null) {
            logFileIn = VMAStoreFile.DEFAULT_STOREFILE;
            logFileDir = VMAStoreFile.DEFAULT_STOREDIR;
        } else {
            File f = new File(logFileIn);
            if (f.isDirectory()) {
                logFileDir = logFileIn;
                logFileIn = new File(f, VMAStoreFile.GLOBAL_STORE).getPath();
            }
        }
        File f = new File(logFileIn);
        if (f.exists()) {
            processLogFiles(new File[] {new File(logFileIn)}, logFileOut, command);
        } else {
            // maybe per-thread
            if (logFileDir != null) {
                File[] files = new File(logFileDir).listFiles();
                if (command instanceof MergeCommand) {
                    command.execute(files, logFileOut);
                } else {
                    processLogFiles(files, logFileOut, command);
                }
            } else {
                usage();
            }
        }

    }

    private static void usage() {
        System.err.println("usage: -f logfileIn [-o logFileOut] [-batch | -unbatch | -ajtrace] [-abstime] [-reltime]");
        System.exit(1);
    }

    private static void processLogFiles(File[] inFiles, String outFile, Command command) throws IOException {
        try {
            out = outFile == null ? System.out : new PrintStream(new FileOutputStream(outFile));
            for (File inFile : inFiles) {
                BufferedReader r = null;
                try {
                    r = new BufferedReader(new FileReader(inFile));

                    while (true) {
                        final String line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        if (line.length() == 0) {
                            continue;
                        }

                        command.visitLine(line);
                    }
                } finally {
                    if (r != null) {
                        try {
                            r.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
            command.finish();
        } finally {
            if (outFile != null && out != null) {
                out.close();
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
        void execute(File[] files, String logFileOut) {

        }

        void visitLine(String line) {
        }

        void finish() {
        }

    }

    private static abstract class BasicCommand extends Command {
        boolean logUsesAbsTime; // constant once assigned
        long lineTime; // time field of last line visited
        long lineAbsTime; // absolute time of last line visited
        Key command;
        String[] lineParts;

        @Override
        void visitLine(String line) {
            setCommand(line);
            if (CVMATextStore.hasTime(command)) {
                lineTime = Long.parseLong(lineParts[1]);
                lineAbsTime = logUsesAbsTime ? lineTime : lineAbsTime + lineTime;
            } else if (command == Key.INITIALIZE_STORE || command == Key.THREAD_SWITCH || command == Key.FINALIZE_STORE) {
                checkTimeFormat();
            }
        }

        void setCommand(String line) {
            lineParts = line.split(" ");
            command = commandMap.get(lineParts[0]);
        }

        void checkTimeFormat() {
            lineTime = Long.parseLong(lineParts[1]);
            lineAbsTime = lineTime;
            if (command == Key.INITIALIZE_STORE) {
                logUsesAbsTime = lineParts[2].equals("true");
            }
        }
    }

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
    /**
     * Command to transform a batched (non-time-ordered) log into a time-ordered log.
     */

    private static class UnBatchCommand extends BasicCommand {
        private boolean seenIL;


        private ArrayList<TimedLine> lines = new ArrayList<TimedLine>();

        @Override
        void visitLine(String line) {
            super.visitLine(line);
            if (command == Key.THREAD_SWITCH) {
                // drop these records
                return;
            } else if (command == Key.INITIALIZE_STORE) {
                if (seenIL) {
                    return;
                } else {
                    seenIL = true;
                }
            }
            lines.add(new TimedLine(lineAbsTime, lineParts));
        }

        @Override
        void finish() {
            TimedLine[] linesArray = lines.toArray(new TimedLine[lines.size()]);
            Arrays.sort(linesArray);
            long lastTime = linesArray[0].time;
            for (int i = 0; i < linesArray.length; i++) {
                String line;
                String[] lineParts = linesArray[i].lineParts;
                if (CVMATextStore.hasTime(commandMap.get(lineParts[0]))) {
                    line = fixupTime(linesArray[i], lastTime);
                    lastTime = linesArray[i].time;
                } else {
                    line = concat(linesArray[i].lineParts);
                }
                out.println(line);
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
     * Command to transform into batch (per-thread) style.
     * Sanity checking for unbatch command.
     *
     */
    private static class BatchCommand extends BasicCommand {
        private static class BatchData {
            ArrayList<String> lines = new ArrayList<String>();
            long lastTime;

            BatchData(long startTime) {
                lastTime = startTime;
                lines.add(new StringBuilder().append(Key.THREAD_SWITCH).append(' ').append(startTime).toString());
            }
        }

        Map<String, BatchData> batchMap = new HashMap<String, BatchData>();
        String initialize;
        String finalize;
        BatchData currentBatch;
        BatchData initialBatch;

        @Override
        void visitLine(String line) {
            super.visitLine(line);
            if (CVMATextStore.hasTime(command)) {
                if (CVMATextStore.hasTimeAndThread(command)) {
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
            } else if (command == Key.INITIALIZE_STORE) {
                initialize = line;
                initialBatch = new BatchData(lineAbsTime);
                currentBatch = initialBatch;
            } else if (command == Key.THREAD_SWITCH) {
                // ignore existing resets
            } else if (command == Key.FINALIZE_STORE) {
                finalize = line;
            } else {
                // no time/thread component
                currentBatch.lines.add(line);
            }

        }

        @Override
        void finish() {
            out.println(initialize);
            // output batches
            for (String batchLine : initialBatch.lines) {
                out.println(batchLine);
            }
            for (BatchData batch : batchMap.values()) {
                for (String batchLine : batch.lines) {
                    out.println(batchLine);
                }
            }
            out.println(finalize);

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
     * Merges per-thread files into a single file with a merge sort.
     */
    public static class MergeCommand extends Command {

        private PushRecord pushRecord;

        public interface PushRecord {
            void pushRecord(String record);
        }

        /**
         * Default constructor for file output mode.
         */
        MergeCommand() {
        }

        /**
         * Constructor for push mode directly to registered callback.
         * @param push
         */
        public MergeCommand(PushRecord pushRecord) {
            this.pushRecord = pushRecord;
        }

        void miscOut(String record) {
            if (pushRecord != null) {
                pushRecord.pushRecord(record);
            } else {
                out.println(record);
            }

        }

        private class FileInfo implements Comparable<FileInfo> {
            final File file;
            BufferedReader reader;
            boolean logUsesAbsTime; // constant once assigned
            long lastAbsTime; // absolute time of last line visited
            Record record;
            int lineNumber;

            FileInfo(File file) throws IOException {
                this.file = file;
                this.reader = new BufferedReader(new FileReader(file));
            }

            @Override
            public String toString() {
                return file.getName() + ": " + lastAbsTime;
            }

            String readRecord() throws IOException {
                String line = reader.readLine();
                record = new Record(line);
                lineNumber++;
                return line;
            }

            long outputRecordAndNext(long previousTime) throws IOException {
                if (CVMATextStore.hasTime(record.command)) {
                    record.adjustRelTime(previousTime);
                }
                miscOut(record.concat());
                previousTime = record.time();
                readRecord();
                return previousTime;
            }

            /**
             * Partially decoded record, contains command as a {@link Key}, absolute time, and record components.
             */
            private class Record {
                final TimedLine timedLine;
                final Key command;

                Record(String line) {
                    String[] parts = line.split(" ");
                    command = commandMap.get(parts[0]);
                    if (CVMATextStore.hasTime(command)) {
                        long thisTime = Long.parseLong(parts[1]);
                        lastAbsTime = logUsesAbsTime ? thisTime : lastAbsTime + thisTime;
                    } else if (command == Key.INITIALIZE_STORE || command == Key.FINALIZE_STORE) {
                        long thisTime = Long.parseLong(parts[1]);
                        lastAbsTime = thisTime;
                        if (command == Key.INITIALIZE_STORE) {
                            logUsesAbsTime = parts[2].equals("true");
                        }
                    } else {
                        // a definition; give it the same time as the last record
                    }
                    timedLine = new TimedLine(lastAbsTime, parts);
                }

                @Override
                public String toString() {
                    return command + ", " + timedLine.toString();
                }

                long time() {
                    return timedLine.time;
                }

                /**
                 * If file uses relative time, adjust the time in the record to {@code baseTime}.
                 * @param baseTime
                 */
                private void adjustRelTime(long baseTime) {
                    if (!logUsesAbsTime) {
                        long rel = timedLine.time - baseTime;
                        if (rel < 0) {
                            throw new IllegalArgumentException("negative relative time!");
                        }
                        timedLine.lineParts[1] = Long.toString(rel);
                    }
                }

                private String concat() {
                    return ConvertLog.concat(timedLine.lineParts);
                }

            }

            @Override
            public int compareTo(FileInfo arg0) {
                return record.time() < arg0.record.time() ? -1 : (record.time() > arg0.record.time() ? 1 : 0);
            }
        }

        @Override
        public void execute(File[] files, String logFileOut) {
            ArrayList<File> fileList = new ArrayList<File>();
            for (int i = 0; i < files.length; i++) {
                if (files[i].length() != 0) {
                    fileList.add(files[i]);
                }
            }
            FileInfo[] fileInfos = new FileInfo[fileList.size()];
            for (int i = 0; i < fileList.size(); i++) {
                File file = fileList.get(i);
                try {
                    fileInfos[i] = new FileInfo(file);
                } catch (IOException ex) {
                    System.err.println(ex);
                    System.exit(1);
                }
            }

            // Ok, all files open, now read first record (INITIALIZE_STORE)

            try {
                // Read INITIALIZE_STORE and sort
                for (FileInfo fileInfo : fileInfos) {
                    fileInfo.readRecord();
                }
                Arrays.sort(fileInfos);

                if (pushRecord == null) {
                    out = logFileOut == null ? System.out : new PrintStream(new FileOutputStream(logFileOut));
                }

                // Earliest is the INITIALIZE_STORE for the merged file
                long previousTime = fileInfos[0].record.timedLine.time;
                miscOut("IL " + previousTime + " false false");  // new INITIALIZE_STORE

                // Read first real record and sort
                for (FileInfo fileInfo : fileInfos) {
                    fileInfo.readRecord();
                }
                Arrays.sort(fileInfos);

                LinkedList<FileInfo> fileInfoList = new LinkedList<FileInfo>();
                for (FileInfo fileInfo : fileInfos) {
                    fileInfoList.add(fileInfo);
                }

                // Starting with file containing earliest record, copy records to the output
                // until we reach one that is older than the earliest record in next youngest file.
                // Then sort file into correct place in the list and repeat.

            outer:
                while (fileInfoList.size() > 1) {
                    FileInfo youngest = fileInfoList.get(0);
                    FileInfo nextYoungest = fileInfoList.get(1);
                    while (youngest.record.time() <= nextYoungest.record.time()) {
                        if (youngest.record.command == Key.FINALIZE_STORE) {
                            // end of this file
                            if (verbose) {
                                System.out.printf("finished %s%n", youngest.file);
                            }
                            fileInfoList.remove();
                            continue outer;
                        }
                        previousTime = youngest.outputRecordAndNext(previousTime);
                    }
                    // sort file
                    if (verbose) {
                        System.out.printf("sort %d %d: ", youngest.record.time(), nextYoungest.record.time());
                    }
                    fileInfoList.remove();
                    boolean inserted = false;
                    for (int i = 1; i < fileInfoList.size(); i++) {
                        if (youngest.record.time() < fileInfoList.get(i).record.time()) {
                            fileInfoList.add(i, youngest);
                            if (verbose) {
                                System.out.printf("inserted at %d%n", i);
                            }
                            inserted = true;
                            break;
                        }
                    }
                    if (!inserted) {
                        if (verbose) {
                            System.out.printf("inserted at end%n");
                        }
                        fileInfoList.add(fileInfoList.size(), youngest);
                    }
                }
                // copy remaining records in last file
                FileInfo last = fileInfoList.get(0);
                long lastRecordAbsTime = last.record.time();
                while (last.record.command != Key.FINALIZE_STORE) {
                    previousTime = last.outputRecordAndNext(previousTime);
                    lastRecordAbsTime = last.record.time();
                }
                miscOut("FL " + lastRecordAbsTime);

            } catch (IOException ex) {
                System.err.println(ex);
                System.exit(1);
            } finally {
                if (logFileOut != null && out != null) {
                    out.close();
                }
                if (pushRecord != null) {
                    pushRecord.pushRecord(null);
                }
            }
        }

    }
    /**
     * Clones a log. Used to convert from rel/abs time and vice-versa.
     *
     */
    private static class CloneCommand extends BasicCommand {
        @Override
        void visitLine(String line) {
            super.visitLine(line);
            if ((logUsesAbsTime && toAbsTime) || (!logUsesAbsTime && toRelTime)) {
                // no change
                out.println(line);
            } else {
                if (CVMATextStore.hasTime(command)) {
                    if (logUsesAbsTime) {
                        // to relative
                        ProgramError.unexpected("abs to rel not implemented");
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
                } else if (command == Key.INITIALIZE_STORE) {
                    line = lineParts[0] + " " + lineAbsTime + " " + !logUsesAbsTime;
                }
                out.println(line);
            }
        }
    }

    /**
     * Converts to the format expect by the AJTrace analyser tool for viewing call graph hierarchies.
     * This is based on {@code METHOD_ENTRY} and {@code RETURN} advice. The commented out code
     * also interprets {@code INVOKE} before/after, but does not properly handle the case where both are present.
     */
    private static class AJcommand extends BasicCommand {

        /*
        private static EnumSet<Key> INVOKE_BEFORE_SET = EnumSet.of(
                        Key.ADVISE_BEFORE_INVOKE_INTERFACE, Key.ADVISE_BEFORE_INVOKE_VIRTUAL,
                        Key.ADVISE_BEFORE_INVOKE_SPECIAL, Key.ADVISE_BEFORE_INVOKE_STATIC);


        private static EnumSet<Key> INVOKE_AFTER_SET = EnumSet.of(
                        Key.ADVISE_AFTER_INVOKE_INTERFACE, Key.ADVISE_AFTER_INVOKE_VIRTUAL,
                        Key.ADVISE_AFTER_INVOKE_SPECIAL, Key.ADVISE_AFTER_INVOKE_STATIC);
        */

        private long startTime;
        private Map<String, Integer> callDepth = new HashMap<String, Integer>();
        private int fullMethodIndex;
        private Map<String, String> classShortForms = new HashMap<String, String>();
        private Map<String, String> methodShortForms = new HashMap<String, String>();
        private Map<String, String> classMethodDefs = new HashMap<String, String>();
        private String[] entryMid = new String[1024];
        private int entryMidIndex;

        @Override
        void visitLine(String line) {
            super.visitLine(line);
            StringBuilder sb = new StringBuilder();
            if (command == Key.INITIALIZE_STORE) {
                sb.append("0 S S ");
                sb.append(lineAbsTime);
                startTime = lineAbsTime;
            } else if (command == Key.CLASS_DEFINITION) {
                classShortForms.put(lineParts[3], ClassRecord.getCanonicalName(lineParts[1]));
                return;
            } else if (command == Key.THREAD_DEFINITION) {
                sb.append("0 D T");
                sb.append(lineParts[2]);
                sb.append(' ');
                sb.append(lineParts[1]);
                callDepth.put(lineParts[2], 1);
            } else if (command == Key.METHOD_DEFINITION) {
                methodShortForms.put(lineParts[3], lineParts[2]);
                return;
            } else if (/*INVOKE_BEFORE_SET.contains(command) || */command == Key.ADVISE_AFTER_METHOD_ENTRY) {
                String thread = lineParts[2];
                String mid = checkMethodDef();
                sb.append(callDepth.get(thread));
                sb.append(" E");
                sb.append(lineAbsTime - startTime);
                sb.append(" T");
                sb.append(thread);
                sb.append(" M");
                sb.append(mid);
                int tcd = callDepth.get(thread);
                callDepth.put(lineParts[2], tcd + 1);
                if (command == Key.ADVISE_AFTER_METHOD_ENTRY) {
                    entryMid[entryMidIndex++] = mid;
                }
            } else if (/*INVOKE_AFTER_SET.contains(command) || */command == Key.ADVISE_BEFORE_RETURN) {
                String thread = lineParts[2];
                int tcd = callDepth.get(thread) - 1;
                callDepth.put(lineParts[2], tcd);
                sb.append(callDepth.get(thread));
                sb.append(" R");
                sb.append(lineAbsTime - startTime);
                sb.append(" T");
                sb.append(thread);
                sb.append(" M");
                String mid = command == Key.ADVISE_BEFORE_RETURN ? entryMid[--entryMidIndex] : checkMethodDef();
                sb.append(mid);
            } else {
                return;
            }
            out.println(sb.toString());
        }

        private String checkMethodDef() {
            StringBuilder sb = new StringBuilder();
            String fullName = classShortForms.get(lineParts[4]) + "." + methodShortForms.get(lineParts[5]);
            String id = classMethodDefs.get(fullName);
            if (id == null) {
                id = new String(Integer.toString(fullMethodIndex++));
                sb.append("0 M M");
                sb.append(id);
                sb.append(' ');
                sb.append(fullName);
                out.println(sb.toString());
                classMethodDefs.put(fullName, id);
            }
            return id;
        }

    }

    /**
     * Converts to a readable format from the compressed form.
     */
    private static class ReadableCommand extends BasicCommand {
        private Map<String, String> lastId = new HashMap<String, String>();

        @Override
        void visitLine(String line) {
            super.visitLine(line);
            String arg1 = lineParts[1];
            String arg2 = lineParts.length > 2 ? lineParts[2] : null;
            String arg3 = lineParts.length > 3 ? lineParts[3] : null;
            String arg4 = lineParts.length > 4 ? lineParts[4] : null;
            String arg5 = lineParts.length > 5 ? lineParts[5] : null;
            String arg6 = lineParts.length > 6 ? lineParts[6] : null;
            String logTimeArg = arg1;
            String threadArg = arg2;
            String objIdArg = "???";

            if (CVMATextStore.hasTime(command)) {
                String atTime = "@" + logTimeArg;
                out.printf("%-10s ", atTime);
            } else {
                out.printf("%-11c", ' ');
            }

            if (CVMATextStore.hasId(command)) {
                if (arg3.charAt(0) == REPEAT_ID) {
                    objIdArg = lastId.get(threadArg);
                } else {
                    objIdArg = arg3;
                    lastId.put(threadArg, objIdArg);
                }
            }
            out.printf("%s ", command);

            if (CVMATextStore.hasTimeAndThread(command)) {
                printThreadId(threadArg);
            }

            if (CVMATextStore.hasId(command)) {
                printObjId(objIdArg);
            }


            switch (command) {
                case INITIALIZE_STORE:
                    out.printf("%s %s", logTimeArg, arg2);
                    break;

                case THREAD_SWITCH:
                case FINALIZE_STORE:
                    out.printf("%s", logTimeArg);
                    break;

                case CLASS_DEFINITION:
                    printClassId(arg3);
                    out.printf(" %s", arg1);
                    printClId(arg2);
                    break;

                case THREAD_DEFINITION:
                    printThreadId(arg2);
                    out.printf(" %s", arg1);
                    break;

                case METHOD_DEFINITION:
                    printMethodId(arg3);
                    printClassId(arg1);
                    out.printf(" %s", arg2);
                    break;

                case FIELD_DEFINITION:
                    printFieldId(arg3);
                    printClassId(arg1);
                    out.printf(" %s", arg2);
                    break;

                case ADVISE_AFTER_NEW:
                case ADVISE_AFTER_NEW_ARRAY:
                    printClassId(lineParts[ID_CLASSNAME_INDEX]);
                    if (command == Key.ADVISE_AFTER_NEW_ARRAY) {
                        out.printf(" %s", lineParts[ID_CLASSNAME_INDEX + 1]);
                    }
                    break;

                case UNSEEN:
                    printClassId(lineParts[ID_CLASSNAME_INDEX]);
                    break;

                case ADVISE_BEFORE_CONST_LOAD:
                    printValue(arg3, arg4);
                    break;

                case ADVISE_BEFORE_LOAD:
                case ADVISE_BEFORE_STORE:
                    out.printf(" %s", arg3);
                    if (command == Key.ADVISE_BEFORE_STORE) {
                        printValue(arg4, lineParts[5]);
                    }
                    break;

                case ADVISE_BEFORE_ARRAY_LOAD:
                case ADVISE_BEFORE_ARRAY_STORE:
                    out.printf(" %s", threadArg, objIdArg, lineParts[ARRAY_INDEX_INDEX]);
                    if (command == Key.ADVISE_BEFORE_ARRAY_STORE) {
                        printValue(lineParts[ARRAY_INDEX_INDEX + 1], lineParts[ARRAY_INDEX_INDEX + 2]);
                    }
                    break;

                case ADVISE_BEFORE_ARRAY_LENGTH:
                    out.printf(" %s", arg4);
                    break;

                case ADVISE_BEFORE_GET_STATIC:
                case ADVISE_BEFORE_PUT_STATIC:
                    printClassIdAndFieldId(lineParts[STATIC_CLASSNAME_INDEX], lineParts[STATIC_CLASSNAME_INDEX + 1]);
                    if (command == Key.ADVISE_BEFORE_PUT_STATIC) {
                        printValue(lineParts[STATIC_CLASSNAME_INDEX + 2], lineParts[STATIC_CLASSNAME_INDEX + 3]);
                    }
                    break;

                case ADVISE_BEFORE_GET_FIELD:
                case ADVISE_BEFORE_PUT_FIELD:
                    printClassIdAndFieldId(lineParts[ID_CLASSNAME_INDEX], lineParts[ID_CLASSNAME_INDEX + 1]);
                    if (command == Key.ADVISE_BEFORE_PUT_FIELD) {
                        printValue(lineParts[ID_CLASSNAME_INDEX + 2], lineParts[ID_CLASSNAME_INDEX + 3]);
                    }
                    break;

                case ADVISE_BEFORE_IF:
                    printIfOpcode(arg3);
                    if (arg4.equals("J")) {
                        out.printf(" %s %s", arg5, arg6);
                    } else {
                        printObjId(arg5);
                        printObjId(arg6);
                    }
                    break;

                case ADVISE_BEFORE_OPERATION:
                    printOperation(arg3);
                    printValue(arg4, arg5);
                    printValue(arg4, arg6);
                    break;

                case ADVISE_BEFORE_INSTANCE_OF:
                case ADVISE_BEFORE_CHECK_CAST:
                    printClassId(arg4);
                    break;

                case ADVISE_BEFORE_CONVERSION:
                    printOperation(arg3);
                    printValue(arg4, arg5);
                    break;

                case REMOVAL:
                    printObjId(arg1);
                    break;

                /*
                case ADVISE_AFTER_INVOKE_INTERFACE:
                case ADVISE_AFTER_INVOKE_STATIC:
                case ADVISE_AFTER_INVOKE_VIRTUAL:
                case ADVISE_AFTER_INVOKE_SPECIAL:
                */
                case ADVISE_BEFORE_INVOKE_INTERFACE:
                case ADVISE_BEFORE_INVOKE_STATIC:
                case ADVISE_BEFORE_INVOKE_VIRTUAL:
                case ADVISE_BEFORE_INVOKE_SPECIAL:
                    printClassIdAndMethodId(lineParts[ID_CLASSNAME_INDEX], lineParts[ID_CLASSNAME_INDEX + 1]);
                    break;


                case ADVISE_BEFORE_RETURN:
                    if (arg3 != null) {
                        printValue(arg3, arg4);
                    }
                    break;

                case ADVISE_BEFORE_STACK_ADJUST:
                    out.printf(" %s", VMABytecodes.values()[Integer.parseInt(arg3)]);
                    break;

                case ADVISE_AFTER_GC:
                case ADVISE_BEFORE_THROW:
                case ADVISE_BEFORE_MONITOR_ENTER:
                case ADVISE_BEFORE_MONITOR_EXIT:
                case ADVISE_BEFORE_GC:
                case ADVISE_BEFORE_THREAD_TERMINATING:
                case ADVISE_BEFORE_THREAD_STARTING:
                    // nothing else
                    break;

                case ADVISE_AFTER_MULTI_NEW_ARRAY:
            }
            out.println();
        }

        private static void printThreadId(String thread) {
            out.printf("t:%s", thread);
        }

        private static void printObjId(String objId) {
            if (objId.equals("0")) {
                out.printf(" null");
            } else {
                out.printf(" oid:%s", objId);
            }
        }

        private static void printClassId(String klass) {
            out.printf(" cid:%s", klass);
        }

        private static void printClassIdAndClId(String klass, String clId) {
            printClassId(klass);
            printClId(clId);
        }

        private static void printClId(String clId) {
            out.printf(" clid:%s", clId);
        }

        private static void printClassIdAndMethodId(String klass, String method) {
            printClassId(klass);
            printMethodId(method);
        }

        private static void printClassIdAndFieldId(String klass, String field) {
            printClassId(klass);
            printFieldId(field);
        }

        private static void printFieldId(String field) {
            out.printf(" fid:%s", field);
        }

        private static void printMethodId(String method) {
            out.printf(" mid:%s", method);
        }

        private static void printValue(String type, String value) {
            char typeCode = type.charAt(0);
            String rType = "???";
            switch (typeCode) {
                case 'J':
                    rType = "long";
                    break;
                case 'F':
                    rType = "float";
                    break;
                case 'D':
                    rType = "double";
                    break;
                case 'O':
                    rType = "oid";
                    break;
            }
            out.printf(" %s:%s", rType, value);
        }

        private static void printIfOpcode(String opcode) {
            out.printf(" %s", VMABytecodes.values()[Integer.parseInt(opcode)]);
        }

        private static void printOperation(String opcode) {
            out.printf(" %s", VMABytecodes.values()[Integer.parseInt(opcode)]);
        }

    }


}
