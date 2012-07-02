/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.LineNumberTable.Entry;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link ClassMethodActor} in the VM.
 * <p>
 * This object also provides access to compilations of the method in the VM, of which
 * there are 0, 1, or 2 in ordinary cases: possibly a "baseline" compilation and possibly
 * an "optimized" compilation.
 * <p>
 * The update strategy for this class relies upon the immutability of the class used to record the current
 * compilation state for the method:  {@link Compilations}.  If that object has not changed in
 * the {@link ClassMethodActor} then the fields holding compilations will not be revisited.
 * <p>
 * Note, however, that there can be active compilations of the method in situations where
 * an activation record exists on the stack for a compilation that might have been subsequently
 * replaced:  deoptimized, replaced by a baseline compilation, and optimized again.  No record
 * of these is kept.  Any forgotten compilation in this situation will have been invalidated.
 *
 * @see ClassMethodActor
 * @see Compilations
 * @see TeleObject
 */
public abstract class TeleClassMethodActor extends TeleMethodActor implements MethodProvider {

    private static final int TRACE_VALUE = 2;

    private static List<TeleTargetMethod> EMPTY = Collections.emptyList();

    private TeleObject currentCompiledState = null;

    /**
     * Currently available compilations.
     */
    private TeleTargetMethod baselineTargetMethod = null;
    private TeleTargetMethod optimizedTargetMethod = null;

    /**
     * A cached list of currently available compilations.
     */
    private List<TeleTargetMethod> compilations = EMPTY;

    /**
     * Constructs a {@link TeleObject} specialized for dealing with the information stored in a VM
     * {@link ClassMetholdActor}.
     * <p>
     * This constructor follows no {@link References} and in particular does not attempt to decode the compilation state
     * of the method. This avoids the infinite regress that can occur when the constructor for a mutually referential
     * object such as a {@link TeleTargetMethod} attempts to do the same thing.
     *
     * @param classMethodActorReference reference to an instance of {@link ClassMethodActor} in the VM.
     */
    protected TeleClassMethodActor(TeleVM vm, Reference classMethodActorReference) {
        super(vm, classMethodActorReference);
    }

    boolean updatingCompiledState;

    /** {@inheritDoc}
     * <p>
     * The compilation history associated with a {@link ClassMethodActor} in the VM
     * is assumed to be represented by an immutable object that is replaced each
     * time there is some change in the compilation state.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        try {
            final Reference compiledStateReference = jumpForwarder(fields().ClassMethodActor_compiledState.readReference(reference()));
            if (compiledStateReference.isZero()) {
                clearCompiledState();
            } else if (updatingCompiledState) {
                // We're already in an update; prevent infinite recursion
                Trace.line(1, "Cut off infinite recursion between TeleClassMethodActor.updateObjectCache() and TeleTargetMethod.updateObjectCache()");
            } else {
                updatingCompiledState = true;
                // the method has been compiled; see if it has changed since the last update.
                final TeleObject compiledState = objects().makeTeleObject(compiledStateReference);
                if (compiledState == null) {
                    clearCompiledState();
                } else if (compiledState != currentCompiledState) {
                    translateCompiledState(compiledState);
                    currentCompiledState = compiledState;
                }
                updatingCompiledState = false;
            }
        } catch (DataIOError dataIOError) {
            updatingCompiledState = false;
            // If something goes wrong, delay the cache update until next time.
            return false;
        }
        return true;
    }

    private void clearCompiledState() {
        currentCompiledState = null;
        baselineTargetMethod = null;
        optimizedTargetMethod = null;
        compilations = EMPTY;
    }

   /**
    * @return local {@link ClassMethodActor} corresponding the VM's {@link ClassMethodActor} for this method.
    */
    public ClassMethodActor classMethodActor() {
        return (ClassMethodActor) methodActor();
    }

    @Override
    public boolean isSubstituted() {
        final ClassMethodActor classMethodActor = classMethodActor();
        if (classMethodActor != null) {
            return METHOD_SUBSTITUTIONS.Static.findSubstituteFor(classMethodActor) != null;
        }
        return false;
    }

