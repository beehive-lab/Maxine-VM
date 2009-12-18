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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.stack.RawStackFrameVisitor.Util.*;

import java.security.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Method substitutions for the {@link java.security.AccessController} class.
 */
@METHOD_SUBSTITUTIONS(AccessController.class)
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
        // TODO implement this properly!
        return action.run();
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
    public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
        // TODO implement this properly!
        return action.run();
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
        // TODO implement this properly!
        try {
            return action.run();
        } catch (Exception exception) {
            throw new PrivilegedActionException(exception);
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
    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action, AccessControlContext context) throws PrivilegedActionException {
        // TODO implement this properly!
        try {
            return action.run();
        } catch (Exception exception) {
            throw new PrivilegedActionException(exception);
        }
    }

    /**
     * This class implements a closure that gathers stack frames during a stack walk. TODO: this should be removed and
     * replaced with utilities in StackFrameWalker.
     */
    private static class Context implements RawStackFrameVisitor {
        final AppendableSequence<ProtectionDomain> result = new LinkSequence<ProtectionDomain>();

        Context() {
        }

        public boolean visitFrame(TargetMethod targetMethod, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, int flags) {
            if (isTopFrame(flags)) {
                // skip 'getCallerMethod()'
                return true;
            }
            if (isAdapter(flags)) {
                return true;
            }
            if (targetMethod == null) {
                // native frame
                return true;
            }
            final Iterator<? extends BytecodeLocation> bytecodeLocations = targetMethod.getBytecodeLocationsFor(instructionPointer);
            if (bytecodeLocations == null) {
                final ProtectionDomain protectionDomain = targetMethod.classMethodActor().holder().protectionDomain();
                if (protectionDomain != null) {
                    result.append(protectionDomain);
                }
            } else {
                while (bytecodeLocations.hasNext()) {
                    final BytecodeLocation bytecodeLocation = bytecodeLocations.next();
                    final MethodActor classMethodActor = bytecodeLocation.classMethodActor;
                    if (classMethodActor.isApplicationVisible()) {
                        final ProtectionDomain protectionDomain = bytecodeLocation.classMethodActor.holder().protectionDomain();
                        if (protectionDomain != null) {
                            result.append(protectionDomain);
                        }
                    }
                }
            }
            return true;
        }
    }

    /**
     * Gets the sequence of protection domains for all the frames on the stack.
     *
     * @return a sequence of protection domains for the current stack
     */
    @NEVER_INLINE
    private static Sequence<ProtectionDomain> getProtectionDomains() {
        final Context context = new Context();
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer(),
                                                       context);
        return context.result;
    }

    /**
     * Gets the access control context for the current stack.
     *
     * @see java.security.AccessController#getStackAccessControlContext()
     * @return the access control context for the current stack
     */
    @SUBSTITUTE
    public static AccessControlContext getStackAccessControlContext() {
        return new AccessControlContext(Sequence.Static.toArray(getProtectionDomains(), new ProtectionDomain[getProtectionDomains().length()]));
    }

    /**
     * Gets the inherited access control context.
     *
     * @see java.security.AccessController#getInheritedAccessControlContext()
     * @return the inherited access control context
     */
    @SUBSTITUTE
    public static AccessControlContext getInheritedAccessControlContext() {
        return UnsafeCast.asAccessControlContext(TupleAccess.readObject(Thread.currentThread(), ClassRegistry.Thread_inheritedAccessControlContext.offset()));
    }
}
