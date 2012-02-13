/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.log.hosted;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.log.VMLog.Record;

/**
 * An example of a logger class with auto generation.
 */
@HOSTED_ONLY
public class ExampleLoggerOwner {
    @HOSTED_ONLY
    @VMLoggerInterface
    private interface ExampleLoggerInterface {
        void foo(
            @VMLogParam(name = "classActor") ClassActor classActor,
            @VMLogParam(name = "base") Pointer base);

        void bar(
            @VMLogParam(name = "count") SomeClass someClass, int count);
    }

    private static class SomeClass {

    }

    public static final ExampleLogger exampleLogger = new ExampleLogger();

    private static final class ExampleLogger extends ExampleLoggerAuto {

        ExampleLogger() {
            super("Example", "an example logger.");
        }

        @Override
        protected void traceFoo(ClassActor classActor, Pointer base) {
            Log.print("Class ");
            Log.print(classActor.name.string);
            Log.print(", base:");
            Log.println(base);
        }

        @Override
        protected void traceBar(SomeClass someClass, int count) {
            // SomeClass specific tracing
        }
    }

// START GENERATED CODE
    private static abstract class ExampleLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            Foo, Bar;

            public static final Operation[] VALUES = values();
        }

        protected ExampleLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logFoo(ClassActor classActor, Pointer base) {
            log(Operation.Foo.ordinal(), classActorArg(classActor), base);
        }
        protected abstract void traceFoo(ClassActor classActor, Pointer base);

        @INLINE
        public final void logBar(SomeClass count, int arg2) {
            log(Operation.Bar.ordinal(), objectArg(count), intArg(arg2));
        }
        protected abstract void traceBar(SomeClass count, int arg2);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //Foo
                    traceFoo(toClassActor(r, 1), toPointer(r, 2));
                    break;
                }
                case 1: { //Bar
                    traceBar(toSomeClass(r, 1), toInt(r, 2));
                    break;
                }
            }
        }
        static SomeClass toSomeClass(Record r, int argNum) {
            return asSomeClass(toObject(r, argNum));
        }
        @INTRINSIC(UNSAFE_CAST)
        private static native SomeClass asSomeClass(Object arg);
    }

// END GENERATED CODE

}
