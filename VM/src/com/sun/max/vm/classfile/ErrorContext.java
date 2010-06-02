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
package com.sun.max.vm.classfile;

import java.util.*;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.verifier.*;

/**
 * A utility for raising errors and associating them with one or more enclosing calling contexts. The general pattern of
 * usage is shown by the following {@linkplain ClassfileReader#loadClass(String) example} of entering a context for
 * parsing a class file:
 *
 * <pre>
 *     public ClassActor loadClass(final String name) {
 *         try {
 *             enterContext("loading" + name);
 *             return loadClass0(name);
 *         } finally {
 *             exitContext();
 *         }
 * </pre>
 *
 * If an exception is raised via one of the static methods in this class while in the call to {@code loadClass0()},
 * then the {@linkplain Throwable#getMessage() detail message} of the raised exception has a line appended to it for
 * each enclosing calling context. The format of each context line is the result of calling {@link Object#toString()} on
 * the argument provided to {@link #enterContext(Object)} prefixed by {@code "    while "}. For example:
 *
 * <pre>
 * java.lang.ClassFormatError: Invalid method signature: Ljava/io/PrintStream;
 *     while loading javasoft.sqe.tests.vm.classfmt.cpl.cplmbr201.cplmbr20102m1.cplmbr20102m11n
 * </pre>
 *
 * The argument to {@code enterContext()} is of type {@code Object} so that the description of the context is computed
 * only if an exception is raised. This reduces the overhead of entering contexts. For example, the above example could
 * be re-written as follows if the cost of the string concatenation is too high compared to the cost of the call to
 * {@code loadClass0()}:
 *
 * <pre>
 *     public ClassActor loadClass(final String name) {
 *         try {
 *             enterContext(new Object() {
 *                 public String toString() {
 *                     return "loading " + name;
 *                 }
 *             });
 *             return loadClass0(name);
 *         } finally {
 *             exitContext();
 *         }
 * </pre>
 *
 * The execution cost of entering a context can be reduced even further by using the provided
 * {@linkplain #perform(Object, Runnable) callback mechanism}.
 *
 * @author Doug Simon
 */
public final class ErrorContext {

    private ErrorContext() {
    }

    private static final ObjectThreadLocal<List<Object>> ERROR_CONTEXTS = new ObjectThreadLocal<List<Object>>("ERROR_CONTEXTS", "Nested error contexts during class loading") {
        @Override
        protected List<Object> initialValue() {
            return new ArrayList<Object>();
        }
    };

    private static List<Object> makeContexts() {
        return ERROR_CONTEXTS.get();
    }

    /**
     * Gets the current error contexts for the current thread.
     */
    public static List<Object> contexts() {
        final List<Object> contexts = ERROR_CONTEXTS.getWithoutInitialization();
        if (contexts == null) {
            return Collections.emptyList();
        }
        return contexts;
    }

    public static void enterContext(Object context) {
        makeContexts().add(context);
    }

    public static void exitContext() {
        try {
            List<Object> contexts = makeContexts();
            contexts.remove(contexts.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            ProgramWarning.message("Unstructured use of error contexts");
        }
    }

    public static void perform(Object context, Runnable runnable) {
        final List<Object> contextStack = makeContexts();
        contextStack.add(context);
        try {
            runnable.run();
        } finally {
            contextStack.remove(contextStack.size() - 1);
        }
    }

    public static String appendContexts(String message) {
        final StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message);
        }
        final String lineSeparator = System.getProperty("line.separator", "\n");
        List<Object> contexts = new ArrayList<Object>(contexts());
        for (int i = contexts.size() - 1; i >= 0; --i) {
            Object context = contexts.get(i);
            if (sb.length() != 0) {
                sb.append(lineSeparator).append("    ");
            }
            sb.append("while ").append(context);
        }
        return sb.toString();
    }

    public static AbstractMethodError abstractMethodError(String message) {
        throw new AbstractMethodError(appendContexts(message));
    }

    public static IncompatibleClassChangeError incompatibleClassChangeError(String message) {
        throw new IncompatibleClassChangeError(appendContexts(message));
    }

    public static NoSuchFieldError noSuchFieldError(String message, Throwable cause) {
        throw (NoSuchFieldError) new NoSuchFieldError(appendContexts(message)).initCause(cause);
    }

    public static NoSuchFieldError noSuchFieldError(String message) {
        throw new NoSuchFieldError(appendContexts(message));
    }

    public static NoSuchMethodError noSuchMethodError(String message, Throwable cause) {
        throw (NoSuchMethodError) new NoSuchMethodError(appendContexts(message)).initCause(cause);
    }

    public static NoSuchMethodError noSuchMethodError(String message) {
        throw new NoSuchMethodError(appendContexts(message));
    }

    public static ClassFormatError classFormatError(String message, Throwable cause) {
        throw (ClassFormatError) new ClassFormatError(appendContexts(message)).initCause(cause);
    }

    public static ClassFormatError classFormatError(String message) {
        throw new ClassFormatError(appendContexts(message));
    }

    public static VerifyError verifyError(String message, ClassMethodActor classMethodActor, CodeAttribute codeAttribute, int address) {
        throw new ExtendedVerifyError(appendContexts(message), classMethodActor, codeAttribute, address);
    }

    public static VerifyError verifyError(String message) {
        throw new VerifyError(appendContexts(message));
    }

    public static NoClassDefFoundError noClassDefFoundError(String message, Throwable cause) {
        throw (NoClassDefFoundError) new NoClassDefFoundError(appendContexts(message)).initCause(cause);
    }

    public static NoClassDefFoundError noClassDefFoundError(String message) {
        throw new NoClassDefFoundError(appendContexts(message));
    }

    public static UnsupportedClassVersionError unsupportedClassVersionError(String message) {
        throw new UnsupportedClassVersionError(appendContexts(message));
    }
}
