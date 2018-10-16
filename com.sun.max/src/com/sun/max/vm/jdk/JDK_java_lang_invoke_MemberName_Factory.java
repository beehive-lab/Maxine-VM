/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MethodHandleNatives.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MemberName.*;

import java.lang.invoke.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

import sun.invoke.util.*;

@METHOD_SUBSTITUTIONS(className = "java.lang.invoke.MemberName$Factory")
public final class JDK_java_lang_invoke_MemberName_Factory {

    /**
     * Substituted from MemberName$Factory rather than MethodHandleNatives to avoid the additional array resizing
     * complexity.
     *
     * @param defc
     * @param matchName
     * @param matchType
     * @param matchFlags
     * @param lookupClass
     * @return
     */
    @SUBSTITUTE(value = "getMembers")
    List<Object> getMembers(Class< ? > defc, String matchName, Object matchType, int matchFlags, Class< ? > lookupClass) {
        List<Object> list = new ArrayList<>();

        Trace.line(1, "MemberName$Lookup.getMembers called");

        matchFlags &= ALL_KINDS;
        String sig = null;
        if (matchType != null) {
            sig = BytecodeDescriptor.unparse(matchType);
            if (sig.startsWith("(")) {
                matchFlags &= ~(ALL_KINDS & ~IS_INVOCABLE);
            } else {
                matchFlags &= ~(ALL_KINDS & ~IS_FIELD);
            }
        }

        boolean searchSupers = (matchFlags & SEARCH_SUPERCLASSES) != 0;
        boolean searchInterfaces = (matchFlags & SEARCH_INTERFACES) != 0;
        boolean localOnly = !(searchSupers | searchInterfaces);

        ClassActor classActor = ClassActor.fromJava(defc);

        if ((matchFlags & IS_FIELD) != 0) {
            throw new RuntimeException("MemberName$Lookup.getMembers() is Field - implement Me");
        } else if ((matchFlags & IS_INVOCABLE) != 0) {
            List<MethodActor> methods = classActor.getMethodActors(searchSupers, searchInterfaces);

            for (MethodActor ma : methods) {
                if (ma.isClassInitializer()) {
                    continue;
                }
                Object memberName = newMemberName();
                init_method_MemberName(memberName, ma, true, defc);
                Trace.line(1, "MemberName$Lookup created id=" + System.identityHashCode(memberName));
                // FIXME better conversions from SignatureDescroptor <=> MethodType
                MethodType mt = MethodType.fromMethodDescriptorString(ma.signature().asString(), null);
                Trace.line(1, "MemberName$Lookup.getMembers Method: " + ma + " TYPE=" + mt);
                asMemberName(memberName).type = mt;
                asMemberName(memberName).name = ma.name();
                list.add(memberName);
            }
        }
        return list;
    }

    @SUBSTITUTE(value = "resolve", signatureDescriptor = "(BLjava/lang/invoke/MemberName;Ljava/lang/Class;)Ljava/lang/invoke/MemberName;")
    private Object resolve(byte refKind, Object memberName, Class< ? > lookupClass) {
        Trace.begin(1, "MemberName$Lookup.resolve: refKind=" + refKind + ", memberName=" + memberName + ", id=" + System.identityHashCode(memberName) + ", lookupClass=" + lookupClass);

        assert refKind == (byte) (asMemberName(memberName).flags >>> REFERENCE_KIND_SHIFT & REFERENCE_KIND_MASK);

        try {
            memberName = JDK_java_lang_invoke_MethodHandleNatives.resolve(memberName, lookupClass);
            asMemberName(memberName).checkForTypeAlias(asMemberName(memberName).clazz);
            asMemberName(memberName).resolution = null;
            assert asMemberName(memberName).referenceKindIsConsistent();
            asMemberName(memberName).initResolved(true);
        } catch (Exception | LinkageError x) {
            Trace.line(1, "***Exception x=" + x.getMessage());
            // assert(asMemberNameAlias(memberName).resolution == null);
            asMemberName(memberName).resolution = x;
        }
        Trace.end(1, "MemberName$Lookup.resolve");
        return memberName;
    }

}
