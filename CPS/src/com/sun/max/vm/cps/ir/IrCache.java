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
package com.sun.max.vm.cps.ir;

import java.util.*;

import com.sun.max.vm.actor.member.*;

/**
 * The {@code IrCache} class implements a caching system for IR
 * nodes that may use method docks, hashmaps, or some other mechanism
 * internally. It optionally may implement a caching policy that will
 * remove/compress IR versions depending on usage patterns, space consumption,
 * etc.
 * 
 * @author Ben L. Titzer
 */
public class IrCache<Method_Type extends IrMethod> {

    protected final Map<ClassMethodActor, Method_Type> cache = new HashMap<ClassMethodActor, Method_Type>();

    public IrCache() {
    }

    public Method_Type get(ClassMethodActor key) {
        return cache.get(key);
    }

    public void set(ClassMethodActor key, Method_Type value) {
        cache.put(key, value);
    }

    public void remove(ClassMethodActor key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}
