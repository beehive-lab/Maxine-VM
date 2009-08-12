/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.vm;

import com.sun.max.io.Files;

import java.io.*;

/**
 * The <code>ExternalCommand</code> class represents an external command with input and output files.
 *
 * @author Ben L. Titzer
 */
public class ExternalCommand {

    public final File workingDir;
    public final File stdinFile;
    public final File stdoutFile;
    public final File stderrFile;
    public final String[] command;

    public ExternalCommand(File workingDir, File stdin, File stdout, File stderr, String[] command) {
        stdinFile = stdin;
        stdoutFile = stdout;
        stderrFile = stderr;
        this.workingDir = workingDir;
        this.command = command;
    }

    public Result exec(boolean append, int timeout) {
        long start = System.currentTimeMillis();
        try {
            final StringBuilder sb = new StringBuilder("exec ");
            for (String s : command) {
                sb.append(escapeShellCharacters(s)).append(' ');
            }
            if (stdinFile != null) {
                sb.append(" < ").append(stdinFile.getAbsolutePath());
            }
            if (stdoutFile != null) {
                sb.append(append ? " >>" : " > ").append(stdoutFile.getAbsolutePath());
                sb.append(append ? " 2>> " : " 2> ").append(stderrFile.getAbsolutePath());
            } else {
                sb.append(" > /dev/null");
                sb.append(" 2>&1");
            }

            final String[] cmdarray = new String[] {"sh", "-c", sb.toString()};

            start = System.currentTimeMillis();
            final Process process = Runtime.getRuntime().exec(cmdarray, null, workingDir);
            final ProcessTimeoutThread processThread = new ProcessTimeoutThread(process, command[0], timeout);
            final int exitValue = processThread.exitValue();
            return new Result(null, exitValue, exitValue == -333, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            return new Result(t, 0, false, System.currentTimeMillis() - start);
        }
    }

    private static String escapeShellCharacters(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        for (int cursor = 0; cursor < s.length(); ++cursor) {
            final char cursorChar = s.charAt(cursor);
            if (cursorChar == '$') {
                sb.append("\\$");
            } else if (cursorChar == ' ') {
                sb.append("\\ ");
            } else {
                sb.append(cursorChar);
            }
        }
        return sb.toString();
    }

    public class Result {
        public final Throwable thrown;
        public final int exitValue;
        public final boolean timedOut;
        public final long timeMs;

        Result(Throwable thrown, int exitValue, boolean timedOut, long timeMs) {
            this.exitValue = exitValue;
            this.timedOut = timedOut;
            this.timeMs = timeMs;
            this.thrown = thrown;
        }

        public boolean completed() {
            return thrown != null && !timedOut;
        }

        public String checkError(Result other, String[] stdoutIgnore, String[] stderrIgnore) {
            if (thrown != null) {
                return thrown.toString();
            }
            if (timedOut) {
                return "timed out after " + timeMs + " ms";
            }
            if (exitValue != other.exitValue) {
                return "exit value = " + exitValue + ", expected " + other.exitValue;
            }
            if (!Files.compareFiles(stdoutFile, other.command().stdoutFile, stdoutIgnore)) {
                return "Standard out " + stdoutFile + " and " + other.command().stdoutFile + " do not match";
            }
            if (!Files.compareFiles(stderrFile, other.command().stderrFile, stdoutIgnore)) {
                return "Standard error " + stderrFile + " and " + other.command().stderrFile + " do not match";
            }
            return null;
        }

        ExternalCommand command() {
            return ExternalCommand.this;
        }
    }

    /**
     * A dedicated thread to wait for the process and terminate it if it gets stuck.
     *
     * @author Ben L. Titzer
     */
    public static class ProcessTimeoutThread extends Thread {

        private final Process process;
        private final int timeoutMillis;
        protected Integer exitValue;
        private boolean timedOut;
        public static final int PROCESS_TIMEOUT = -333;

        public ProcessTimeoutThread(Process process, String name, int timeoutSeconds) {
            super(name);
            this.process = process;
            this.timeoutMillis = 1000 * timeoutSeconds;
        }

        @Override
        public void run() {
            try {
                // Sleep for the prescribed timeout duration
                Thread.sleep(timeoutMillis);

                // Not interrupted: terminate associated process
                timedOut = true;
                process.destroy();
            } catch (InterruptedException e) {
                // Process completed within timeout
            }
        }

        public int exitValue() throws IOException {
            start();
            try {
                exitValue = process.waitFor();
                // Process exited: interrupt timeout thread so that it stops
                interrupt();
            } catch (InterruptedException interruptedException) {
                // do nothing.
            }

            try {
                // Wait for timeout thread to stop
                join();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }

            if (timedOut) {
                exitValue = -333;
            }
            return exitValue;
        }
    }

}
