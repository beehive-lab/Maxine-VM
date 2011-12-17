/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.callanalysis;

import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) {
        try {
            OutputStream stream = new BufferedOutputStream(new Socket("localhost", 4444).getOutputStream(), 0x4000);

            String[] prefixes = {"com.oracle.max.graal.callanalysis", "com.sun.max.program"};
            StaticCallAnalyzer analyzer = new StaticCallAnalyzer(stream, prefixes, Main.class.getClassLoader());
            analyzer.start("Graal Call Analysis");
            analyzer.startGraph("Main.main()");
            analyzer.analyze("com.oracle.max.graal.callanalysis.Main", "main");
            analyzer.endGraph();
            analyzer.startGraph("SubtypeDiscovery.visitFile()");
            analyzer.analyze("com.oracle.max.graal.callanalysis.SubtypeDiscovery", "visitFile");
            analyzer.endGraph();
            analyzer.end();

            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
