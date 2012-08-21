/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.max.vm.ext.vma.store.*;

/**
 * Main class of the object analysis query application.
 *
 */
public class QueryAnalysis {

    private static boolean verbose = false;
    private static int maxLines = Integer.MAX_VALUE;

    public static void main(String[] args) {
        ArrayList<String> dataDirs = new ArrayList<String>();
        ArrayList<String> queryClassDirs = new ArrayList<String>();
        String commandFile = null;
        String initialQuery = null;

        // Add the default query directory
        final String classpath = System.getProperty("java.class.path");
        final String[] entries = classpath.split(File.pathSeparator);
        for (String entry : entries) {
            if (entry.contains("com.oracle.max.vma.tools")) {
                queryClassDirs.add(entry);
            }
        }

        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-f")) {
                i++;
                while ((i < args.length) && !args[i].startsWith("-")) {
                    dataDirs.add(args[i]);
                    i++;
                }
                if (i < args.length) {
                    i--; // pushback next command
                }
            } else if (arg.equals("-i")) {
                i++;
                commandFile = args[i];
            } else if (arg.equals("-e")) {
                i++;
                initialQuery = args[i];
            } else if (arg.equals("-q")) {
                i++;
                while ((i < args.length) && !args[i].startsWith("-")) {
                    queryClassDirs.add(args[i]);
                    i++;
                }
                if (i < args.length) {
                    i--; // pushback next command
                }
            } else if (arg.equals("-v") || arg.equals("-verbose")) {
                verbose = true;
            } else if (arg.equals("-l")) {
                maxLines = Integer.parseInt(args[++i]);
            } else {
                System.err.println("Unknown command " + arg);
                usage();
            }
        }
        // Checkstyle: resume modified control variable check

        if (dataDirs.size() == 0) {
            dataDirs.add(VMAStoreFile.DEFAULT_STOREDIR);
        }

        for (String queryClassDir : queryClassDirs) {
            try {
                String queryClassDirCanon = new File(queryClassDir).getCanonicalPath();
                String queryClassUrl = "file://" + queryClassDirCanon + File.separator;
                QueryBase.addQueryClassDir(queryClassUrl);
                ArrayList<TraceRun> traceRuns = new ArrayList<TraceRun>(dataDirs.size());
                for (int t = 0; t < dataDirs.size(); t++) {
                    traceRuns.add(ProcessLog.processTrace(dataDirs.get(t), verbose, maxLines));
                }

                if (commandFile != null) {
                    interact(new FileReader(commandFile), traceRuns);
                }
                if (initialQuery != null) {
                    interact(new StringReader("e " + initialQuery), traceRuns);
                }
                interact(new InputStreamReader(System.in), traceRuns);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void usage() {
        System.err.println("usage: -f datafile1 datafile2 ... [-i commandfile] [-v]");
        System.exit(1);
    }

    private static void interact(Reader in, ArrayList<TraceRun> traceRuns) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        PrintStream ps = System.out;
        int traceFocus = 0;
        while (true) {
            System.out.print("%% ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }
            try {
                String[] lineParts = line.split(" ");
                switch (lineParts[0].charAt(0)) {
                    case 'e':
                        String queryName = lineParts[1];
                        String[] args = new String[lineParts.length - 2];
                        System.arraycopy(lineParts, 2, args, 0, args.length);
                        QueryBase query = QueryBase.ensureLoaded(queryName);
                        query.execute(traceRuns, traceFocus, ps, query.parseStandardArgs(args));
                        break;

                    case 'i': {
                        FileReader iin = null;
                        try {
                            iin = new FileReader(lineParts[1]);
                            interact(iin, traceRuns);
                        } catch (Exception ex) {
                            System.err.println(ex);
                        } finally {
                            if (iin != null) {
                                iin.close();
                            }
                        }
                        break;
                    }

                    case 'o':
                        if (lineParts.length == 1) {
                            if (ps != System.out) {
                                ps.close();
                                ps = System.out;
                            }
                        } else {
                            PrintStream nps = new PrintStream(new FileOutputStream(lineParts[1]));
                            ps = nps;
                        }
                        break;

                    case 'h':
                    case '?':
                        QueryBase.listQueries();
                        break;

                    case 'q':
                    case 'x':
                        System.exit(0);
                        break;

                    default:
                        System.err.println("unknown command " + lineParts[0]);
                }

            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }
}
