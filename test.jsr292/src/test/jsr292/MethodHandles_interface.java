/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package test.jsr292;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public class MethodHandles_interface {

    private static final String STRING_FOR_ENCODING = "ABCD";

    public static void main(String[] args) throws Throwable {
        final byte[] out = new byte[STRING_FOR_ENCODING.length()];
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh1 = lookup.findVirtual(sun.nio.cs.ArrayEncoder.class, "encode", MethodType.methodType(int.class, char[].class, int.class, int.class, byte[].class));
        MethodHandle mh2 = lookup.unreflect(sun.nio.cs.ArrayEncoder.class.getMethod("encode", char[].class, int.class, int.class, byte[].class));
        final CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        try {
            final int result1 = (int) mh1.invoke(encoder, STRING_FOR_ENCODING.toCharArray(), 0, STRING_FOR_ENCODING.length(), out);
            System.out.println(result1);
            final int result2 = (int) mh2.invoke(encoder, STRING_FOR_ENCODING.toCharArray(), 0, STRING_FOR_ENCODING.length(), out);
            System.out.println(result2);
            assert result1 == result2 : result1 + " != " + result2;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
