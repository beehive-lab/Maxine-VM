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

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;

/**
 * Method substitutions for java.lang.Object.
 *
 * @author Bernd Mathiske
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
        return ObjectAccess.readClassActor(this).mirror();
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
        MaxineVM.target().configuration.monitorScheme().monitorNotify(this);
    }

    /**
     * Notify all threads waiting on this object's condition variable.
     * @see java.lang.Object#notifyAll()
     */
    @SUBSTITUTE("notifyAll")
    public void notifyAll_SUBSTITUTE() {
        MaxineVM.target().configuration.monitorScheme().monitorNotifyAll(this);
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
        MaxineVM.target().configuration.monitorScheme().monitorWait(this, timeout);
    }

}
