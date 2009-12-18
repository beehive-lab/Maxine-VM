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
package com.sun.max.vm.stack.exp;

import static com.sun.max.vm.VMOptions.*;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * This class implements stack walking functionality and can be used to walk the stack
 * for exception handling, preparing a reference map for GC, and inspecting the stack
 * frames for reflection and security purposes.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class ExpStackWalker {

    /**
     * A VM option for enabling stack frame walk tracing.
     */
    public static final VMBooleanXXOption traceStackWalk = register(new VMBooleanXXOption("-XX:-TraceStackWalk", ""), MaxineVM.Phase.STARTING);

    /**
     * This class represents the current stack frame that is being walked.
     * This includes the instruction pointer, the stack pointer, the frame pointer, and whether
     * the frame is the top frame.
     */
    public final class Cursor {

        /**
         * The current instruction pointer.
         */
        public Pointer ip = Pointer.zero();

        /**
         * The current stack pointer.
         */
        public Pointer sp = Pointer.zero();

        /**
         * The current frame pointer.
         */
        public Pointer fp = Pointer.zero();

        /**
         * Indicates whether this frame is on the top of the stack.
         */
        public boolean isTopFrame = false;

        private Cursor() {
        }

        /**
         * Updates the cursor to point to the next stack frame when advancing to the next frame in the stack.
         * This method implicitly sets {@link #isTopFrame} to {@code false}.
         * @param instructionPointer the new instruction pointer
         * @param stackPointer the new stack pointer
         * @param framePointer the new frame pointer
         */
        public void update(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
            setFields(instructionPointer, stackPointer, framePointer, false);
        }

        private void clear() {
            setFields(Pointer.zero(), Pointer.zero(), Pointer.zero(), false);
        }

        private void copyFrom(Cursor other) {
            setFields(other.ip, other.sp, other.fp, other.isTopFrame);
        }

        private void setFields(Pointer instructionPointer, Pointer stackPointer, Pointer fp, boolean isTopFrame) {
            this.ip = instructionPointer;
            this.sp = stackPointer;
            this.fp = fp;
            this.isTopFrame = isTopFrame;
        }

        private Cursor copy() {
            Cursor other = new Cursor();
            other.setFields(ip, sp, fp, isTopFrame);
            return other;
        }

        public ExpStackAccess stackAccess() {
            return stackAccess;
        }
    }

    // Cursors pointing to the current and the last frame during the frame walk.
    private final ExpStackAccess stackAccess;
    private final Cursor current = new Cursor();
    private final Cursor last = new Cursor();
    private final PrepareReferenceMapVisitor prepareVisitor;

    /**
     * Creates a new stack walker with the specified stack access and specified pre-allocated
     * reference map preparer.
     *
     * @param stackAccess the object allowing access to the stack
     * @param preparer the reference map preparer object
     */
    public ExpStackWalker(ExpStackAccess stackAccess, ExpReferenceMapPreparer preparer) {
        this.stackAccess = stackAccess;
        this.prepareVisitor = new PrepareReferenceMapVisitor(preparer);
    }

    private <T extends InternalVisitor> T walk(Pointer ip, Pointer sp, Pointer fp, T visitor) {
        last.clear();
        current.setFields(ip, sp, fp, true);

        while (stackAccess.isValidIP(current.ip)) {
            ExpStackFrameLayout layout = stackAccess.identify(current);
            trace(layout, last, current);

            if (layout == null) {
                // We are at the end of the stack!
                if (traceStackWalk.getValue()) {
                    Log.print("End of native call stack reached.");
                }
                return visitor;
            }
            // found a stack frame layout, visit it
            visitor.visit(layout, last, current);
            // copy current to last
            last.copyFrom(current);
            // ask the stack frame layout to advance to the next
            layout.advance(current);
        }

        if (traceStackWalk.getValue()) {
            Log.print("Invalid IP reached: ");
            Log.print(current.ip);
        }

        return visitor;
    }

    /**
     * Walks the stack to find an appropriate exception handler (based on the type of the thrown exception) for the
     * given throwable. When a handler is found, the stack is unwind and execution is resumed at the handler.
     *
     * @param ip the instruction pointer of the top most frame
     * @param sp the stack pointer of the top most frame
     * @param fp the frame pointer of the top most frame
     * @param exception the thrown exception object
     */
    public void unwindToExceptionHandler(Pointer ip, Pointer sp, Pointer fp, Throwable exception) {
        walk(ip, sp, fp, new UnwindExceptionVisitor(exception));
    }

    /**
     * Walks the stack and returns a list of Java stack frames. Native methods or stubs for which no Java method exists
     * are skipped in the stack walk. Note that the stack frame of one compiled method may result in more than one Java
     * stack frame depending on method inlining.
     *
     * @param ip the instruction pointer of the top most frame
     * @param sp the stack pointer of the top most frame
     * @param fp the frame pointer of the top most frame
     * @return a list of Java stack frames
     */
    public List<ExpJavaStackFrame> gatherJavaFrames(Pointer ip, Pointer sp, Pointer fp) {
        return walk(ip, sp, fp, new GatherJavaFramesVisitor()).frames;
    }
    /**
     * Walks the stack and returns a list of stack frames associated with compiled methods. Native methods are skipped
     * in the stack walk.
     *
     * @param ip the instruction pointer of the top most frame
     * @param sp the stack pointer of the top most frame
     * @param fp the frame pointer of the top most frame
     * @return a list of stack frames
     */
    public List<ExpRawStackFrame> gatherRawFrames(Pointer ip, Pointer sp, Pointer fp) {
        return walk(ip, sp, fp, new GatherRawFramesVisitor()).frames;
    }

    /**
     * Walks the stack and prepares the reference map for each frame.
     * @param ip the instruction pointer of the top most frame
     * @param sp the stack pointer of the top most frame
     * @param fp the frame pointer of the top most frame
     * @return the reference map preparer supplied to the constructor of this class
     */
    public ExpReferenceMapPreparer prepareReferenceMap(Pointer ip, Pointer sp, Pointer fp) {
        return walk(ip, sp, fp, prepareVisitor).preparer;
    }

    /**
     * Internal visitor to perform the appropriate action(s) per frame.
     */
    abstract static class InternalVisitor {
        abstract void visit(ExpStackFrameLayout layout, Cursor last, Cursor current);
    }

    /**
     * Visitor to prepare the reference map for a frame.
     */
    static class PrepareReferenceMapVisitor extends InternalVisitor {
        private final ExpReferenceMapPreparer preparer;

        public PrepareReferenceMapVisitor(ExpReferenceMapPreparer preparer) {
            this.preparer = preparer;
        }

        @Override
        void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            layout.prepareReferenceMap(current, last, preparer);
        }
    }

    /**
     * Visitor to unwind the stack to an exception handler.
     */
    static class UnwindExceptionVisitor extends InternalVisitor {
        private final Throwable exception;

        public UnwindExceptionVisitor(Throwable exception) {
            this.exception = exception;
        }

        @Override
        void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            Address catchAddress = layout.findCatchAddress(current, exception.getClass());
            if (!catchAddress.isZero()) {
                if (traceStackWalk.getValue()) {
                    boolean b = Log.lock();
                    Log.print("Unwinding to method ");
                    Log.print(layout.description(current));
                    Log.print(" and address ");
                    Log.print(catchAddress.toLong());
                    Log.unlock(b);
                }

                layout.unwindToAddress(exception, current, catchAddress);
                FatalError.unexpected("Should not reach here, unwind should return directly to catch address");
            }
        }
    }

    /**
     * Visitor to gather the Java-level frames of a stack.
     */
    static class GatherJavaFramesVisitor extends InternalVisitor {
        public final List<ExpJavaStackFrame> frames = new ArrayList<ExpJavaStackFrame>(5);

        @Override
        void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            layout.appendJavaFrames(current, frames, false);
        }
    }

    /**
     * Visitor to gather the raw frames of a stack.
     */
    static class GatherRawFramesVisitor extends InternalVisitor {
        public final List<ExpRawStackFrame> frames = new ArrayList<ExpRawStackFrame>(5);

        @Override
        void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            frames.add(new ExpRawStackFrame(layout, current.copy()));
        }
    }

    private void trace(ExpStackFrameLayout layout, Cursor last, Cursor current) {
        if (traceStackWalk.getValue()) {
            boolean b = Log.lock();
            Log.print("Frame[ip=");
            Log.print(current.ip.toLong());
            Log.print(", sp=" + current.sp.toLong());
            Log.print(", fp=" + current.fp.toLong());
            Log.print(", layout=");

            if (layout != null) {
                Log.print(layout.description(current));
            } else {
                Log.print("null");
            }

            Log.println("]");
            Log.unlock(b);
        }
    }

}
