/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MemberName.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MethodHandleNatives.*;

import java.lang.invoke.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

public interface BootstrapMethod {

    MemberNameAlias resolve(ConstantPool pool, int index, NameAndTypeConstant nameAndTypeConstant);
    Object getAppendix();

    class Unresolved implements BootstrapMethod {
        private final int   bootstrapMethodRef;
        private final int[] bootstrapArgumentRefs;

        Unresolved(int bootstrapMethodRef, int[] bootstrapArgumentRefs) {
            this.bootstrapMethodRef = bootstrapMethodRef;
            this.bootstrapArgumentRefs = bootstrapArgumentRefs;
        }

        @Override
        public MemberNameAlias resolve(ConstantPool pool, int index, NameAndTypeConstant nameAndTypeConstant) {
            Trace.begin(1, "BootstrapMethod.resolve()");
            final ClassActor          holder          = pool.holder();
            final Utf8Constant        name            = nameAndTypeConstant.name();
            final SignatureDescriptor signature       = nameAndTypeConstant.signature();
            final MethodType          methodType      = signature.getMethodHandleType(pool.classLoader());
            final int                 argumentsNumber = bootstrapArgumentRefs.length;
            final Object[]            arguments       = new Object[argumentsNumber];

            for (int i = 0; i < argumentsNumber; i++) {
                final int    bootstrapArgumentIndex = bootstrapArgumentRefs[i];
                PoolConstant bootstrapArgument      = pool.constants()[bootstrapArgumentIndex];

                if (bootstrapArgument.tag() != STRING && bootstrapArgument.tag() != CLASS
                        && bootstrapArgument.tag() != INTEGER && bootstrapArgument.tag() != LONG
                        && bootstrapArgument.tag() != FLOAT && bootstrapArgument.tag() != DOUBLE
                        && bootstrapArgument.tag() != METHOD_HANDLE && bootstrapArgument.tag() != METHOD_TYPE) {
                    throw classFormatError("The bootstrap_arguments can only point to a string, class, integer, long, float, double, method handle, or method type constant");
                }

                // Resolve potentially resolvable arguments
                if (bootstrapArgument instanceof ClassConstant) {
                    arguments[i] = ((ClassConstant) bootstrapArgument).resolve(pool, bootstrapArgumentIndex).toJava();
                } else if (bootstrapArgument instanceof FieldRefConstant) {
                    arguments[i] = ((FieldRefConstant) bootstrapArgument).resolve(pool, bootstrapArgumentIndex).toJava();
                } else if (bootstrapArgument instanceof MethodRefConstant) {
                    arguments[i] = ((MethodRefConstant) bootstrapArgument).resolve(pool, bootstrapArgumentIndex).toJava();
                } else if (bootstrapArgument instanceof ResolvableConstant) {
                    arguments[i] = ((ResolvableConstant) bootstrapArgument).resolve(pool, bootstrapArgumentIndex);
                } else {
                    assert bootstrapArgument instanceof ValueConstant;
                    arguments[i] = ((ValueConstant) bootstrapArgument).value(pool, bootstrapArgumentIndex).asBoxedJavaValue();
                }
            }

            final MethodHandle bootstrapMethodHandle =
                    ((MethodHandleConstant) pool.constants()[bootstrapMethodRef]).resolve(pool, bootstrapMethodRef);

            Object[] appendices = new Object[1];
            assert appendices[0] == null;
            final MemberNameAlias memberName =
                    asMemberName(linkCallSite(holder.javaClass(), bootstrapMethodHandle, name, methodType, arguments, appendices));
            Object appendix = appendices[0];
            assert appendix instanceof CallSite || appendix instanceof MethodHandle;
            MethodType type = (appendix instanceof CallSite) ? ((CallSite) appendix).type() : ((MethodHandle) appendix).type();
            assert type.equals(methodType);

            // Update bootstrapMethods with the resolved bootstrapmethod
            holder.bootstrapMethods()[index] = new Resolved(memberName, appendix);

            Trace.line(1, "holder => " + holder.javaClass());
            Trace.line(1, "bootstrap => " + bootstrapMethodHandle);
            Trace.line(1, "methodType => " + bootstrapMethodHandle.type());
            Trace.line(1, "name => " + name);
            Trace.line(1, "type => " + signature);
            Trace.line(1, "arguments => " + arguments);
            Trace.line(1, "appendix => " + appendix);
            Trace.end(1, "BootstrapMethod.resolve()");

            return memberName;
        }

        @Override
        public Object getAppendix() {
            return null;
        }
    }

    class Resolved implements BootstrapMethod{
        private final MemberNameAlias memberName;
        private final Object appendix;

        public Resolved(MemberNameAlias memberName, Object appendix) {
            this.memberName = memberName;
            this.appendix = appendix;
        }

        @Override
        public MemberNameAlias resolve(ConstantPool pool, int index, NameAndTypeConstant nameAndTypeConstant) {
            return memberName;
        }

        @Override
        public Object getAppendix() {
            return appendix;
        }
    }
}
