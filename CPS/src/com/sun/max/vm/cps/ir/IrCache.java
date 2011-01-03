/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