    @Override
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return this;
    }

    @Override
    public TeleCodeAttribute getTeleCodeAttribute() {
        if (vm().tryLock()) {
            try {
                final Reference codeAttributeReference = jumpForwarder(fields().ClassMethodActor_codeAttribute.readReference(reference()));
                return (TeleCodeAttribute) objects().makeTeleObject(codeAttributeReference);
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public TargetMethodAccess[] getTargetMethods() {
        TeleError.unimplemented();
        return null;
    }

    /**
     * Decodes the content of the field in {@link ClassMethodActor} that holds the
     * compilation history. For efficiency, it is stored differently depending on the
     * length of the history and whether a compilation is currently underway.  This method
     * must agree with the design of {@link Compilations}.
     *
     * @see ClassMethodActor
     * @see Compilations
     * @param compiledState an object that represents a polymorphic encoding of the current compilation state
     */
    private void translateCompiledState(TeleObject compiledState) {
        if (compiledState == null) {
            clearCompiledState();
        } else if (compiledState.classActorForObjectType().javaClass() == Compilations.class) {
            Reference optimizedReference = jumpForwarder(fields().Compilations_optimized.readReference(compiledState.reference()));
            Reference baselineReference = jumpForwarder(fields().Compilations_baseline.readReference(compiledState.reference()));
            compilations = new ArrayList<TeleTargetMethod>(2);
            if (optimizedReference.isZero()) {
                optimizedTargetMethod = null;
            } else {
                optimizedTargetMethod = (TeleTargetMethod) objects().makeTeleObject(optimizedReference);
                compilations.add(optimizedTargetMethod);
            }
            if (baselineReference.isZero()) {
                baselineTargetMethod = null;
            } else {
                baselineTargetMethod = (TeleTargetMethod) objects().makeTeleObject(baselineReference);
                compilations.add(baselineTargetMethod);
            }
        } else if (compiledState.classActorForObjectType().javaClass() == Compilation.class) {
            // this is a compilation that is currently underway, get the previous compiled state from it
            Reference prevCompilationsReference = jumpForwarder(fields().Compilation_prevCompilations.readReference(compiledState.reference()));
            if (!prevCompilationsReference.isZero()) {
                translateCompiledState(objects().makeTeleObject(prevCompilationsReference));
            }  else {
                // this is the first compilation, no previous state
                clearCompiledState();
            }
        }
    }

    /**
     * @return the number of times this method has been compiled
     */
    public final int compilationCount() {
        return compilations.size();
    }

    /**
     * @return all compilations of the method, in order, oldest first.
     */
    public Iterable<TeleTargetMethod> compilations() {
        return compilations;
    }

   /**
     * Gets the most recent, most optimized compilation of this method in the VM,
     * null if no compilations.
     */
    public TeleTargetMethod getCurrentCompilation() {
        if (optimizedTargetMethod != null) {
            return optimizedTargetMethod;
        }
        return baselineTargetMethod;
    }

    /**
     * @return the specified compilation of the method in the VM, first compilation at index=0; null if no such compilation.
     */
    public TeleTargetMethod getCompilation(Nature nature) {
        switch (nature) {
            case BASELINE:
                return baselineTargetMethod;
            case OPT:
                return optimizedTargetMethod;
            default:
                return null;
        }
    }

    @Override
    public LineTableEntry[] getLineTable() {

        final ClassMethodActor classMethodActor = this.classMethodActor();
        final Entry[] entries = classMethodActor.codeAttribute().lineNumberTable().entries();
        final LineTableEntry[] result = new LineTableEntry[entries.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LineTableEntry(entries[i].bci(), entries[i].lineNumber());
        }
        return result;
    }

    @Override
    public VariableTableEntry[] getVariableTable() {

        final ClassMethodActor classMethodActor = this.classMethodActor();
        final LocalVariableTable.Entry[] entries = classMethodActor.codeAttribute().localVariableTable().entries();

        final VariableTableEntry[] result = new VariableTableEntry[entries.length];

        for (int i = 0; i < result.length; i++) {

            String signature = null;
            if (entries[i].signatureIndex() == 0) {
                signature = entries[i].descriptor(classMethodActor.codeAttribute().cp).toString();
            } else {
                signature = entries[i].signature(classMethodActor.codeAttribute().cp).toString();
            }

            // TODO: Check if generic signature can be retrieved!
            final String genericSignature = signature;

            result[i] = new VariableTableEntry(entries[i].startBCI(), entries[i].length(), entries[i].name(classMethodActor.codeAttribute().cp).string, entries[i].slot(),
                            signature, genericSignature);
        }

        return result;
    }

    public final String getName() {
        return actorName().string;
    }

    public void writeSummary(PrintStream printStream) {
        final TeleCodeAttribute codeAttribute = getTeleCodeAttribute();
        // Always use the {@link ConstantPool} taken from the {@link CodeAttribute}; in a substituted method, the
        // constant pool for the bytecodes is the one from the origin of the substitution, not the current holder of the method.
        final TeleConstantPool teleConstantPool = codeAttribute.getTeleConstantPool();
        final ConstantPool constantPool = teleConstantPool.getTeleHolder().classActor().constantPool();
        final byte[] methodBytes = codeAttribute.readBytecodes();
        final PrintWriter printWriter = new PrintWriter(printStream);
        printWriter.println("code for: " + classMethodActor().format("%H.%n(%p)"));
        final BytecodePrinter bytecodePrinter = new BytecodePrinter(printWriter, constantPool);
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(bytecodePrinter);
        try {
            bytecodeScanner.scan(new BytecodeBlock(methodBytes));
        } catch (Throwable throwable) {
            printWriter.flush();
            ProgramWarning.message("could not print bytecodes: " + throwable);
        }
        printWriter.flush();
    }

    static class NativeStubCodeAttributeDeepCopier extends DeepCopier {
        TeleCodeAttribute teleCodeAttribute;

        NativeStubCodeAttributeDeepCopier(DeepCopier parent, TeleCodeAttribute teleCodeAttribute) {
            super(parent);
            this.teleCodeAttribute = teleCodeAttribute;
        }

        @Override
        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            if (fieldActor.type().name().contains("ConstantPool") && fieldActor.name().equals("cp")) {
                return teleCodeAttribute.getTeleConstantPool().deepCopy(this);
            }
            return super.makeDeepCopy(fieldActor, teleObject);
        }

    }

    /**
     * {@inheritDoc}
     * <p>
     * Default deep copying behavior is to use local copies of {@link ClassMethodActor} in place of those in the VM,
     * since they are presumed to be equivalent by virtue of being loaded from the same class files. In particular,
     * debugging information generated by the compiler is embedded in the {@link ClassMethodActor}'s
     * {@link CodeAttribute}.
     * <p>
     * This can create an inconsistency in the case of auto-generated native stubs, where the bytecodes are created at
     * runtime <i>and</i> may depend on tracing options set in the target VM.
     * <p>
     * In this case, we deep copy the {@link CodeAttribute} from the VM.
     */
    @Override
    public Object createDeepCopy(DeepCopier context) {
        Object result = super.createDeepCopy(context);
        ClassMethodActor classMethodActor = classMethodActor();
        if (classMethodActor != null && isNative() && !isSubstituted()) {
            // By default the CodeAttribute field defaults to the local instance
            // We force the target copy and the associated ConstantPool
            TeleCodeAttribute teleCodeAttribute = getTeleCodeAttribute();
            CodeAttribute codeAttribute = null;
            if (teleCodeAttribute != null) {
                DeepCopier dc = new NativeStubCodeAttributeDeepCopier(context, teleCodeAttribute);
                codeAttribute = (CodeAttribute) teleCodeAttribute.deepCopy(dc);
            }
            Field codeAttributeField = fields().ClassMethodActor_codeAttribute.fieldActor().toJava();
            codeAttributeField.setAccessible(true);
            Field compileeField = fields().ClassMethodActor_compilee.fieldActor().toJava();
            compileeField.setAccessible(true);
            try {
                // Patch into the local ClassMethodActor instance a copy of the method's code from the VM.
                codeAttributeField.set(classMethodActor, codeAttribute);
                // Force the ClassMethodActor's "compilee" field to point to itself, so that the code won't get changed.
                compileeField.set(classMethodActor, classMethodActor);
            } catch (IllegalArgumentException e) {
                TeleWarning.message("Failed to patch local ClassMethodActor with copied code");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                TeleWarning.message("Failed to patch local ClassMethodActor with copied code");
                e.printStackTrace();
            }
            Trace.line(TRACE_VALUE, tracePrefix() + "Using copied remote CodeAttribute for " + classMethodActor.name());
        }
        return result;
    }

}
