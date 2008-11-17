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
package com.sun.max.vm.thread;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * The VmThreadMap class contains all the active threads in the MaxineVM.
 *
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 */
public final class VmThreadMap {

    /**
     * The global thread map of active threads in the VM.
     */
    public static final VmThreadMap ACTIVE = new VmThreadMap();

    private final IDMap _idMap = new IDMap(64);
    private Pointer _vmThreadLocalsListHead = Pointer.zero();
    private volatile int _vmThreadStartCount;

    private VmThreadMap() {
    }

    /**
     * Adds the specified thread locals to this thread map and initializes several of its
     * important values (such as its ID and VM thread reference).
     *
     * Note that this method does not perform synchronization on the thread map, because it must
     * only be executed in a newly created thread while the creating thread holds the lock on
     * this thread map.
     *
     * @param id the ID of the VM thread, which should match the ID of the VmThread
     * @param vmThreadLocals a pointer to the VM thread locals for the thread
     * @return a reference to the VmThread for this thread
     */
    public VmThread addVmThreadLocals(int id, Pointer vmThreadLocals) {
        final VmThread vmThread = _idMap.get(id);
        VmThreadLocal.ID.setConstantWord(vmThreadLocals, Address.fromInt(id));
        VmThreadLocal.VM_THREAD.setConstantReference(vmThreadLocals, Reference.fromJava(vmThread));
        // insert this thread locals into the list
        setNext(vmThreadLocals, _vmThreadLocalsListHead);
        setPrev(_vmThreadLocalsListHead, vmThreadLocals);
        // at the head
        _vmThreadLocalsListHead = vmThreadLocals;
        // and signal that this thread has started up and joined the list
        _vmThreadStartCount++;
        return vmThread;
    }

    /**
     * Remove the specified VM thread locals from this thread map.
     * @param vmThreadLocals the thread locals to remove from this map
     */
    public void removeVmThreadLocals(Pointer vmThreadLocals) {
        synchronized (this) {
            final int id = VmThreadLocal.ID.getConstantWord(vmThreadLocals).asAddress().toInt();
            if (_vmThreadLocalsListHead == vmThreadLocals) {
                // this vm thread locals is at the head of list
                _vmThreadLocalsListHead = getNext(_vmThreadLocalsListHead);
            } else {
                // this vm thread locals is somewhere in the middle
                final Pointer prev = getPrev(vmThreadLocals);
                final Pointer next = getNext(vmThreadLocals);
                setPrev(next, prev);
                setNext(prev, next);
            }
            // set this vm thread locals' links to zero
            setPrev(vmThreadLocals, Pointer.zero());
            setNext(vmThreadLocals, Pointer.zero());
            // release the ID for a later thread's use
            _idMap.release(id);
        }
    }

    /**
     * Add the main thread to this thread map.
     *
     * @param vmThread the vmThread representing the main thread
     */
    public void addMainVmThread(VmThread vmThread) {
        vmThread.setThreadMapID(_idMap.acquire(vmThread));
    }

    // Helper routines to manipulate the linked list of vm thread locals

    @INLINE
    private Pointer getPrev(Pointer vmThreadLocals) {
        return VmThreadLocal.BACKWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
    }

    @INLINE
    private Pointer getNext(Pointer vmThreadLocals) {
        return VmThreadLocal.FORWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
    }

    @INLINE
    private void setPrev(Pointer vmThreadLocals, Pointer prev) {
        if (!vmThreadLocals.isZero()) {
            VmThreadLocal.BACKWARD_LINK.setConstantWord(vmThreadLocals, prev);
        }
    }

    @INLINE
    private void setNext(Pointer vmThreadLocals, Pointer next) {
        if (!vmThreadLocals.isZero()) {
            VmThreadLocal.FORWARD_LINK.setConstantWord(vmThreadLocals, next);
        }
    }

    /**
     * Creates the native thread for a VM thread and start it running. This method acquires an ID
     * for the new thread and returns it to the caller.
     *
     * @param vmThread the VM thread to create
     * @param stackSize the requested stack size
     * @param priority the initial priority of the thread
     * @return the native thread created
     */
    public Word startVmThread(VmThread vmThread, Size stackSize, int priority) {
        final int id = _idMap.acquire(vmThread);
        synchronized (this) {
            final int count = _vmThreadStartCount;
            final Word nativeThread = VmThread.nativeThreadCreate(id, stackSize, priority);
            if (nativeThread.isZero()) {
                vmThread.beTerminated();
                FatalError.unexpected("nativeThreadCreate() failed");
            }
            if (!waitForThreadStartup(count)) {
                vmThread.beTerminated();
                FatalError.unexpected("waitForThreadStartup() failed");
            }
            return nativeThread;
        }
    }

    /**
     * Waits for all non-daemon threads in the thread map to finish.
     */
    public void joinAllNonDaemons() {
        while (true) {
            final VmThread vmThread = findNonDaemon();
            if (vmThread == null) {
                return;
            }
            vmThread.join();
        }
    }

