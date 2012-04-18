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

/*
 * This is essentially equivalent to set of proxy classes that implements all the DependencyVisitor types
 * in the VM. Since the VM is a closed world, it is more efficient to do it this way.
 */

// START GENERATED CODE
import com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor;
import com.sun.max.vm.compiler.deps.InlinedMethodDependencyProcessor.InlinedMethodDependencyProcessorVisitor;
import com.sun.max.vm.compiler.deps.ConcreteMethodDependencyProcessor.ConcreteMethodDependencyProcessorVisitor;
import com.sun.max.vm.compiler.deps.ConcreteTypeDependencyProcessor.ConcreteTypeDependencyProcessorVisitor;


class AllDependencyVisitors {

    static class ToStringDependencyVisitor extends DependencyVisitor
            implements InlinedMethodDependencyProcessorVisitor, ConcreteMethodDependencyProcessorVisitor, ConcreteTypeDependencyProcessorVisitor {

        ToStringDependencyVisitor(StringBuilder sb) {
            ConcreteTypeDependencyProcessor.toStringConcreteTypeDependencyProcessorVisitor.setStringBuilder(sb);
            ConcreteMethodDependencyProcessor.toStringConcreteDependencyProcessorVisitor.setStringBuilder(sb);
            InlinedMethodDependencyProcessor.toStringInlinedMethodDependencyProcessorVisitor.setStringBuilder(sb);
        }

        public boolean doConcreteMethod(TargetMethod targetMethod, MethodActor method, MethodActor impl, ClassActor context) {
            return ConcreteMethodDependencyProcessor.toStringConcreteDependencyProcessorVisitor.doConcreteMethod(targetMethod, method, impl, context);
        }

        public boolean doInlinedMethod(TargetMethod targetMethod, ClassMethodActor method, ClassMethodActor inlinee, ClassActor context) {
            return InlinedMethodDependencyProcessor.toStringInlinedMethodDependencyProcessorVisitor.doInlinedMethod(targetMethod, method, inlinee, context);

        }

        public boolean doConcreteSubtype(TargetMethod targetMethod, ClassActor context, ClassActor subtype) {
            return ConcreteTypeDependencyProcessor.toStringConcreteTypeDependencyProcessorVisitor.doConcreteSubtype(targetMethod, context, subtype);
        }

    }

}

