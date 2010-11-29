/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
