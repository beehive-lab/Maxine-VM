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
/*VCSID=a1cb46be-a2e3-41e1-8141-1fdc93de6f4a*/
package com.sun.max.vm.compiler.target;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;

/**
 * Describes an exception handler for a given range of target instructions.
 * TargetExceptionHandler are organized in a linked list for a given range.
 *
 * @author Laurent Daynes
 */
public class TargetExceptionHandler {

    /**
     * Offset to the first instruction of the exception handler in the target method.
     */
    private final int _offsetInTarget;

    /**
     * Resolution guard for the exception class declared in the catch clause for this handler.
     * The exception class may not be resolved yet when a method is compiled. There's no need
     * to resolve the exception class when checking if a handler needs to handle the exception:
     * if the declared exception class wasn't resolved, an instance of it (or of one of its sub-classes)
     * couldn't be created, hence this handler cannot handle the exception.
     */
    private final ReferenceResolutionGuard _declaredExceptionResolutionGuard;

     /**
     * Next handler covering the same range of instructions as this handler.
     */
    private TargetExceptionHandler _next;

    private final ExceptionHandlerEntry _exceptionHandlerInfo;

    public TargetExceptionHandler(int offsetInTarget, ExceptionHandlerEntry exceptionHandlerEntry, ReferenceResolutionGuard guard, TargetExceptionHandler next) {
        _offsetInTarget = offsetInTarget;
        _declaredExceptionResolutionGuard = guard;
        _next = next;
        _exceptionHandlerInfo = exceptionHandlerEntry;
    }

    public ExceptionHandlerEntry exceptionHandlerEntry() {
        return _exceptionHandlerInfo;
    }

    public TargetExceptionHandler last() {
        TargetExceptionHandler lastHandler = this;
        TargetExceptionHandler next = lastHandler._next;
        while (next != null) {
            lastHandler =  next;
            next = lastHandler._next;
        }
        return  lastHandler;
    }

    public TargetExceptionHandler next() {
        return _next;
    }

    public void setNext(TargetExceptionHandler next) {
        _next = next;
    }

    /**
     * @param throwable
     * @return true if this handler catches {@code throwable}
     */
    public boolean handlesException(Object throwable) {
        assert throwable != null;
        if (_declaredExceptionResolutionGuard.isClear()) {
            // Guard isn't set. Either the class hasn't been loaded yet (in which case the
            // exception cannot be thrown), or it hasn't been resolved yet by guard.
            final ConstantPool constantPool = _declaredExceptionResolutionGuard.constantPool();
            final int index = _declaredExceptionResolutionGuard.constantPoolIndex();
            if (!constantPool.classAt(index).isResolvableWithoutClassLoading(constantPool)) {
                return false;
            }
            _declaredExceptionResolutionGuard.set(constantPool.classAt(index).resolve(constantPool, index));
        }
        final ClassActor declaredExceptionClassActor = (ClassActor) _declaredExceptionResolutionGuard.value();
        if (MaxineVM.isPrototyping()) {
            return declaredExceptionClassActor.toJava().isInstance(throwable);
        }
        return declaredExceptionClassActor.isInstance(throwable);
    }

    public String declaredExceptionName() {
        final ConstantPool constantPool = _declaredExceptionResolutionGuard.constantPool();
        final int index = _declaredExceptionResolutionGuard.constantPoolIndex();
        return constantPool.classAt(index).valueString(constantPool);
    }

    /**
     * @return offset in the target method to the first instruction of the handler.
     */
    public int offsetInTarget() {
        return _offsetInTarget;
    }

    public TargetExceptionHandler find(Object throwable) {
        TargetExceptionHandler handler = this;
        do {
            if (handler.handlesException(throwable)) {
                break;
            }
            handler = handler._next;
        } while(handler != null);
        return handler;
    }
}

