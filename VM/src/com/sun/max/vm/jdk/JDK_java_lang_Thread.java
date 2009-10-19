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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.actor.member.InjectedReferenceFieldActor.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * Method substitutions for {@link java.lang.Thread java.lang.Thread}.
 *
 */
@METHOD_SUBSTITUTIONS(Thread.class)
final class JDK_java_lang_Thread {

    private JDK_java_lang_Thread() {
    }

    /**
     * Register native methods, which there are none in this implementation.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Cast this object reference to the {@code java.lang.Thread} object it represents.
     * @return this object casted to {@code java.lang.Thread}
     */
    @UNSAFE_CAST
    private native Thread thisThread();

    /**
     * Get the VM thread for this thread.
     * @return the corresponding VM thread for this {@code java.lang.Thread}
     */
    @INLINE
    private VmThread thisVMThread() {
        return VmThread.fromJava(thisThread());
    }

    /**
     * Access checks required by the security manager. Implementation note: this method
     * assigns the injected field from {@code java.lang.Thread} to {@code com.sun.max.vm.thread.VmThread}.
     * @see java.lang.Thread#checkAccess()
     */
    @SUBSTITUTE
    public void checkAccess() {
        final Thread thread = thisThread();

        // A copy of the original content of 'checkAccess()':
        final SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(thread);
        }

        // Our own additions:
        if (VmThread.fromJava(thread) == null) {
            // Aha, we must have gotten here during the constructor, otherwise the injected field would have been set already
            final VmThread vmThread = VmThreadFactory.create(thread);
            TupleAccess.writeObject(thread, Thread_vmThread.offset(), vmThread);
        }
    }

    /**
     * Retrieve the current thread.
     * @see java.lang.Thread#currentThread()
     * @return the current thread
     */
    @SUBSTITUTE
    public static Thread currentThread() {
        return VmThread.current().javaThread();
    }

    /**
     * Yield execution to other threads, if possible.
     * @see java.lang.Thread#yield()
     */
    @SUBSTITUTE
    public static void yield() {
        VmThread.yield();
    }

    /**
     * Sleep for the specified amount of time.
     * @see java.lang.Thread#sleep(long)
     * @param millis the number of milliseconds to sleep
     */
    @SUBSTITUTE
    public static void sleep(long millis) throws InterruptedException {
        VmThread.sleep(millis);
    }

    /**
     * Starts the thread running.
     * @see java.lang.Thread#start()
     */
    @SUBSTITUTE
    private void start0() {
        thisVMThread().start0();
    }

    /**
     * Checks whether this thread has been interrupted.
     * @see java.lang.Thread#isInterrupted()
     * @param clearInterrupted {@code true} if the interrupted status of this thread should be cleared by
     * this operation; {@code false} otherwise
     * @return the interrupted status of another thread
     */
    @SUBSTITUTE
    private boolean isInterrupted(boolean clearInterrupted) throws InterruptedException {
        return thisVMThread().isInterrupted(clearInterrupted);
    }

    /**
     * Checks whether this thread is still alive (i.e. has not been terminated).
     * @see java.lang.Thread#isAlive()
     * @return {@code true} if this thread is still alive; {@code false} otherwise
     */
    @SUBSTITUTE
    public boolean isAlive() {
        return thisVMThread().state() != Thread.State.NEW && thisVMThread().state() != Thread.State.TERMINATED;
    }

    /**
     * Counts the number of stack frames on this thread's stack.
     * @see java.lang.Thread#countStackFrames()
     * @return the number of stack frames on this thread's stack
     */
    @SUBSTITUTE
    public int countStackFrames() {
        return thisVMThread().countStackFrames();
    }

    /**
     * Checks whether this thread holds the monitor of the specified object.
     * @see java.lang.Thread#holdsLock(Object)
     * @param object the object which to check
     * @return {@code true} if this thread currently owns the monitor of the specified object; {@code false}
     * otherwise
     */
    @SUBSTITUTE
    public static boolean holdsLock(Object object) {
        return Monitor.threadHoldsMonitor(object, VmThread.current());
    }

    /**
     * Dump the stacks of the specified threads.
     * @see java.lang.Thread#dumpThreads()
     * @param threads the threads to dump
     * @return an array of {@link java.lang.StackTraceElement java.lang.StackTraceElement}'s for each
     * thread's stack
     */
    @SUBSTITUTE
    private static StackTraceElement[][] dumpThreads(Thread[] threads) {
        return VmThread.dumpThreads(threads);
    }

    /**
     * Gets all threads in the system.
     * @see java.lang.Thread#getThreads()
     * @return an array of all the threads in the system
     */
    @SUBSTITUTE
    private static Thread[] getThreads() {
        return VmThread.getThreads();
    }

    /**
     * Set the priority of this thread at the OS level.
     * @see java.lang.Thread#setPriority0(int)
     * @param newPriority the new priority
     */
    @SUBSTITUTE
    private void setPriority0(int newPriority) {
        thisVMThread().setPriority0(newPriority);
    }

    /**
     * Stops this thread at the OS level.
     * @see java.lang.Thread#stop0(Object)
     * @param throwable the throwable to throw in the target thread
     */
    @SUBSTITUTE
    private void stop0(Object throwable) {
        thisVMThread().stop0(throwable);
    }

    /**
     * Suspends this thread at the OS-level.
     * @see java.lang.Thread#suspend0()
     */
    @SUBSTITUTE
    private void suspend0() {
        thisVMThread().suspend0();
    }

    /**
     * Resumts this thread at the OS level.
     * @see java.lang.Thread#resume0()
     */
    @SUBSTITUTE
    private void resume0() {
        thisVMThread().resume0();
    }

    /**
     * Interrupts this thread at the OS level.
     * @see java.lang.Thread#interrupt0()
     */
    @SUBSTITUTE
    private void interrupt0() {
        thisVMThread().interrupt0();
    }

    /**
     * Gets the state of this thread.
     * @see java.lang.Thread#getState();
     * @return the state of this thread
     */
    @SUBSTITUTE
    private Thread.State getState() {
        return thisVMThread().state();
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static FieldActor nameFieldActor;

    @INLINE
    private static FieldActor nameFieldActor() {
        if (nameFieldActor == null) {
            nameFieldActor = ClassActor.fromJava(Thread.class).findFieldActor(SymbolTable.makeSymbol("name"));
        }
        return nameFieldActor;
    }

    /**
     * Sets the name of the the thread, also updating the name in the corresponding VmThread.
     * @param name new name for thread
     */
    @SUBSTITUTE
    private void setName(String name) {
        checkAccess();
        thisVMThread().setName(name);
        TupleAccess.writeObject(this, nameFieldActor().offset(), name.toCharArray());
    }

}
