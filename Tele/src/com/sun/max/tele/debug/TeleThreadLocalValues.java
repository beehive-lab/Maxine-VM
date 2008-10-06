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
/*VCSID=c9ceefab-5556-451c-abbd-966b6f4ad487*/
package com.sun.max.tele.debug;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.thread.*;

/**
 * The values of the {@linkplain VmThreadLocal thread local variables} for a {@linkplain TeleNativeThread thread}.
 *
 * @author Doug Simon
 */
public class TeleThreadLocalValues extends EnumMap<VmThreadLocal, Long> {

    public TeleThreadLocalValues() {
        super(VmThreadLocal.class);
        final Long zero = Long.valueOf(0L);
        for (VmThreadLocal threadLocalVariable : VmThreadLocal.VALUES) {
            put(threadLocalVariable, zero);
        }
    }

    /**
     * Gets the value of a given thread local variable as a word.
     */
    public Word getWord(VmThreadLocal threadLocalVariable) {
        return Address.fromLong(get(threadLocalVariable));
    }

    public boolean isInJavaCode() {
        return get(LAST_JAVA_CALLER_INSTRUCTION_POINTER) == 0 && get(LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C) == 0;
    }
}
