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
package com.sun.max.ins.method;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;


public class NativeCodeMaps {

    public static class Info {
        public final String name;
        public final Address base;
        public final int length;

        Info(String name, long base, int length) {
            this.name = name;
            this.base = Address.fromLong(base);
            this.length = length;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    final static Map<String, Info> map = new HashMap<String, Info>();

    public static Info find(Address address) {
        Trace.line(1, "NativeCodeMaps.find" + address.to0xHexString());
        for (Info info : map.values()) {
            if (address.greaterEqual(info.base) && address.lessThan(info.base.plus(info.length))) {
                Trace.line(1, "NativeCodeMaps.find: found" + info.name + " base " + info.base.to0xHexString());
                return info;
            }
        }
        return null;
    }

    public static void read(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                String[] parts = line.split(",");
                map.put(parts[0], new Info(parts[0], parseHexLong(parts[1]), Integer.parseInt(parts[2])));
            }
        } catch (IOException ex) {
            // TODO error dialog
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * {@link Long.parseLong} does not like negative hex values like ffffffff.
     * @param s
     * @return
     */
    private static long parseHexLong(String s) {
        long result = 0;
        for (int i = 0; i < s.length(); i++) {
            int val = s.charAt(i);
            if (val >= '0' && val <= '9') {
                val = val - '0';
            } else {
                val = val - 'a' + 10;
            }
            result = (result << 4) | val;
        }
        return result;
    }
}
