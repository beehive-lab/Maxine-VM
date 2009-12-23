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
package com.sun.max.vm.hotpath.compiler;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.cps.jit.*;
import com.sun.max.vm.compiler.cps.tir.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.thread.*;

public class JitTracer extends Tracer {

    private static ObjectThreadLocal<JitTracer> tracers = new ObjectThreadLocal<JitTracer>("JIT_TRACER", "Tracer used for HotPath compiler.") {
        @Override
        protected JitTracer initialValue() {
            return new JitTracer();
        }
    };

    /**
     * @return the {@link JitTracer} object associated with the current thread.
     */
    private static JitTracer current() {

        return tracers.get();
    }

    /**
     * Starts recording a new trace.
     */
    public static Address startCurrent(TreeAnchor anchor) {
        return current().start(anchor);
    }

    /**
     * Traces the instruction at the specified instructionPointer.
     */
    @UNSAFE
    public static Address traceCurrent(Pointer instructionPointer, Pointer stackPointer) {
        return current().trace(instructionPointer, stackPointer);
    }

    @UNSAFE
    private Address start(TreeAnchor anchor) {
        final boolean begin = begin(anchor);
        if (begin) {
            // Return the address of the traced bytecodes.
            // TODO: trace jit
            // final JitTargetMethod targetMethod = (JitTargetMethod) CompilationScheme.Static.compile(anchor.method());
            // return targetMethod.codeStart().plus(targetMethod.targetCodePositionFor(anchor.position()));
        }
        return Address.zero();
    }

    @UNSAFE
    private Address trace(Pointer instructionPointer, Pointer stackPointer) {
        /*
        assert _treeAnchor != null;

        // We have stopped tracing yet we still receive tracing notifications. This is because we may still have trace
        // instrumented frames on the stack, switch back to original frames and continue execution.
        if (_isTracing == false) {
            Console.println(Color.MAGENTA, "UNWIND");
            return finish(instructionPointer);
        }

        // Abort recording if we have exceeded the maximum trace length.
        if (_length++ > MAX_LENGTH) {
            return abort(instructionPointer, AbortReason.FAILED_LENGTH_METRIC);
        }

        final JitTargetMethod tracedMethod = (JitTargetMethod) Code.codePointerToTargetMethod(instructionPointer);
        final ClassMethodActor methodActor = tracedMethod.classMethodActor();
        final int bytecodePosition = tracedMethod.bytecodePositionFor(instructionPointer);
        final Bytecode bytecode = Bytecode.from(methodActor.codeAttribute().code()[bytecodePosition]);

        // If we've looped back to the anchor bytecode position then stop recording.
        if (bytecodePosition == _treeAnchor.position()) {
            if (_cycles++ > 0) {
                Console.println(Color.GREEN, "RECORDING COMPLETED: " + _treeAnchor);
                _treeAnchor.setTree(new TirTree());
                return finish(instructionPointer);
            }
        } else if (bytecode.is(Flags.INVOKE_)) {
            enter(instructionPointer, stackPointer);
        } else if (bytecode.is(Flags.RETURN_)) {
            exit(instructionPointer, stackPointer);
        } else {
            // return abort(instructionPointer, AbortReason.UNSUPPORTED_BYTECODE);
        }

        _recorder.record(methodActor, bytecodePosition, stackPointer);

        return Address.zero();
        */
        return null;
    }

    /**
     * Reverts the current thread's execution stack to use non traced JITed methods.
     * @param instructionPointer the {@link Pointer} to the current instruction.
     * @return
     */
    @UNSAFE
    private Address finish(Pointer instructionPointer) {
        super.finish();
        return getOriginalBytecodeAddress(instructionPointer);
    }

    @UNSAFE
    private Address abort(Pointer instructionPointer, AbortReason abortReason) {
        super.abort(abortReason);
        return finish(instructionPointer);
    }

    @UNSAFE
    private void enter(Pointer instructionPointer, Pointer stackPointer) {
//        for (int i = 0; i < 8; i++) {
//            Console.println(Color.LIGHTBLUE, stackPointer.plus(i * Word.size()).toHexString().toUpperCase() + " : " + stackPointer.getWord(i).toHexString().toUpperCase());
//        }
    }

    @UNSAFE
    private void exit(Pointer instructionPointer, Pointer stackPointer) {
//        for (int i = 0; i < 8; i++) {
//            Console.println(Color.LIGHTBLUE, stackPointer.plus(i * Word.size()).toHexString().toUpperCase() + " : " + stackPointer.getWord(i).toHexString().toUpperCase());
//        }
    }

    @UNSAFE
    private Address getOriginalBytecodeAddress(JitTargetMethod tracedMethod, int bytecodePosition) {
        final JitTargetMethod originalMethod = (JitTargetMethod) tracedMethod.classMethodActor().currentTargetMethod();
        return originalMethod.codeStart().plus(originalMethod.targetCodePositionFor(bytecodePosition));
    }

    @UNSAFE
    private Address getOriginalBytecodeAddress(Pointer instructionPointer) {
        final JitTargetMethod tracedMethod = (JitTargetMethod) Code.codePointerToTargetMethod(instructionPointer);
        final int bytecodePosition = tracedMethod.bytecodePositionFor(instructionPointer);
        return getOriginalBytecodeAddress(tracedMethod, bytecodePosition);
    }

    @Override
    protected boolean evaluateAcmpBranch(BranchCondition condition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean evaluateBranch(BranchCondition condition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean evaluateIcmpBranch(BranchCondition condition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected int evaluateInt(int stackDepth) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected boolean evaluateNullBranch(BranchCondition condition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Object evaluateObject(int stackDepth) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Bailout evaluateTree(TirTree tree) {
        // TODO Auto-generated method stub
        return null;
    }
}
