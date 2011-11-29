/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.deps;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAssumptions.Assumption;
import com.sun.cri.ci.CiAssumptions.ConcreteMethod;
import com.sun.cri.ci.CiAssumptions.ConcreteSubtype;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deps.DependenciesManager.UniqueConcreteMethodSearch;

/**
 * Validates dependencies for a compiled method.
 */
class DependencyValidator extends CiAssumptionProcessor {

    /**
     * Maps of context types to dependencies involving them.
     */
    HashMap<ClassActor, ArrayList<Assumption>> dependencies = new HashMap<ClassActor, ArrayList<Assumption>>(10);

    /**
     * The number of unique concrete method dependencies where the concrete method is the context method.
     */
    private int localUCMs = 0;

    /**
     * The number of unique concrete method dependencies where the concrete method is not the context method.
     */
    private int nonLocalUCMs = 0;

    /**
     * Lazily created helper for validating unique concrete methods.
     */
    private UniqueConcreteMethodSearch ucms;

    private boolean isUniqueConcreteMethod(MethodActor context, MethodActor method) {
        if (ucms == null) {
            ucms = new UniqueConcreteMethodSearch();
        }
        return ucms.doIt(context.holder(), method) == method;
    }

    Dependencies result() {
        if (dependencies != null) {
            return new Dependencies(dependencies, localUCMs, nonLocalUCMs);
        }
        return Dependencies.INVALID;
    }

    @Override
    public boolean doConcreteSubtype(ConcreteSubtype cs) {
        final ClassActor context = (ClassActor) cs.context;
        final ClassActor subtype = (ClassActor) cs.subtype;
        if (context.uniqueConcreteType == subtype.id) {
            make((ClassActor) cs.context).add(cs);
            return true;
        }
        // Drop whatever was built so far.
        dependencies = null;
        return false;
    }

    @Override
    public boolean doConcreteMethod(ConcreteMethod cm) {
        if (!isUniqueConcreteMethod((MethodActor) cm.context, (MethodActor) cm.method)) {
            // Drop whatever was built so far.
            dependencies = null;
            return false;
        }
        final ClassActor contextHolder = (ClassActor) cm.context.holder();

        make(contextHolder).add(cm);
        if (cm.method == cm.context) {
            localUCMs++;
        } else {
            nonLocalUCMs++;
        }
        return true;
    }

    private ArrayList<Assumption> make(ClassActor type) {
        ArrayList<Assumption> list = dependencies.get(type);
        if (list == null) {
            list = new ArrayList<Assumption>(4);
            dependencies.put(type, list);
        }
        return list;
    }
}
