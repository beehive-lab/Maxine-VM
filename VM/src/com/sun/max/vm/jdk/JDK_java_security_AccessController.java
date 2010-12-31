/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.security.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Method substitutions for the {@link java.security.AccessController} class.
 * We leverage some common code in the substitutions for {@link sun.reflect.Reflection}.
 */
@METHOD_SUBSTITUTIONS(AccessController.class)
public
final class JDK_java_security_AccessController {

    private JDK_java_security_AccessController() {
    }

    /**
     * Performs a privileged action.
     *
     * @see java.security.AccessController#doPrivileged(PrivilegedAction)
     * @param <T> the type of the result of the privileged action
     * @param action the action to perform
     * @return the result of performing the action
     */
    @SUBSTITUTE
    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        final JDK_sun_reflect_Reflection.Context context = JDK_sun_reflect_Reflection.getCallerContext(1);
        final VmThread current = VmThread.current();
        current.pushPrivilegedElement(context.method.holder(), context.frameId, null);
        try {
            return action.run();
        } finally {
            current.popPrivilegedElement();
        }
    }

    /**
     * Performs a privileged action.
     *
     * @see java.security.AccessController#doPrivileged(PrivilegedAction, AccessControlContext)
     * @param <T> the type of the result of the privileged action
     * @param action the action to perform
     * @param context the access control context
     * @return the result of performing the action
     */
    @SUBSTITUTE
    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext accessControlContext) {
        final JDK_sun_reflect_Reflection.Context context = JDK_sun_reflect_Reflection.getCallerContext(1);
        final VmThread current = VmThread.current();
        current.pushPrivilegedElement(context.method.holder(), context.frameId, accessControlContext);
        try {
            return action.run();
        } finally {
            current.popPrivilegedElement();
        }
    }

    /**
     * Performs a privileged action that may generate an exception.
     *
     * @see java.security.AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext)
     * @param <T> the type of the result of the privileged action
     * @param action the action to perform
     * @return the result of performing the action
     * @throws PrivilegedActionException if the privileged action caused an exception
     */
    @SUBSTITUTE
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        final JDK_sun_reflect_Reflection.Context context = JDK_sun_reflect_Reflection.getCallerContext(1);
        final VmThread current = VmThread.current();
        current.pushPrivilegedElement(context.method.holder(), context.frameId, null);
        try {
            return action.run();
        } catch (Exception exception) {
            throw new PrivilegedActionException(exception);
        } finally {
            current.popPrivilegedElement();
        }
    }

    /**
     * Performs a privileged action that may generate an exception.
     *
     * @see java.security.AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext)
     * @param <T> the type of the result of the privileged action
     * @param action the action to perform
     * @param context the access control context
     * @return the result of performing the action
     * @throws PrivilegedActionException if the privileged action caused an exception
     */
    @SUBSTITUTE
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action, AccessControlContext accessControlContext) throws PrivilegedActionException {
        final JDK_sun_reflect_Reflection.Context context = JDK_sun_reflect_Reflection.getCallerContext(1);
        final VmThread current = VmThread.current();
        current.pushPrivilegedElement(context.method.holder(), context.frameId, accessControlContext);
        try {
            return action.run();
        } catch (Exception exception) {
            throw new PrivilegedActionException(exception);
        } finally {
            current.popPrivilegedElement();
        }
    }

   /**
     * This class implements a closure that analyzes protection domains on the call stack.
     * It is based on the HotSpot implementation.
     */
    static class Context extends SourceFrameVisitor {

        boolean skipping = true;
        final List<ProtectionDomain> protectionDomains = new LinkedList<ProtectionDomain>();
        final VmThread.PrivilegedElement privilegedElement = VmThread.current().getTopPrivilegedElement();
        AccessControlContext privilegedContext;
        ProtectionDomain prevProtectionDomain;
        boolean isPrivileged;

        Context() {
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (skipping) {
                if (method.holder().equals(ClassActor.fromJava(JDK_java_security_AccessController.class)) &&
                    method.name.string.equals("getStackAccessControlContext")) {
                    skipping = false;
                }
                return true;
            }

            isPrivileged = false;
            ProtectionDomain protectionDomain = null;
            // check the privileged frames for a match
            if (privilegedElement != null && privilegedElement.frameId == frameId) {
                isPrivileged = true;
                privilegedContext = privilegedElement.context;
                protectionDomain = privilegedElement.classActor.protectionDomain();
            } else {
                protectionDomain = method.holder().protectionDomain();
            }

            if (prevProtectionDomain != protectionDomain && protectionDomain != null) {
                protectionDomains.add(protectionDomain);
                prevProtectionDomain = protectionDomain;
            }

            // terminate if we found a privileged domain
            return !isPrivileged;
        }
    }

    /**
     * Gets the sequence of protection domains for all the frames on the stack.
     *
     * @return a sequence of protection domains for the current stack
     */
    @NEVER_INLINE
    private static Context getContext() {
        final Context context = new Context();
        context.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        return context;
    }

    @ALIAS(declaringClass = AccessControlContext.class, name = "<init>")
    private native void init(ProtectionDomain[] context, boolean isPrivileged);

    @INTRINSIC(UNSAFE_CAST)
    static native JDK_java_security_AccessController asThis(AccessControlContext acc);

    @ALIAS(declaringClass = AccessControlContext.class)
    AccessControlContext privilegedContext;

    /**
     * Gets the access control context for the current stack.
     *
     * @see java.security.AccessController#getStackAccessControlContext()
     * @return the access control context for the current stack
     */
    @SUBSTITUTE
    public static AccessControlContext getStackAccessControlContext() {
        final Context context = new Context();
        context.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        ProtectionDomain[] protectionDomains = null;
        if (context.protectionDomains.isEmpty()) {
            if (context.isPrivileged && context.privilegedContext == null) {
                return null;
            }
        } else {
            protectionDomains = context.protectionDomains.toArray(new ProtectionDomain[context.protectionDomains.size()]);
        }

        AccessControlContext acc = (AccessControlContext) Heap.createTuple(ClassActor.fromJava(AccessControlContext.class).dynamicHub());
        JDK_java_security_AccessController thisAccessControlContext = asThis(acc);
        thisAccessControlContext.init(protectionDomains, context.isPrivileged);
        if (context.isPrivileged) {
            // need to manually set privilegedContext as no constructor for that
            thisAccessControlContext.privilegedContext = context.privilegedContext;
        }
        return acc;
    }

    /**
     * Gets the inherited access control context.
     *
     * @see java.security.AccessController#getInheritedAccessControlContext()
     * @return the inherited access control context
     */
    @SUBSTITUTE
    public static AccessControlContext getInheritedAccessControlContext() {
        JDK_java_lang_Thread thread = JDK_java_lang_Thread.asThis(Thread.currentThread());
        return thread.inheritedAccessControlContext;
    }
}