    private VmThread findNonDaemon() {
        Pointer vmThreadLocals = _vmThreadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            final VmThread vmThread = UnsafeLoophole.cast(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals).toJava());
            if (!vmThread.javaThread().isDaemon()) {
                return vmThread;
            }
            vmThreadLocals = VmThreadLocal.FORWARD_LINK.getConstantWord(vmThreadLocals).asPointer();
        }
        return null;
    }

    private boolean waitForThreadStartup(int count) {
        int spin = 10000;
        while (_vmThreadStartCount == count) {
            // spin for a little while, waiting for other thread to start
            if (spin-- == 0) {
                spin = 100;
                while (_vmThreadStartCount == count) {
                    // wait for 100ms, 1ms at a time
                    if (spin-- == 0) {
                        return false;
                    }
                    VmThread.nonJniSleep(1);
                }
            }
        }
        return true;
    }

    /**
     * Iterates over all the VM threads in this thread map and run the specified procedure.
     *
     * @param predicate a predicate to apply on each thread
     * @param procedure the procedure to apply to each VM thread
     */
    public void forAllVmThreads(Predicate<VmThread> predicate, Procedure<VmThread> procedure) {
        Pointer vmThreadLocals = _vmThreadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            final VmThread vmThread = UnsafeLoophole.cast(VmThreadLocal.VM_THREAD.getConstantReference(vmThreadLocals).toJava());
            if (predicate == null || predicate.evaluate(vmThread)) {
                procedure.run(vmThread);
            }
            vmThreadLocals = getNext(vmThreadLocals);
        }
    }

    /**
     * Iterates over all the VM thread locals in this thread map and run the specified procedure.
     *
     * @param predicate a predicate to check on the VM thread locals
     * @param procedure the procedure to apply to each VM thread locals
     */
    public void forAllVmThreadLocals(Pointer.Predicate predicate, Pointer.Procedure procedure) {
        Pointer vmThreadLocals = _vmThreadLocalsListHead;
        while (!vmThreadLocals.isZero()) {
            if (predicate == null || predicate.evaluate(vmThreadLocals)) {
                procedure.run(vmThreadLocals);
            }
            vmThreadLocals = getNext(vmThreadLocals);
        }
    }

    public static final Pointer.Predicate _isNotCurrent = new Pointer.Predicate() {
        @Override
        public boolean evaluate(Pointer vmThreadLocals) {
            return vmThreadLocals != VmThread.current().vmThreadLocals();
        }
    };

    /**
     * Looks up a thread based on its serial number.
     *
     * @param serial the serial number of the thread
     * @return the VmThread with the specified serial number
     */
    public VmThread getThreadFromSerial(long serial) {
        return _idMap.getThreadFromSerial(serial);
    }

    /**
     * Gets the {@code VmThread} object associated with the specified thread id.
     *
     * @param id the thread id
     * @return a reference to the {@code VmThread} object for the specified id
     */
    @INLINE
    public VmThread getVmThreadForID(int id) {
        return _idMap.get(id);
    }

    /**
     * The {@code IDMap} class manages thread ids and a mapping between thread ids and
     * the corresponding {@code VmThread} instance.
     */
    private class IDMap {
        private int _nextID;
        private int[] _freeList;
        private VmThread[] _vmThreads;

        IDMap(int initialSize) {
            _freeList = new int[initialSize];
            _vmThreads = new VmThread[initialSize];
            for (int i = 0; i < _freeList.length; i++) {
                _freeList[i] = i + 1;
            }
        }

        synchronized int acquire(VmThread vmThread) {
            FatalError.check(vmThread.threadMapID() == 0, "VmThread already has an ID");
            final int length = _freeList.length;
            if (_nextID >= length) {
                // grow the free list and initialize the new part
                final int[] newFreeList = Arrays.grow(_freeList, length * 2);
                for (int i = length; i < newFreeList.length; i++) {
                    newFreeList[i] = i + 1;
                }
                _freeList = newFreeList;

                // grow the vmThreads list and copy
                final VmThread[] newVmThreads = new VmThread[length * 2];
                for (int i = 0; i < length; i++) {
                    newVmThreads[i] = _vmThreads[i];
                }
                _vmThreads = newVmThreads;
            }
            final int id = _nextID;
            _nextID = _freeList[_nextID];
            _vmThreads[id] = vmThread;
            vmThread.setThreadMapID(id);
            return id;
        }

        synchronized void release(int id) {
            _freeList[id] = _nextID;
            _vmThreads[id] = null;
            _nextID = id;
        }

        @INLINE
        VmThread get(int id) {
            // this operation may be performance critical, so avoid the bounds check
            return UnsafeLoophole.cast(VmThread.class, ArrayAccess.getObject(_vmThreads, id));
        }

        VmThread getThreadFromSerial(long serial) {
            for (int i = 0; i < _vmThreads.length; i++) {
                if (_vmThreads[i].serial() == serial) {
                    return _vmThreads[i];
                }
            }
            return null;
        }
    }
}
