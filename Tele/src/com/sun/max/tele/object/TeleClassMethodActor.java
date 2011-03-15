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

import java.util.*;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.LineNumberTable.Entry;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link ClassMethodActor} in the VM.
 *
 * @author Michael Van De Vanter
 * @author Ben L. Titzer
 */
public abstract class TeleClassMethodActor extends TeleMethodActor implements MethodProvider {

    private static final TeleTargetMethod[] NO_TARGET_METHODS = {};

    /**
     * Cached history of compilation for this method in the tele VM.
     */
    private TeleTargetMethod[] teleTargetMethodHistory = NO_TARGET_METHODS;

    // Keep construction minimal for both performance and synchronization.
    protected TeleClassMethodActor(TeleVM vm, Reference classMethodActorReference) {
        super(vm, classMethodActorReference);
    }

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
            final Reference targetStateReference = vm().teleFields().ClassMethodActor_targetState.readReference(reference());
            if (!targetStateReference.isZero()) {
                // the method has been compiled; check the type to determine the number of times
                translateTargetState(heap().makeTeleObject(targetStateReference));
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
            } catch (DataIOError dataIOError) {
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public TargetMethodAccess[] getTargetMethods() {
        return teleTargetMethodHistory;
    }

    private void translateTargetState(TeleObject targetState) {
        if (targetState instanceof TeleTargetMethod) {
            // The object actually is an instance of TargetMethod, which means that
            // it is the first and only compilation so far.
            TeleTargetMethod teleTargetMethod = (TeleTargetMethod) targetState;
            if (teleTargetMethodHistory.length == 0) {
                // We haven't seen this first compilation yet: record it.
                teleTargetMethodHistory = new TeleTargetMethod[] {teleTargetMethod};
            } else {
                // We already have recorded a compilation history, so it
                // should have only the one compilation and it shouldn't have changed.
                assert teleTargetMethodHistory.length == 1;
                if (teleTargetMethodHistory[0] != teleTargetMethod) {
                    TeleWarning.message("Compilation anomaly in " + getClass().getName());
                    teleTargetMethodHistory = new TeleTargetMethod[] {teleTargetMethod};
                }
            }
        } else if (targetState instanceof TeleArrayObject) {
            // The object actually is an instance of TargetMethod[], which means that
            // there has been more than one compilation so far.
            final TeleArrayObject teleTargetMethodHistoryArray = (TeleArrayObject) targetState;
            int numberOfCompilations = teleTargetMethodHistoryArray.length();
            teleTargetMethodHistory = new TeleTargetMethod[numberOfCompilations];
            for (int i = 0; i < numberOfCompilations; i++) {
                // copy the target methods in reverse order (most recent is stored at beginning of array)
                final Reference targetMethodReference = teleTargetMethodHistoryArray.readElementValue(i).asReference();
                teleTargetMethodHistory[numberOfCompilations - i - 1] = (TeleTargetMethod) heap().makeTeleObject(targetMethodReference);
            }

        } else if (targetState.classActorForObjectType().javaClass() == Compilation.class) {
            // this is a compilation, get the previous target state from it
            Reference previousTargetStateReference = vm().teleFields().Compilation_previousTargetState.readReference(targetState.reference());
            if (!previousTargetStateReference.isZero()) {
                translateTargetState(heap().makeTeleObject(previousTargetStateReference));
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


}
