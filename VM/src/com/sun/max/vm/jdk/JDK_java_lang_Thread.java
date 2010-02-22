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
import com.sun.max.platform.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Method substitutions for {@link java.lang.Thread java.lang.Thread}.
 *
 */
@METHOD_SUBSTITUTIONS(Thread.class)
public final class JDK_java_lang_Thread {

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
     *
     * @return the corresponding VM thread for this {@code java.lang.Thread}. This value will be {@code null} for a
     *         thread that hasn't been started.
     */
    @INLINE
    private VmThread thisVMThread() {
        return VmThread.fromJava(thisThread());
    }

    public static Thread createThreadForAttach(VmThread vmThread, String name, ThreadGroup group, boolean daemon) throws Throwable {
        FatalError.check(group != null, "ThreadGroup for attaching thread cannot be null");

        final Thread javaThread = (Thread) Heap.createTuple(ClassRegistry.THREAD.dynamicHub());
        TupleAccess.writeObject(javaThread, Thread_vmThread.offset(), vmThread);
        TupleAccess.writeInt(javaThread, ClassRegistry.Thread_priority.offset(), Thread.NORM_PRIORITY);
        vmThread.setJavaThread(javaThread, name);
        ReferenceValue threadValue = ReferenceValue.from(javaThread);
        ReferenceValue groupValue = ReferenceValue.from(group);
        ReferenceValue targetValue = ReferenceValue.NULL;
        ReferenceValue nameValue = ReferenceValue.from(name == null ? (String) ClassRegistry.Thread_nextThreadNum.invoke().asObject() : name);
        LongValue stackSizeValue = LongValue.ZERO;
        if (Platform.target().operatingSystem == OperatingSystem.DARWIN) {
            // The Thread.init() method on Apple takes an extra boolean parameter named 'set_priority'
            // which indicates if the priority should be explicitly set. For all calls to init() this
            // argument is true *except* for a call for the purpose of attaching a thread when it is false.
            ClassRegistry.Thread_init.invoke(threadValue, groupValue, targetValue, nameValue, stackSizeValue, BooleanValue.FALSE);
        } else {
            ClassRegistry.Thread_init.invoke(threadValue, groupValue, targetValue, nameValue, stackSizeValue);
        }

        if (daemon) {
            javaThread.setDaemon(true);
        }

        ClassRegistry.ThreadGroup_add_Thread.invoke(groupValue, threadValue);
        return javaThread;
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
        final VmThread vmThread = VmThreadFactory.create(thisThread());
        TupleAccess.writeObject(thisThread(), Thread_vmThread.offset(), vmThread);
        vmThread.setPriority0(thisThread().getPriority());
        vmThread.start0();
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
        VmThread vmThread = thisVMThread();
        return vmThread == null ? false : vmThread.isInterrupted(clearInterrupted);
    }

    /**
     * Checks whether this thread is still alive (i.e. has not been terminated).
     * @see java.lang.Thread#isAlive()
     * @return {@code true} if this thread is still alive; {@code false} otherwise
     */
    @SUBSTITUTE
    public boolean isAlive() {
        VmThread vmThread = thisVMThread();
        return vmThread != null && vmThread.state() != Thread.State.NEW && vmThread.state() != Thread.State.TERMINATED;
    }

    /**
     * Counts the number of stack frames on this thread's stack.
     * @deprecated The definition of this call depends on {@link #suspend},
     *         which is deprecated.  Further, the results of this call
     *         were never well-defined.
     * @see java.lang.Thread#countStackFrames()
     * @return 0
     */
    @SUBSTITUTE
    @Deprecated
    public int countStackFrames() {
        return 0;
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
        return VmThreadMap.dumpThreads(threads);
    }

    /**
     * Gets all threads in the system.
     * @see java.lang.Thread#getThreads()
     * @return an array of all the threads in the system
     */
    @SUBSTITUTE
    private static Thread[] getThreads() {
        return VmThreadMap.getThreads();
    }

    /**
     * Set the priority of this thread at the OS level.
     * @see java.lang.Thread#setPriority0(int)
     * @param newPriority the new priority
     */
    @SUBSTITUTE
    private void setPriority0(int newPriority) {
        VmThread vmThread = thisVMThread();
        if (vmThread != null) {
            vmThread.setPriority0(newPriority);
        }
    }

    /**
     * Stops this thread at the OS level.
     * @see java.lang.Thread#stop0(Object)
     * @param throwable the throwable to throw in the target thread
     */
    @SUBSTITUTE
    private void stop0(Object throwable) {
        VmThread vmThread = thisVMThread();
        if (vmThread != null) {
            vmThread.stop0(throwable);
        }
    }

    /**
     * Suspends this thread at the OS-level.
     * @see java.lang.Thread#suspend0()
     */
    @SUBSTITUTE
    private void suspend0() {
        VmThread vmThread = thisVMThread();
        if (vmThread != null) {
            vmThread.suspend0();
        }
    }

    /**
     * Resumes this thread at the OS level.
     * @see java.lang.Thread#resume0()
     */
    @SUBSTITUTE
    private void resume0() {
        VmThread vmThread = thisVMThread();
        if (vmThread != null) {
            vmThread.resume0();
        }
    }

    /**
     * Interrupts this thread at the OS level.
     * @see java.lang.Thread#interrupt0()
     */
    @SUBSTITUTE
    private void interrupt0() {
        VmThread vmThread = thisVMThread();
        if (vmThread != null) {
            vmThread.interrupt0();
        }
    }

    /**
     * Gets the state of this thread.
     * @see java.lang.Thread#getState();
     * @return the state of this thread
     */
    @SUBSTITUTE
    private Thread.State getState() {
        VmThread vmThread = thisVMThread();
        return vmThread == null ? Thread.State.NEW : vmThread.state();
    }

    /**
     * Sets the name of the the thread, also updating the name in the corresponding VmThread.
     * @param name new name for thread
     */
    @SUBSTITUTE
    private void setName(String name) {
        thisThread().checkAccess();
        if (thisVMThread() != null) {
            thisVMThread().setName(name);
        }
        TupleAccess.writeObject(this, ClassRegistry.Thread_name.offset(), name.toCharArray());
    }
}
