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
package test.output;

import sun.reflect.*;

/**
 * Performs a number of recursive calls that go in and out of native code
 * until a fixed recursion level is reached. Then the stack frame trace
 * is compared with reference VM.
 *
 * @author Doug Simon
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
        while (true) {
            Class callerClass = Reflection.getCallerClass(count);
            if (callerClass == null) {
                break;
            }
            System.out.println(count + ": " + callerClass);
            count++;
        }
    }
}
