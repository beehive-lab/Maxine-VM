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
package com.sun.max.tele.object;

import java.io.*;
import java.util.*;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.LineNumberTable.Entry;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link ClassMethodActor} in the VM.
 * <p>
 * Note that the compilations of a method are recorded as a {@linkplain ClassMethodActor#compiledState single polymorphic object} field
 * that must be decoded using knowledge of how the encoding is done in the VM.
 * <p>
 * It is more important
 *
 * @see ClassMethodActor
 * @see Compilations
 * @see TeleObject
 */
public abstract class TeleClassMethodActor extends TeleMethodActor implements MethodProvider {

    private static final TeleTargetMethod[] NO_TARGET_METHODS = {};

    /**
     * Cached history of compilation for this method in the tele VM.
     */
    private TeleTargetMethod[] teleTargetMethodHistory = NO_TARGET_METHODS;

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
     * is assumed to grow monotonically, as recompilations
     * take place in the VM, and that the old compilations still persist.
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        try {
            final Reference compiledStateReference = vm().teleFields().ClassMethodActor_compiledState.readReference(reference());
            if (!compiledStateReference.isZero()) {
                if (!updatingCompiledState) {
                    updatingCompiledState = true;
                    // the method has been compiled; check the type to determine the number of times
                    translateCompiledState(heap().makeTeleObject(compiledStateReference));
                    updatingCompiledState = false;
                } else {
                    // prevent infinite recursion
                    Trace.line(1, "Cut off infinite recursion between TeleClassMethodActor.updateObjectCache() and TeleTargetMethod.updateObjectCache()");
                }
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
            return false;
        }
        return true;
    }

   /**
    * @return local {@link ClassMethodActor} corresponding the VM's {@link ClassMethodActor} for this method.
    */
    public ClassMethodActor classMethodActor() {
        return (ClassMethodActor) methodActor();
    }

    @Override
    public TeleClassMethodActor getTeleClassMethodActorForObject() {
        return this;
    }

    @Override
    public TeleCodeAttribute getTeleCodeAttribute() {
        Reference codeAttributeReference = null;
        if (vm().tryLock()) {
            try {
                codeAttributeReference = vm().teleFields().ClassMethodActor_codeAttribute.readReference(reference());
                if (codeAttributeReference != null) {
                    return (TeleCodeAttribute) heap().makeTeleObject(codeAttributeReference);
                }
            } catch (TerminatedProcessIOException terminatedProcessIOException) {
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public TargetMethodAccess[] getTargetMethods() {
        return teleTargetMethodHistory;
    }

    /**
     * Decodes the content of the field in {@link ClassMethodActor} that holds the
     * compilation history. For efficiency, it is stored differently depending on the
     * length of the history and whether a compilation is currently underway.  This method
     * must agree with the design of {@link Compilations}.
     *
     * @see ClassMethodActor
     * @see Compilations
     * @param compiledState an object that represents an encoded compilation history
     */
    private void translateCompiledState(TeleObject compiledState) {
        if (compiledState == null) {
            return;
        }
        if (compiledState.classActorForObjectType().javaClass() == Compilations.class) {
            Reference interpreterCompatibleReference = vm().teleFields().Compilations_interpreterCompatible.readReference(compiledState.reference());
            Reference optimizedReference = vm().teleFields().Compilations_optimized.readReference(compiledState.reference());
            ArrayList<TeleTargetMethod> hist = new ArrayList<TeleTargetMethod>(2);
            if (!optimizedReference.isZero()) {
                hist.add((TeleTargetMethod) heap().makeTeleObject(optimizedReference));
            }
            if (!interpreterCompatibleReference.isZero()) {
                hist.add((TeleTargetMethod) heap().makeTeleObject(interpreterCompatibleReference));
            }
            teleTargetMethodHistory = hist.toArray(new TeleTargetMethod[hist.size()]);
        } else if (compiledState.classActorForObjectType().javaClass() == Compilation.class) {
            // this is a compilation that is currently underway, get the previous compiled state from it
            Reference prevCompilationsReference = vm().teleFields().Compilation_prevCompilations.readReference(compiledState.reference());
            if (!prevCompilationsReference.isZero()) {
                translateCompiledState(heap().makeTeleObject(prevCompilationsReference));
            }  else {
                // this is the first compilation, no previous state
                teleTargetMethodHistory = NO_TARGET_METHODS;
            }
        }
    }

    /**
     * @return the number of times this method has been compiled
     */
    public final int compilationCount() {
        return teleTargetMethodHistory.length;
    }

    /**
     * @return all compilations of the method, in order, oldest first.
     */
    public Iterable<TeleTargetMethod> compilations() {
        return Arrays.asList(teleTargetMethodHistory);
    }

   /**
     * @return  surrogate for the most recent compilation of this method in the VM,
     * null if no compilations.
     */
    public TeleTargetMethod getCurrentCompilation() {
        if (compilationCount() > 0) {
            return teleTargetMethodHistory[teleTargetMethodHistory.length - 1];
        }
        return null;
    }

    /**
     * @return the specified compilation of the method in the VM, first compilation at index=0; null if no such compilation.
     */
    public TeleTargetMethod getCompilation(int index) {
        if (0 <= index && index < teleTargetMethodHistory.length) {
            return teleTargetMethodHistory[index];
        }
        return null;
    }

    /**
     * @param teleTargetMethod
     * @return the position of the specified compilation in the VM in the compilation history of this method, -1 if not present
     */
    public int compilationIndexOf(TeleTargetMethod teleTargetMethod) {
        for (int i = 0; i <= teleTargetMethodHistory.length - 1; i++) {
            if (teleTargetMethodHistory[i] == teleTargetMethod) {
                return i;
            }
        }
        return -1;
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

}
