/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is a test case that (attempts to) test catching an exception in an invalidated method.
 */
public class CatchInInvalidatedMethod {
    public static void main(String[] args) {
        Value v = new Value();

        // Make 'compute' hot so that it is recompiled with the optimizing compiler.
        // The optimizing compiler will (hopefully) inline Value.get() as
        // of Negation has yet been loaded.
        for (int i = 0; i < 20000; i++) {
            v.i = compute(v, true);
        }
        System.out.println("v: " + v);

        // This time call compute such that it will execute
        v.i = compute(v, false);

        System.out.println("v: " + v);
    }

    static class Value {
        int i;

        int get() {
            return i;
        }

        @Override
        public String toString() {
            return String.valueOf(i);
        }
    }

    /**
     * Invalidates speculative inlining of {@link Value#get()} in {@link CatchInInvalidatedMethod#compute(Value, boolean)}.
     */
    static class Negation extends Value {
        @Override
        int get() {
            return -i;
        }
    }

    public static int compute(Value v, boolean warmup) {
        if (warmup) {
            return v.get() * 100;
        }
        try {
            return triggerDeoptAndRaiseException();
        } catch (MyException e) {
            System.out.println("caught: " + e);
            return v.get();
        }
    }

    static class MyException extends Exception {
    }

    /**
     * Triggers loading of {@link Negation} which in turn should deoptimize the active execution of
     * {@link #compute}. Instead of returning normally, an exception is thrown.
     */
    public static int triggerDeoptAndRaiseException() throws MyException {
        Negation nv = new Negation();
        System.out.println("nv: " + nv);

        throw new MyException();
    }
}
