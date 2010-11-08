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
package com.sun.max.vm.stack;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.thread.*;

/**
 * A Java frame anchor is a {@linkplain StackAllocate stack-allocated} block of memory in the frame of a Java
 * method that makes a transition between the 'in Java' and 'in native' states for a thread.
 * The anchor records the {@linkplain #PC instruction}, {@linkplain #SP stack} and {@linkplain #FP frame}
 * pointers of an execution point in a Java frame. Each anchor also points to the anchor further
 * (logically) down the stack of the closest caller that made a thread state transition.
 *
 * The head of the list of anchors for a thread is maintained in {@link VmThreadLocal#LAST_JAVA_FRAME_ANCHOR}.
 *
 * @author Doug Simon
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
     * @value value the new value of this field in {@code anchor}
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
    @FOLD
    public static int size() {
        return VALUES.size() * Word.size();
    }

    /**
     * Creates an anchor in the frame of the caller (hence the {@link INLINE} annotation).
     */
    @INLINE
    public static Pointer create(Word stackPointer, Word framePointer, Word instructionPointer, Word previousAnchor) {
        Pointer anchor = StackAllocate.stackAllocate(size());
        FP.set(anchor, framePointer);
        SP.set(anchor, stackPointer);
        PC.set(anchor, instructionPointer);
        PREVIOUS.set(anchor, previousAnchor);
        return anchor;
    }
}
