/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vma.tools.qa;

import java.io.*;
import java.util.ArrayList;

import com.oracle.max.vm.ext.vma.log.*;

/**
 * Main class of the object analysis query application.
 *
 * @author Mick Jordan
 *
 */
public class QueryAnalysis {

    private static boolean verbose = false;
    private static boolean prettyTrace = false;
    private static int maxLines = Integer.MAX_VALUE;

    public static void main(String[] args) {
        ArrayList<String> dataFiles = new ArrayList<String>();
        String commandFile = null;
        String queryClassDir = System.getProperty("query.dir");

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-f")) {
                i++;
                while ((i < args.length) && !args[i].startsWith("-")) {
                    dataFiles.add(args[i]);
                    i++;
                }
                if (i < args.length) {
                    i--; // pushback next command
                }
            } else if (arg.equals("-i")) {
                i++;
                commandFile = args[i];
            } else if (arg.equals("-q")) {
                i++;
                queryClassDir = args[i];
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-p")) {
                prettyTrace = true;
            } else if (arg.equals("-l")) {
                maxLines = Integer.parseInt(args[++i]);
            } else {
                System.err.println("Unknown command " + arg);
                usage();
            }
        }
        // Checkstyle: resume modified control variable check

        if (dataFiles.size() == 0) {
            dataFiles.add(VMAdviceHandlerLogFile.DEFAULT_LOGFILE);
        }

        if (queryClassDir == null) {
            final String classpath = System.getProperty("java.class.path");
            final String[] entries = classpath.split(File.pathSeparator);
            for (String entry : entries) {
                if (entry.contains("com.oracle.max.vma.tools")) {
                    queryClassDir = entry;
                    break;
                }
            }
        }

        try {
            queryClassDir = new File(queryClassDir).getCanonicalPath();
            String queryClassUrl = "file://" + queryClassDir + File.separator;
            QueryBase.setClassDir(queryClassUrl);
            ArrayList<TraceRun> traceRuns = new ArrayList<TraceRun>(
                    dataFiles.size());
            for (int t = 0; t < dataFiles.size(); t++) {
                traceRuns.add(ProcessLog.processTrace(
                        dataFiles.get(t), verbose, prettyTrace, maxLines));
            }

            if (commandFile != null) {
                interact(new FileInputStream(commandFile), traceRuns);
            }
            interact(System.in, traceRuns);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.err.println("usage: -f datafile1 datafile2 ... [-i commandfile] [-v]");
        System.exit(1);
    }

    private static void interact(InputStream in, ArrayList<TraceRun> traceRuns) {
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(in)));
        Util.resetStreamTokenizer(st);
        PrintStream ps = System.out;
        int traceFocus = 0;
        System.out.print("%% ");
        while (true) {
            try {
                if (st.nextToken() == StreamTokenizer.TT_EOF) {
                    break;
                }
                switch (st.ttype) {
                    case StreamTokenizer.TT_EOL:
                        System.out.print("%% ");
                        break;

                    case StreamTokenizer.TT_WORD:
                        switch (st.sval.charAt(0)) {
                            case 'v':
                                verbose = !verbose;
                                break;

                            case 'p':
                                prettyTrace = !prettyTrace;
                                break;

                            case 'e':
                                st.nextToken();
                                String queryName = st.sval;
                                ArrayList<String> aargs = new ArrayList<String>();
                                int i = 0;
                                while (st.nextToken() != StreamTokenizer.TT_EOL) {
                                    aargs.add(st.sval);
                                    i++;
                                }
                                st.pushBack();
                                String[] args = aargs.toArray(new String[i]);
                                Query query = QueryBase.ensureLoaded(queryName);
                                query.execute(traceRuns, traceFocus, ps, args);
                                break;

                            case 'o':
                                st.nextToken();
                                if (st.ttype == StreamTokenizer.TT_EOL) {
                                    if (ps != System.out) {
                                        ps.close();
                                        ps = System.out;
                                    }
                                    st.pushBack();
                                } else {
                                    PrintStream nps = new PrintStream(new FileOutputStream(st.sval));
                                    ps = nps;
                                }
                                break;

                            case 't':
                                st.nextToken();
                                if (st.ttype == StreamTokenizer.TT_WORD) {
                                    int tn = Integer.parseInt(st.sval);
                                    if ((tn >= 0) && (tn < traceRuns.size())) {
                                        traceFocus = tn;
                                    } else {
                                        System.err.println("tracefile number out of range");
                                    }
                                } else {
                                    System.err.println("tracefile number expected");
                                }
                                break;

                            case 'f':
                                st.nextToken();
                                if (st.ttype == StreamTokenizer.TT_EOL) {
                                    System.err.println("tracefile expected");
                                } else {
                                    traceRuns.add(ProcessLog.processTrace(st.sval, verbose, prettyTrace, maxLines));
                                    System.out.println("trace stored at index " + (traceRuns.size() - 1));
                                }
                                break;

                            case 'q':
                            case 'x':
                                return;
                            default:
                                System.err.println("unknown command " + st.sval);
                        }
                        break;

                    case StreamTokenizer.TT_NUMBER:
                        System.err.println("command expected");
                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }
}
