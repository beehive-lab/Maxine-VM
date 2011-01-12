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
package com.sun.max.vm.cps.ir.igv;

import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.ir.*;

/**
 * Initializes the properties of a group of graphs for a specific method.
 *
 * @author Thomas Wuerthinger
 */
class InitializeGroupIrMethodVisitor implements IrMethodVisitor {

    private static final String TYPE_PROPERTY_NAME = "type";
    private static final String NAME_PROPERTY_NAME = "type";
    private static final String BIR_TYPE_PROPERTY_VALUE = "com.sun.max.vm.compiler.bir";
    private static final String BIR_NAME_PROPERTY_SUFFIX = "BIR";
    private static final String CIR_TYPE_PROPERTY_VALUE = "com.sun.max.vm.compiler.cir";
    private static final String CIR_NAME_PROPERTY_SUFFIX = "CIR";
    private static final String DIR_TYPE_PROPERTY_VALUE = "com.sun.max.vm.compiler.dir";
    private static final String DIR_NAME_PROPERTY_SUFFIX = "DIR";
    private static final String EIR_TYPE_PROPERTY_VALUE = "com.sun.max.vm.compiler.eir";
    private static final String EIR_NAME_PROPERTY_SUFFIX = "EIR";

    private final GraphWriter.Group group;

    InitializeGroupIrMethodVisitor(GraphWriter.Group group) {
        this.group = group;
    }

    private void initGroupName(GraphWriter.Group grp, IrMethod method, String suffix) {
        grp.getProperties().setProperty(NAME_PROPERTY_NAME, method.classMethodActor().format("%H.%n(%p)") + " / " + suffix);
    }

    public void visit(BirMethod method) {
        group.getProperties().setProperty(TYPE_PROPERTY_NAME, BIR_TYPE_PROPERTY_VALUE);
        initGroupName(group, method, BIR_NAME_PROPERTY_SUFFIX);
    }

    public void visit(CirMethod method) {
        group.getProperties().setProperty(TYPE_PROPERTY_NAME, CIR_TYPE_PROPERTY_VALUE);
        initGroupName(group, method, CIR_NAME_PROPERTY_SUFFIX);
    }

    public void visit(DirMethod method) {
        group.getProperties().setProperty(TYPE_PROPERTY_NAME, DIR_TYPE_PROPERTY_VALUE);
        initGroupName(group, method, DIR_NAME_PROPERTY_SUFFIX);

    }

    public void visit(EirMethod method) {
        group.getProperties().setProperty(TYPE_PROPERTY_NAME, EIR_TYPE_PROPERTY_VALUE);
        initGroupName(group, method, EIR_NAME_PROPERTY_SUFFIX);
    }
}
