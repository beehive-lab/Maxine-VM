/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm;

import static test.com.sun.max.vm.MaxineTester.Logs.*;

import java.io.*;
import java.util.*;

import test.com.sun.max.vm.MaxineTester.Logs;

import com.sun.max.*;
import com.sun.max.io.*;

/**
 * The {@code ExternalCommand} class represents an external command with input and output files.
 */
public class ExternalCommand {

    public final File workingDir;
    public final File stdinFile;
    public final Logs logs;
    public final String[] command;
    public final String[] env;

    public ExternalCommand(File workingDir, File stdin, Logs logs, String[] command, String[] env) {
        this.stdinFile = stdin;
        this.logs = logs;
        this.workingDir = workingDir;
        this.command = command;
        this.env = env;
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
            if (logs.base != null) {
                sb.append(append ? " >>" : " > ").append(logs.get(STDOUT).getAbsolutePath());
                sb.append(append ? " 2>> " : " 2> ").append(logs.get(STDERR).getAbsolutePath());
            } else {
                sb.append(" > /dev/null");
                sb.append(" 2>&1");
            }

            final String[] cmdarray = new String[] {"bash", "-c", sb.toString()};

            if (logs.base != null) {
                final PrintStream ps = new PrintStream(new FileOutputStream(logs.get(COMMAND)));
                ps.println(Utils.toString(cmdarray, " "));
                for (int i = 0; i < cmdarray.length; ++i) {
                    ps.println("Command array[" + i + "] = \"" + cmdarray[i] + "\"");
                }
                ps.println("Working directory: " + (workingDir == null ? "CWD" : workingDir.getAbsolutePath()));
                ps.println("Enviroment:");
                if (env != null) {
                    for (String def : env) {
                        ps.println("    " + def);
                    }
                } else {
                    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                        ps.println("    " + entry.getKey() + "=" + entry.getValue());
                    }
                }
                ps.close();
            }

            start = System.currentTimeMillis();
            final Process process = Runtime.getRuntime().exec(cmdarray, env, workingDir);
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
            return thrown == null && !timedOut;
        }

        public String checkError(Result other, OutputComparison comparison) {
            if (thrown != null) {
                return thrown.toString();
            }
            if (timedOut) {
                return "timed out after " + timeMs + " ms";
            }
            if (exitValue != other.exitValue) {
                return "exit value = " + exitValue + ", expected " + other.exitValue;
            }
            if (comparison.stdout && !Files.compareFiles(logs.get(STDOUT), other.command().logs.get(STDOUT), comparison.stdoutIgnore)) {
                return "Standard out " + logs.get(STDOUT) + " and " + other.command().logs.get(STDOUT) + " do not match";
            }
            if (comparison.stderr && !Files.compareFiles(logs.get(STDERR), other.command().logs.get(STDERR), comparison.stderrIgnore)) {
                return "Standard error " + logs.get(STDERR) + " and " + other.command().logs.get(STDERR) + " do not match";
            }
            return null;
        }

        ExternalCommand command() {
            return ExternalCommand.this;
        }
    }

    public static class OutputComparison {
        public boolean stdout = true;
        public boolean stderr = false;
        public String[] stdoutIgnore;
        public String[] stderrIgnore;
    }

    /**
     * A dedicated thread to wait for the process and terminate it if it gets stuck.
     *
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
