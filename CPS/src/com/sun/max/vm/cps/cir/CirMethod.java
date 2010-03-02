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
package com.sun.max.vm.cps.cir;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.cir.builtin.*;
import com.sun.max.vm.cps.cir.bytecode.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * CIR representation of Java methods.
 *
 * @author Bernd Mathiske
 */
public class CirMethod extends CirProcedure implements CirRoutine, CirFoldable, CirInlineable, IrMethod {

    private final ClassMethodActor classMethodActor;
    private final Kind[] parameterKinds;

    public CirMethod(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
        this.parameterKinds = classMethodActor.getParameterKinds();
    }

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    public MethodActor foldingMethodActor() {
        return classMethodActor.compilee();
    }

    public Kind resultKind() {
        return classMethodActor.resultKind();
    }

    @Override
    public Kind[] parameterKinds() {
        return parameterKinds;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public String name() {
        return classMethodActor.name.toString();
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return MethodID.fromMethodActor(classMethodActor);
    }

    public String getQualifiedName() {
        return classMethodActor.holder().name + "." + name();
    }

    public void cleanup() {
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public Value value() {
        return ReferenceValue.from(classMethodActor);
    }

    public CirCall fold(CirOptimizer cirOptimizer, CirValue... arguments) throws CirFoldingException {
        if (MaxineVM.isHosted()) {
            // This happens when interpreting a CIR method with the CirInterpreter
            if (!isFoldable(cirOptimizer, arguments)) {
                return CirFoldable.Static.fold(this, arguments);
            }
        }

        final CirGenerator cirGenerator = cirOptimizer.cirGenerator();
        if (classMethodActor.isStatic() && arguments.length == 2) {
            // no application arguments, just the 2 continuations
            cirGenerator.makeIrMethod(this);
            return inline(cirOptimizer, arguments, NO_JAVA_FRAME_DESCRIPTOR);
        }
        return CirFoldable.Static.fold(this, arguments);
    }

    public boolean isFoldable(CirOptimizer cirOptimizer, CirValue[] arguments) {
        final ClassMethodActor compilee = classMethodActor.compilee();
        if (compilee.isHiddenToReflection()) {
            return false;
        }
        if (!compilee.isDeclaredFoldable()) {
            return false;
        }
        // Ignore the continuation parameters
        assert arguments.length >= 2;
        int max = arguments.length - 2;
        for (int i = 0; i < max; i++) {
            final CirValue argument = arguments[i];
            if (argument instanceof CirConstant || argument instanceof CirMethod) {
                continue;
            }
            return false;
        }
        return true;
    }

    private CirBytecode cirBytecode;

    /**
     * Determines if this object represents a native stub generated for a native method.
     */
    public boolean isNative() {
        return classMethodActor().compilee().isNative();
    }

    @RESET
    private CirClosure cachedClosure;

    private void cache(CirClosure closure) {
        if (MaxineVM.isHosted()) {
            // So let's not fill the host VM's heap too much here.
        } else {
            cachedClosure = closure;
        }
    }

    public final CirClosure copyClosure() {
        assert cirBytecode != null;
        traceBeforeDecoding();
        final CirClosure closure = (CirClosure) CirBytecodeReader.read(cirBytecode);
        traceAfterDecoding();
        cache(closure);
        return closure;
    }

    private void traceAfterDecoding() {
        if (Trace.hasLevel(6)) {
            Trace.end(6, "Decoding CIR for " + this);
        }
    }

    private void traceBeforeDecoding() {
        if (Trace.hasLevel(6)) {
            Trace.begin(6, "Decoding CIR for " + this);
        }
    }

    public final CirClosure closure() {
        CirClosure closure = cachedClosure;
        if (closure == null) {
            closure = copyClosure();
            cache(closure);
        }
        return closure;
    }

    public void setGenerated(CirClosure closure) {
        // The variables in the canonical CIR bytecode form must have unique serial numbers
        CirAlphaConversion.apply(closure);
        cache(closure);

        traceBeforeEncoding();
        final CirBytecodeWriter writer = new CirBytecodeWriter(closure, this);
        cirBytecode = writer.bytecode();
        traceAfterEncoding();
    }

    private void traceAfterEncoding() {
        if (Trace.hasLevel(6)) {
            CirBytecodeReader.trace(cirBytecode, Trace.stream(), false);
            Trace.end(6, "Encoding CIR for " + this);
        }
    }

    private void traceBeforeEncoding() {
        if (Trace.hasLevel(6)) {
            Trace.begin(6, "Encoding CIR for " + this);
        }
    }

    public final boolean isGenerated() {
        return cirBytecode != null;
    }

    protected boolean makingIr;

    public synchronized CirCall inline(CirOptimizer cirOptimizer, CirValue[] arguments, CirJavaFrameDescriptor javaFrameDescriptor) {
        if (cirBytecode == null) {
            // This usually denotes a case where the code for a snippet includes
            // a section that is only executed if MaxineVM.isHosted() is true and
            // this section uses the snippet. The solution in this case is to
            // put the bootstrapping-only code in a separate method that will not be
            // inlined
            ProgramError.check(!makingIr, "cannot inline " + classMethodActor() + " while making its IR");

            makingIr = true;
            cirOptimizer.cirGenerator().makeIrMethod(this);
        }
        final CirClosure closure = copyClosure();
        if (javaFrameDescriptor != null) {
            javaFrameDescriptor.pushInto(closure);
        }
        return CirBetaReduction.applyMultiple(closure, arguments);
    }

    public boolean mustNotInline(CirOptimizer cirOptimizer, CirValue[] arguments) {
        return false;
    }

    public boolean neverInline() {
        return classMethodActor().isDeclaredNeverInline();
    }

    public boolean needsJavaFrameDescriptor() {
        return true;
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitMethod(this);
    }

    @Override
    public void acceptBlockScopedVisitor(CirBlockScopedVisitor visitor, CirBlock scope) {
        visitor.visitMethod(this, scope);
    }

    @Override
    public CirNode acceptTransformation(CirTransformation transformation) {
        return transformation.transformMethod(this);
    }

    @Override
    public boolean acceptUpdate(CirUpdate update) {
        return update.updateMethod(this);
    }

    public String traceToString() {
        return traceToString(false);
    }

    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return CirTraceObserver.class;
    }

    public boolean contains(final Builtin builtin, boolean defaultResult) {
        return CirSearch.byPredicate(closure(), new CirPredicate() {
            @Override
            public boolean evaluateBuiltin(CirBuiltin cirBuiltin) {
                return cirBuiltin.builtin == builtin;
            }
        }) != null;
    }

    public int reasonsMayStop() {
        return Stoppable.CALL_STOP;
    }

    public int count(final Builtin builtin, int defaultResult) {
        return CirCount.apply(closure(), new CirPredicate() {
            @Override
            public boolean evaluateBuiltin(CirBuiltin cirBuiltin) {
                return cirBuiltin.builtin == builtin;
            }
        });
    }

}
