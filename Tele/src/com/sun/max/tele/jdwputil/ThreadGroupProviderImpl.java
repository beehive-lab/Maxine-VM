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
package com.sun.max.tele.jdwputil;

import java.util.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * Represents a thread group used for logical grouping in the JDWP
 * protocol. Currently we only distinguish between Java and native threads.
 *
 * @author Thomas Wuerthinger
 * @author Michael Van De Vanter
 */
public class ThreadGroupProviderImpl implements ThreadGroupProvider {

    private final TeleVM teleVM;
    private final boolean containsJavaThreads;

    public ThreadGroupProviderImpl(TeleVM teleVM, boolean b) {
        this.teleVM = teleVM;
        this.containsJavaThreads = b;
    }

    public String getName() {
        return containsJavaThreads ? "Java Threads" : "Native Threads";
    }

    public ThreadGroupProvider getParent() {
        return null;
    }

    public ThreadProvider[] getThreadChildren() {
        final List<ThreadProvider> result = new LinkedList<ThreadProvider>();
        for (TeleNativeThread t : teleVM.teleProcess().threads()) {
            if (t.isJava() == containsJavaThreads) {
                result.add(t);
            }
        }
        return result.toArray(new ThreadProvider[result.size()]);
    }

    public ThreadGroupProvider[] getThreadGroupChildren() {
        return new ThreadGroupProvider[0];
    }

    public ReferenceTypeProvider getReferenceType() {
        assert false : "No reference type for thread groups available!";
        return null;
    }

}
