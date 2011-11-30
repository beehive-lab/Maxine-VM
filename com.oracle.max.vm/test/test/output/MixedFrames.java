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
package test.output;

import sun.reflect.*;

/**
 * Performs a number of recursive calls that go in and out of native code
 * until a fixed recursion level is reached. Then the stack frame trace
 * is compared with reference VM.
 */
public class MixedFrames {

    static {
        System.loadLibrary("javatest");
    }

    public static void main(String[] args) {
        MixedFrames mixedFrames = new MixedFrames();
        mixedFrames.testNative(50);
    }

    private int i;
    private Object o;

    native void nativeUpdateFields(int recursion, int i, Object o);

    static class OtherClass {
        static void updateFields(MixedFrames mf, int recursion, int i) {
            mf.nativeUpdateFields(recursion, i, mf);
        }
    }

    private void testNative(int recursion) {
        final int intValue = 42;
        if (recursion > 0) {
            OtherClass.updateFields(this, recursion - 1, intValue);
            return;
        }
        System.gc();
        if (i != intValue) {
            System.out.println(i + " != " + intValue);
        }
        if (o != this) {
            System.out.println(o + " != " + this);
        }

        Throwable throwable = new Throwable();
        System.out.println("Trace of mixed frame stack:");
        for (StackTraceElement e : throwable.getStackTrace()) {
            String frame = e.toString();
            // Filter frames until stack trace construction in Maxine omits all the necessary system frames.
            if (frame.startsWith(MixedFrames.class.getName())) {
                System.out.println("\tat " + e);
            }
        }

        int count = 0;
        int prefix = 0;
        while (true) {
            Class callerClass = Reflection.getCallerClass(count);
            if (callerClass == null) {
                break;
            }
            // Filter frames until stack trace construction in Maxine omits all the necessary system frames.
            if (callerClass.getName().startsWith(MixedFrames.class.getName())) {
                System.out.println(prefix + ": " + callerClass);
                prefix++;
            }
            count++;
        }
    }
}
