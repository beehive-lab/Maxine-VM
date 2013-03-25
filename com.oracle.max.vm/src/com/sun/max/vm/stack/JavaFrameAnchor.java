/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.oracle.graal.replacements.Snippet.Fold;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * A Java frame anchor is a stack-allocated block of memory in the frame of a Java
 * method that makes a transition between the 'in Java' and 'in native' states for a thread.
 * The anchor records the {@linkplain #PC instruction}, {@linkplain #SP stack} and {@linkplain #FP frame}
 * pointers of an execution point in a Java frame. Each anchor also points to the anchor further
 * (logically) down the stack of the closest caller that made a thread state transition.
 *
 * The head of the list of frame anchors for a thread is maintained in {@link VmThreadLocal#LAST_JAVA_FRAME_ANCHOR}.
 */
public enum JavaFrameAnchor {

    PREVIOUS,
    PC,
    SP,
    FP;

    /**
     * The offset of this field within an anchor.
     */
    public final int offset = Word.size() * ordinal();

    /**
     * Gets the value of this field.
     *
     * @param anchor the anchor from which to read the value
     * @return the value of this field within {@code anchor}
     */
    @INLINE
    public final Pointer get(Pointer anchor) {
        return anchor.readWord(offset).asPointer();
    }

    /**
     * Sets the value of this field.
     *
     * @param anchor the anchor in which to set the value
     * @param value the new value of this field in {@code anchor}
     */
    @INLINE
    public final void set(Pointer anchor, Word value) {
        anchor.writeWord(offset, value);
    }

    /**
     * Determines if a given anchor indicates a frame that transitioned into Java code.
     * That is, a frame in a {@linkplain VM_ENTRY_POINT VM entry point}.
     * This will return true if the {@link #PC} is zero.
     *
     * @param anchor the anchor to test
     * @return {@code true} if {@code anchor} is zero or {@code PC} is zero in {@code anchor}
     */
    public static boolean inJava(Pointer anchor) {
        return !anchor.isZero() && JavaFrameAnchor.PC.get(anchor).isZero();
    }

    /**
     * Gets the current anchor (which may be null) for a given thread.
     *
     * @param tla the thread locals for a thread
     * @return the Java frame anchor recorded for the thread denoted by {@code tla}
     */
    public static Pointer from(Pointer tla) {
        Pointer etla = ETLA.load(tla);
        return LAST_JAVA_FRAME_ANCHOR.load(etla);
    }

    /**
     * Prints a given list of anchors to the VM's {@linkplain Log log} stream.
     *
     * @param anchor the head of the anchor list to be printed
     */
    public static void log(Pointer anchor) {
        boolean lockDisabledSafepoints = Log.lock();
        while (!anchor.isZero()) {
            Log.print(anchor);
            Log.print("@{pc=");
            Log.print(PC.get(anchor));
            Log.print(", fp=");
            Log.print(FP.get(anchor));
            Log.print(", sp=");
            Log.print(SP.get(anchor));
            Log.print("} -> ");
            anchor = PREVIOUS.get(anchor);
        }
        Log.print(" null");
        Log.unlock(lockDisabledSafepoints);
    }

    public static final List<JavaFrameAnchor> VALUES = Arrays.asList(values());

    /**
     * Gets the storage size of a {@link JavaFrameAnchor}.
     */
    @Fold
    public static int size() {
        return VALUES.size() * Word.size();
    }

    /**
     * Creates an anchor in the frame of the caller (hence the {@link INLINE} annotation).
     */
    @INLINE
    public static Pointer create(Word sp, Word fp, CodePointer ip, Word previousAnchor) {
        Pointer anchor = Intrinsics.alloca(size(), false);
        FP.set(anchor, fp);
        SP.set(anchor, sp);
        PC.set(anchor, ip.toAddress());
        PREVIOUS.set(anchor, previousAnchor);
        return anchor;
    }
}
