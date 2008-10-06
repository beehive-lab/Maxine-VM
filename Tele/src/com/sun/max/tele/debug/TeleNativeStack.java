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
/*VCSID=14f39362-ee39-4e92-88a5-ca0b2369de18*/
package com.sun.max.tele.debug;

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Encapsulates a snapshot of the frames on a stack.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Aritra Bandyopadhyay
 */
public class TeleNativeStack extends FixedMemoryRegion {

    public TeleNativeStack(Address base, Size size) {
        super(base, size);
    }

    /**
     * Refreshes the values of the {@linkplain #threadLocalValues() thread local variables} of this stack.
     *
     * @param thread the thread that owns this stack
     */
    void refresh(TeleNativeThread thread) {
        if (thread != null) {
            _threadLocalValues = scanStackForVMThreadReference(thread, start(), size());
        } else {
            _threadLocalValues = null;
        }
    }

    /**
     * @return the values of the {@linkplain VmThreadLocal thread local variables} on this stack it this stack is
     *         associated with a {@link VmThread}, null otherwise
     */
    public TeleThreadLocalValues threadLocalValues() {
        return _threadLocalValues;
    }

    private TeleThreadLocalValues _threadLocalValues;

    /**
     * Scans the slots of this stack looking for the thread local variables indicating if the stack is associated with a
     * {@linkplain VmThread VM thread}.
     *
     * @param thread the thread whose stack base is at {@code base} and whose size is {@code size}
     * @return the values of the thread local variables for {@code thread} if the stack is determined to be that of a {@link VmThread}, null otherwise
     */
    private static TeleThreadLocalValues scanStackForVMThreadReference(TeleNativeThread thread, Address base, Size size) {
        if (base.isZero() || size.isZero()) {
            // unknown stack base or size => not a Java thread
            return null;
        }
        final TeleVM teleVM = thread.teleProcess().teleVM();
        final DataAccess dataAccess = teleVM.teleProcess().dataAccess();
        final int pageSize = VMConfiguration.hostOrTarget().platform().pageSize();
        final Pointer stop = base.plus(Math.min(pageSize, size.toInt())).asPointer();
        for (Pointer pointer = base.asPointer(); pointer.lessEqual(stop); pointer = pointer.plusWords(1)) {
            try {
                final Word value = dataAccess.readWord(pointer);
                if (value.equals(VmThread.TAG)) {
                    final Pointer vmThreadLocals = pointer.minusWords(VmThreadLocal.TAG.index());
                    final Word normalVmThreadLocals = dataAccess.readWord(vmThreadLocals, VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.offset());
                    if (normalVmThreadLocals.equals(vmThreadLocals)) {
                        final Word vmThreadWord = dataAccess.readWord(vmThreadLocals, VmThreadLocal.VM_THREAD.offset());
                        if (!vmThreadWord.isZero()) {
                            final Reference vmThreadReference = teleVM.wordToReference(vmThreadWord);
                            try {
                                if (VmThread.class.isAssignableFrom(teleVM.makeClassActorForTypeOf(vmThreadReference).toJava())) {
                                    final TeleThreadLocalValues threadLocalValues = new TeleThreadLocalValues();
                                    for (VmThreadLocal threadLocalVariable : VmThreadLocal.VALUES) {
                                        final Word threadLocalValue = dataAccess.readWord(vmThreadLocals, threadLocalVariable.offset());
                                        threadLocalValues.put(threadLocalVariable, threadLocalValue.asAddress().toLong());
                                    }
                                    return threadLocalValues;
                                }
                            } catch (TeleError teleError) {
                                return null;
                            }
                        }
                    }
                }
            } catch (DataIOError e) {
                return null;
            }
        }
        return null;
    }
}
