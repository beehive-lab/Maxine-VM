/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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

package test.bench.net;

import java.io.*;
import java.net.*;

import test.bench.util.*;


/**
 *
 * @author Puneeet Lakhina
 */
public class HTTPGetBench extends RunBench {

    protected HTTPGetBench(MicroBenchmark bench) {
        super(bench);
    }

    public static boolean test(int port, int resreps) {
        try {
            return new HTTPGetBench(new HTTPGetBenchMark(port, resreps)).runBench();
        } catch (IOException ex) {
            System.err.println(ex);
            return false;
        }
    }

    static class HTTPGetBenchMark extends MicroBenchmark {

        private ServerSocket socket;
        private static boolean logWire = System.getProperty("test.bench.net.http.logwire") != null;
        private int responseReps;
        private int writerBufSize = 8192;

        public HTTPGetBenchMark(int port, int responsereps) throws IOException {
            this.responseReps = responsereps;
            this.socket = new ServerSocket(port);
            final String bufSizeProperty = System.getProperty("test.bench.net.http.bufsize");
            if (bufSizeProperty != null) {
                writerBufSize = Integer.parseInt(bufSizeProperty);
            }
        }

        @Override
        public long run() throws Exception {
            Socket clientSocket = socket.accept();
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = br.readLine();
            log(requestLine);
            String s = null;
            while ((s = br.readLine()) != null) {
                log(s);
                if ("".equalsIgnoreCase(s)) {
                    break;
                }
            }
            log("received input. Now writing output");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()), writerBufSize);
            String response = buildResponse(requestLine);
            log(response);
            long length = response.getBytes().length;
            log(response.length() + " Bytes: " + length);
            // clientSocket.getOutputStream().write(response.getBytes());
            bw.write(response);
            log("Wrote response");
            bw.close();
            br.close();
            log("Closed output stream");
            clientSocket.close();
            log("Ended run");
            return length;
        }

        private String buildResponse(String requestLine) {
            StringBuilder builder = new StringBuilder("HTTP/1.1 200 OK\r\n");
            builder.append("Content-Type: text/html\r\n");
            builder.append("\r\n");

            builder.append("<html><head><title>Test Page</title></head>");
            for (int i = 1; i <= responseReps; i++) {
                builder.append(String.format("<h%d>Header size %d</h%d>", i, i, i));
            }
//            builder.append("<b>" + new Date().toString() + "</b>");
            builder.append("</html>");
            return builder.toString();
        }

        private static void log(String s) {
            if (logWire) {
                System.out.println(s);
            }
        }

    }

    public static void main(String[] args) throws Exception {
        RunBench.runTest(HTTPGetBench.class, args);
    }
}
