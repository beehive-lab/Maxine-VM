/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor;

/*
 * This is essentially equivalent to set of proxy classes that implements all the DependencyVisitor types
 * in the VM. Since the VM is a closed world, it is more efficient to do it this way.
 */

// START GENERATED CODE
import com.sun.max.vm.compiler.deps.ConcreteMethodDependencyProcessor.*;
import com.sun.max.vm.compiler.deps.ConcreteTypeDependencyProcessor.*;
import com.sun.max.vm.compiler.deps.InlinedMethodDependencyProcessor.*;
import com.sun.max.vm.jvmti.JVMTI_DependencyProcessor;
import com.sun.max.vm.jvmti.JVMTI_DependencyProcessor.*;

class AllToStringDependencyVisitor extends DependencyVisitor implements
        ConcreteMethodDependencyProcessorVisitor,
        ConcreteTypeDependencyProcessorVisitor,
        InlinedMethodDependencyProcessorVisitor,
        JVMTI_DependencyProcessorVisitor {

    AllToStringDependencyVisitor(StringBuilder sb) {
        ConcreteMethodDependencyProcessor.toStringConcreteMethodDependencyProcessorVisitor.setStringBuilder(sb);
        ConcreteTypeDependencyProcessor.toStringConcreteTypeDependencyProcessorVisitor.setStringBuilder(sb);
        InlinedMethodDependencyProcessor.toStringInlinedMethodDependencyProcessorVisitor.setStringBuilder(sb);
        JVMTI_DependencyProcessor.toStringJVMTI_DependencyProcessorVisitor.setStringBuilder(sb);
    }

    public boolean doConcreteMethod(TargetMethod arg0, MethodActor arg1, MethodActor arg2, ClassActor arg3) {
        return ConcreteMethodDependencyProcessor.toStringConcreteMethodDependencyProcessorVisitor.doConcreteMethod(arg0, arg1, arg2, arg3);
    }
    public boolean doConcreteSubtype(TargetMethod arg0, ClassActor arg1, ClassActor arg2) {
        return ConcreteTypeDependencyProcessor.toStringConcreteTypeDependencyProcessorVisitor.doConcreteSubtype(arg0, arg1, arg2);
    }
    public boolean doInlinedMethod(TargetMethod arg0, ClassMethodActor arg1, ClassMethodActor arg2, ClassActor arg3) {
        return InlinedMethodDependencyProcessor.toStringInlinedMethodDependencyProcessorVisitor.doInlinedMethod(arg0, arg1, arg2, arg3);
    }
    public boolean doCheckSettings(TargetMethod arg0, long arg1, long[] arg2) {
        return JVMTI_DependencyProcessor.toStringJVMTI_DependencyProcessorVisitor.doCheckSettings(arg0, arg1, arg2);
    }
}
// END GENERATED CODE

