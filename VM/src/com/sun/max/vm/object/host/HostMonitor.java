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
package com.sun.max.vm.object.host;

import static com.sun.max.unsafe.WithoutAccessCheck.*;

import java.util.*;
import java.util.concurrent.locks.*;

import com.sun.max.annotate.*;

@HOSTED_ONLY
public final class HostMonitor {

    private HostMonitor() {
    }

    private static final Map<Object, Monitor> monitorMap = new IdentityHashMap<Object, Monitor>();

    private static class Monitor extends ReentrantLock {
        @Override
        public boolean isHeldByCurrentThread() {
            // Thank you java.util.concurrent; we override this method just to get access!
            return super.isHeldByCurrentThread();
        }
    }

    private static Monitor getMonitor(Object object) {
        Monitor lock = monitorMap.get(object);
        if (lock == null) {
            lock = new Monitor();
            monitorMap.put(object, lock);
        }
        return lock;
    }

    private static Monitor checkOwner(Monitor monitor) {
        if (!monitor.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        return monitor;
    }

    public static void enter(Object object) {
        if (unsafe != null) {
            unsafe.monitorEnter(object);
        } else {
            getMonitor(object).lock();
        }
    }

    public static void exit(Object object) {
        if (unsafe != null) {
            unsafe.monitorExit(object);
        } else {
            checkOwner(getMonitor(object)).unlock();
        }
    }

    public static void wait(Object object, long timeout) throws InterruptedException {
        if (unsafe != null) {
            object.wait(timeout);
        } else {
            final Monitor monitor = checkOwner(getMonitor(object));
            final int depth = monitor.getHoldCount();
            synchronized (monitor) {
                unlock(monitor, depth);
                // reuse the java condition variable to implement our condition variable
                monitor.wait(timeout);
            }
            relock(monitor, depth);
        }
    }

    public static void notify(Object object) {
        if (unsafe != null) {
            object.notify();
        } else {
            final Monitor monitor = checkOwner(getMonitor(object));
            final int depth = monitor.getHoldCount();
            synchronized (monitor) {
                unlock(monitor, depth);
                monitor.notify();
            }
            relock(monitor, depth);
        }
    }

    public static void notifyAll(Object object) {
        if (unsafe != null) {
            object.notifyAll();
        } else {
            final Monitor monitor = checkOwner(getMonitor(object));
            final int depth = monitor.getHoldCount();
            synchronized (monitor) {
                unlock(monitor, depth);
                monitor.notifyAll();
            }
            relock(monitor, depth);
        }
    }

    private static void relock(final Monitor monitor, final int depth) {
        for (int i = 0; i < depth; i++) {
            monitor.lock();
        }
    }

    private static void unlock(final Monitor monitor, final int depth) {
        for (int i = 0; i < depth; i++) {
            monitor.unlock();
        }
    }

}
