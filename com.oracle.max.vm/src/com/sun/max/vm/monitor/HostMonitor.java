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
package com.sun.max.vm.monitor;

import static com.sun.max.vm.hosted.WithoutAccessCheck.*;

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
