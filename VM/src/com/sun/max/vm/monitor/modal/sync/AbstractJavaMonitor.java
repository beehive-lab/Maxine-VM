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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.sync.JavaMonitorManager.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Provides basic services to allow subclasses to be managed by JavaMonitorManager.
 *
 * @author Simon Wilkinson
 */
abstract class AbstractJavaMonitor implements ManagedMonitor {

    private Object _boundObject;
    protected volatile VmThread _ownerThread;
    protected int _recursionCount;
    private Word _displacedMiscWord;

    protected BindingProtection _bindingProtection;
    private Word _preGCLockWord;

    private static final FieldActor _displacedMiscWordFieldActor = FieldActor.findInstance(AbstractJavaMonitor.class, "_displacedMiscWord");

    // Support for direct linked lists of JavaMonitors.
    private ManagedMonitor _next;

    protected AbstractJavaMonitor() {
        _bindingProtection = BindingProtection.PRE_ACQUIRE;
    }

    @Override
    public abstract void monitorEnter();

    @Override
    public abstract void monitorExit();

    @Override
    public abstract void monitorWait(long timeoutMilliSeconds) throws InterruptedException;

    @Override
    public abstract void monitorNotify(boolean all);

    @Override
    public abstract void monitorPrivateAcquire(VmThread owner, int lockQty);

    @Override
    public abstract void monitorPrivateRelease();

    @Override
    public abstract void alloc();

    @Override
    public final boolean isOwnedBy(VmThread thread) {
        return _ownerThread == thread;
    }

    @Override
    public final Word displacedMisc() {
        return _displacedMiscWord;
    }

    @Override
    public final void setDisplacedMisc(Word lockWord) {
        _displacedMiscWord = lockWord;
    }

    @Override
    public final Word compareAndSwapDisplacedMisc(Word suspectedValue, Word newValue) {
        return Reference.fromJava(this).compareAndSwapWord(_displacedMiscWordFieldActor.offset(), suspectedValue, newValue);
    }

    @Override
    public void reset() {
        _boundObject = null;
        _ownerThread = null;
        _recursionCount = 0;
        _displacedMiscWord = Word.zero();
        _preGCLockWord = Word.zero();
        _bindingProtection = BindingProtection.PRE_ACQUIRE;
    }

    @Override
    public final void setBoundObject(Object object) {
        _boundObject = object;
    }

    @Override
    public final Object boundObject() {
        return _boundObject;
    }

    @Override
    public final boolean isBound() {
        return _boundObject != null;
    }

    @Override
    public final boolean isHardBound() {
        return isBound() && ObjectAccess.readMisc(_boundObject).equals(InflatedMonitorLockWord64.boundFromMonitor(this));
    }

    @Override
    public final void preGCPrepare() {
        _preGCLockWord = InflatedMonitorLockWord64.boundFromMonitor(this);
    }

    @Override
    public final boolean requiresPostGCRefresh() {
        return isBound() && ObjectAccess.readMisc(_boundObject).equals(_preGCLockWord);
    }

    @Override
    public final void refreshBoundObject() {
        ObjectAccess.writeMisc(_boundObject, InflatedMonitorLockWord64.boundFromMonitor(this));
    }

    @Override
    public final BindingProtection bindingProtection() {
        return _bindingProtection;
    }

    @Override
    public final void setBindingProtection(BindingProtection deflationState) {
        _bindingProtection = deflationState;
    }

    @INLINE
    @Override
    public final ManagedMonitor next() {
        return _next;
    }

    @INLINE
    @Override
    public final void setNext(ManagedMonitor next) {
        _next = next;
    }

    public void dump() {
        Debug.print(ObjectAccess.readClassActor(this).name().string());
        Debug.print(" boundTo=");
        Debug.print(boundObject() == null ? "null" : ObjectAccess.readClassActor(boundObject()).name().string());
        Debug.print(" owner=");
        Debug.print(_ownerThread == null ? "null" : _ownerThread.getName());
        Debug.print(" recursion=");
        Debug.print(_recursionCount);
        Debug.print(" binding=");
        Debug.print(_bindingProtection.name());
    }

}
