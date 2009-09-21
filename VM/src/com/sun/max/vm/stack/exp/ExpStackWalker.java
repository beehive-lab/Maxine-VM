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
import com.sun.max.vm.thread.*;

/**
 *
 *
 * @author Thomas Wuerthinger
 */
public abstract class ExpStackWalker {

    /**
     * A VM option for enabling stack frame walk tracing.
     */
    public static final VMBooleanXXOption traceStackWalk = register(new VMBooleanXXOption("-XX:-TraceStackWalk", ""), MaxineVM.Phase.STARTING);


    public static final class Cursor {

        public Pointer instructionPointer;
        public Pointer stackPointer;
        public Pointer framePointer;
        public boolean isTopFrame;

        private Cursor(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, boolean isTopFrame) {
            update(instructionPointer, stackPointer, framePointer, isTopFrame);
        }

        private Cursor() {
            clear();
        }

        /**
         * This method may be used to change the stack frame this cursor is pointing to.
         * @param instructionPointer the new instruction pointer
         * @param stackPointer the new stack pointer
         * @param framePointer the new frame pointer
         */
        public void update(Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
            update(instructionPointer, stackPointer, framePointer, false);
        }

        private void clear() {
            update(Pointer.zero(), Pointer.zero(), Pointer.zero(), false);
        }

        private void update(Cursor current) {
            update(current.instructionPointer, current.stackPointer, current.framePointer, current.isTopFrame);
        }

        private void update(Pointer instructionPointer, Pointer stackPointer, Pointer fp, boolean isTopFrame) {
            this.instructionPointer = instructionPointer;
            this.stackPointer = stackPointer;
            this.framePointer = fp;
            this.isTopFrame = isTopFrame;
        }

        private Cursor copy() {
            return new Cursor(instructionPointer, stackPointer, framePointer, isTopFrame);
        }
    }

    // Cursors pointing to the current and the last frame during the frame walk.
    private final Cursor current = new Cursor();
    private final Cursor last = new Cursor();

    private static interface InternalVisitor{
        void visit(ExpStackFrameLayout layout, Cursor last, Cursor current);
    }


    class PrepareReferenceMapVisitor implements InternalVisitor {

        private final ExpReferenceMapPreparer preparer;

        public PrepareReferenceMapVisitor(ExpReferenceMapPreparer preparer) {
            this.preparer = preparer;
        }

        @Override
        public void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            //layout.prepareReferenceMap(last, current, preparer);
        }

    }


    private ExpStackFrameLayout findStackFrameLayout(Cursor c) {
        return null;
    }

    private void advanceNativeFrame(Cursor current, int nativeCallPos) {
        current.clear();
    }

    private void walk(Pointer ip, Pointer sp, Pointer fp, InternalVisitor visitor) {

        // Get offset to the top of stack to the native call stack
        int nativeCallStackPos = readVmThreadLocal(VmThreadLocal.NATIVE_CALL_STACK_SIZE).asOffset().toInt();

        last.clear();
        current.update(ip, sp, fp, true);
        while (isValidIP(current)) {

            ExpStackFrameLayout layout = findStackFrameLayout(current);
            trace(layout, last, current);

            if (layout == null) {

                // We are in native code!
                if (nativeCallStackPos <= 0) {

                    // We are at the end of the stack!
                    if (traceStackWalk.getValue()) {
                        Log.print("End of native call stack reached!");
                    }

                    return;
                }

                last.update(current);
                advanceNativeFrame(current, nativeCallStackPos);
                nativeCallStackPos--;

            } else {

                visitor.visit(layout, last, current);
                last.update(current);
                layout.advance(current);
            }
        }


        if (traceStackWalk.getValue()) {
            Log.print("Unexpected end of stack walk!");
        }
    }

    private boolean isValidIP(Cursor current) {
        return !current.instructionPointer.isZero();
    }

    /**
     * Reads the value of a given VM thread local from the safepoint-enabled thread locals.
     *
     * @param local the VM thread local to read
     * @return the value (as a pointer) of {@code local} in the safepoint-enabled thread locals
     */
    public abstract Word readVmThreadLocal(VmThreadLocal local);

    private void trace(ExpStackFrameLayout layout, Cursor last, Cursor current) {

        if (traceStackWalk.getValue()) {
            boolean b = Log.lock();
            Log.print("Frame[ip=");
            Log.print(current.instructionPointer.toLong());
            Log.print(", sp=" + current.stackPointer.toLong());
            Log.print(", fp=" + current.framePointer.toLong());
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

    private class UnwindExceptionVisitor implements InternalVisitor {
        private final Throwable exception;

        public UnwindExceptionVisitor(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
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
        final GatherJavaFramesVisitor visitor = new GatherJavaFramesVisitor();
        walk(ip, sp, fp, visitor);
        return visitor.frames;
    }

    private class GatherJavaFramesVisitor implements InternalVisitor {
        public final List<ExpJavaStackFrame> frames = new ArrayList<ExpJavaStackFrame>(5);

        @Override
        public void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            layout.appendJavaFrames(current, frames);
        }
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
        final GatherRawFramesVisitor visitor = new GatherRawFramesVisitor();
        walk(ip, sp, fp, visitor);
        return visitor.frames;
    }

    private class GatherRawFramesVisitor implements InternalVisitor {
        public final List<ExpRawStackFrame> frames = new ArrayList<ExpRawStackFrame>(5);

        @Override
        public void visit(ExpStackFrameLayout layout, Cursor last, Cursor current) {
            frames.add(new ExpRawStackFrame(layout, current.copy()));
        }
    }
}
