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
package com.sun.max.tele.object;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.LineNumberTable.*;
import com.sun.max.vm.reference.*;

/**
 *  Canonical surrogate for an object of type {@link ClassMethodActor} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public abstract class TeleClassMethodActor extends TeleMethodActor implements MethodProvider {

    /**
     * @param methodActor Local {@link MethodActor} object.
     * @return Local {@link TeleClassMethodActor} representing the corresponding {@link MethodActor} in the {@link TeleVM},
     *  null if unknown to the registry.
     */
    public static TeleClassMethodActor get(TeleVM teleVM, MethodActor methodActor) {
        // TODO (mlvdv)  Discriminate based on ClassLoader
        for (TargetCodeRegion targetCodeRegion : teleVM.teleCodeRegistry().targetCodeRegions()) {
            final TeleClassMethodActor teleClassMethodActor = targetCodeRegion.teleTargetRoutine().getTeleClassMethodActor();
            if (teleClassMethodActor.classMethodActor() == methodActor) {
                return teleClassMethodActor;
            }
        }
        return null;
    }

    // Keep construction minimal for both performance and synchronization.
    protected TeleClassMethodActor(TeleVM teleVM, Reference classMethodActorReference) {
        super(teleVM, classMethodActorReference);
    }

   /**
    * @return local {@link ClassMethodActor} corresponding the the {@link TeleVM}'s {@link ClassMethodActor} for this method.
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
        final Reference codeAttributeReference = teleVM().fields().ClassMethodActor_codeAttribute.readReference(reference());
        return (TeleCodeAttribute) TeleObject.make(teleVM(), codeAttributeReference);
    }

    /**
     * Cached history of compilation for this method in the tele VM.  Null means not initialized yet.
     */
    private TeleTargetMethod[] _teleTargetMethodHistory = null;

    private void initialize() {
        if (_teleTargetMethodHistory == null) {
            _teleTargetMethodHistory = new TeleTargetMethod[0];
            readTeleMethodState();
        }
    }

    @Override
    public TargetMethodAccess[] getTargetMethods() {
        return _teleTargetMethodHistory;
    }

    public void refreshView() {
        initialize();
        readTeleMethodState();
    }

    /**
     * Refreshes cache of information about the compilation state of this method in the {@link TeleVM}.
     */
    private void readTeleMethodState() {
        final Reference methodStateReference = teleVM().fields().ClassMethodActor_methodState.readReference(reference());
        if (!methodStateReference.isZero()) {
            final int numberOfCompilations =  teleVM().fields().MethodState_numberOfCompilations.readInt(methodStateReference);
            if (numberOfCompilations != _teleTargetMethodHistory.length) {
                _teleTargetMethodHistory = new TeleTargetMethod[numberOfCompilations];
                final Reference targetMethodHistoryArrayReference = teleVM().fields().MethodState_targetMethodHistory.readReference(methodStateReference);
                final TeleArrayObject teleTargetMethodHistoryArray = (TeleArrayObject) TeleObject.make(teleVM(), targetMethodHistoryArrayReference);
                for (int i = 0; i <= numberOfCompilations - 1; i++) {
                    final Reference targetMethodReference = teleTargetMethodHistoryArray.readElementValue(i).asReference();
                    _teleTargetMethodHistory[i] = (TeleTargetMethod) TeleObject.make(teleVM(), targetMethodReference);
                }
            }
        }
    }

    /**
     * @return the number of times this method has been compiled
     */
    public final int numberOfCompilations() {
        initialize();
        return _teleTargetMethodHistory.length;
    }
    /**
     * @return whether there is any compiled target code for this method.
     */
    public boolean hasTargetMethod() {
        return numberOfCompilations() > 0;
    }

    /**
     * @return all compilations of the method, in order, oldest first.
     */
    public Iterable<TeleTargetMethod> targetMethods() {
        if (hasTargetMethod()) {
            return Arrays.iterable(_teleTargetMethodHistory);
        }
        return Arrays.iterable(new TeleTargetMethod[0]);
    }

   /**
     * @return  surrogate for the most recent compilation of this method in the {@link TeleVM}.
     */
    public TeleTargetMethod getCurrentJavaTargetMethod() {
        if (hasTargetMethod()) {
            return _teleTargetMethodHistory[_teleTargetMethodHistory.length - 1];
        }
        return null;
    }

    /**
     * @return the specified compilation of the method in the {@link TeleVM}, first compilation at index=0; null if no such compilation.
     */
    public TeleTargetMethod getJavaTargetMethod(int index) {
        initialize();
        if (0 <= index && index < _teleTargetMethodHistory.length) {
            return _teleTargetMethodHistory[index];
        }
        return null;
    }

    /**
     * @param teleTargetMethod
     * @return the position of the specified compilation in the {@link TeleVM} in the compilation history of this method, -1 if not present
     */
    public int indexOf(TeleTargetMethod teleTargetMethod) {
        initialize();
        for (int i = 0; i <= _teleTargetMethodHistory.length - 1; i++) {
            if (_teleTargetMethodHistory[i] == teleTargetMethod) {
                return i;
            }
        }
        return -1;
    }

     /**
     * Sets a target breakpoint in the current compilation in the {@link TeleVM}, null if no compilation.
     */
    public TeleTargetBreakpoint setTargetBreakpointAtEntry() {
        // TODO (mlvdv)  Deprecate, support only for specified compilations, not the "current" one.
        if (hasTargetMethod()) {
            return getCurrentJavaTargetMethod().setTargetBreakpointAtEntry();
        }
        return null;
    }


    @Override
    public LineTableEntry[] getLineTable() {

        final ClassMethodActor classMethodActor = this.classMethodActor();
        final Entry[] entries = classMethodActor.codeAttribute().lineNumberTable().entries();
        final LineTableEntry[] result = new LineTableEntry[entries.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LineTableEntry(entries[i].position(), entries[i].lineNumber());
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
                signature = entries[i].descriptor(classMethodActor.codeAttribute().constantPool()).toString();
            } else {
                signature = entries[i].signature(classMethodActor.codeAttribute().constantPool()).toString();
            }

            // TODO: Check if generic signature can be retrieved!
            final String genericSignature = signature;

            result[i] = new VariableTableEntry(entries[i].startPosition(), entries[i].length(), entries[i].name(classMethodActor.codeAttribute().constantPool()).string(), entries[i].slot(),
                            signature, genericSignature);
        }

        return result;
    }

}
