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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;

/**
 * Method substitutions for java.lang.Object.
 */
@METHOD_SUBSTITUTIONS(Object.class)
final class JDK_java_lang_Object {

    private JDK_java_lang_Object() {
    }

    /**
     * Register native methods. None in this implementation.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Gets the class (exact dynamic type) of this class.
     * @see java.lang.Object#getClass()
     * @return the class of this object
     */
    @SUBSTITUTE("getClass")
    @INLINE
    public Class getClass_SUBSTITUTE() {
        return ObjectAccess.readClassActor(this).javaClass();
    }

    /**
     * Computes the hashcode of this object.
     * @see java.lang.Object#hashCode()
     * @return an integer representing a default hashcode for this object
     */
    @SUBSTITUTE
    @Override
    public int hashCode() {
        return ObjectAccess.makeHashCode(this);
    }

    /**
     * Clone this object, if the operation is supported.
     * @see java.lang.Object#clone()
     * @return a new instance of this object, if this operation is supported
     * @throws CloneNotSupportedException if this object does not implement the
     * {@link java.lang.Cloneable Cloneable} interface
     */
    @SUBSTITUTE
    @Override
    protected Object clone() throws CloneNotSupportedException {
        if (Cloneable.class.isInstance(this)) {
            return Heap.clone(this);
        }
        throw new CloneNotSupportedException();
    }

    /**
     * Notify one thread waiting on this object's condition variable.
     * @see java.lang.Object#notify()
     */
    @SUBSTITUTE("notify")
    public void notify_SUBSTITUTE() {
        vm().config.monitorScheme().monitorNotify(this);
    }

    /**
     * Notify all threads waiting on this object's condition variable.
     * @see java.lang.Object#notifyAll()
     */
    @SUBSTITUTE("notifyAll")
    public void notifyAll_SUBSTITUTE() {
        vm().config.monitorScheme().monitorNotifyAll(this);
    }

    /**
     * Wait for this object's condition variable.
     * @see java.lang.Object.wait()
     * @param timeout the maximum number of milliseconds to wait; with {@code 0}
     * indicating an indefinite wait
     * @throws InterruptedException if this thread is interrupted during the wait
     */
    @SUBSTITUTE("wait")
    public void wait_SUBSTITUTE(long timeout) throws InterruptedException {
        vm().config.monitorScheme().monitorWait(this, timeout);
    }

}
