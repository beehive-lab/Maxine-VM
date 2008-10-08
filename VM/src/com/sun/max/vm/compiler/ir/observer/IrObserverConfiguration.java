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
package com.sun.max.vm.compiler.ir.observer;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;

/**
 * This class implements global configuration for attaching observers to any newly created IrGenerator instances. It
 * relies on two system properties: "max.ir.observers" and "max.ir.observer.filters".
 * <p>
 * The first system property can be used to specify a list of IrObservers that are attached to each IrGenerator instance
 * at prototyping and startup time. It accepts a comma-separated list of class names. The class names can either be
 * fully qualified or assumed to reside in this package (com.sun.max.vm.compiler.ir.observer).
 * <p>
 * For example:
 * <p>
 *
 * <pre>
 *     -Dmax.ir.observers=IrTraceObserver
 * </pre>
 * <p>
 * attaches the trace observer.
 * <p>
 * The second system property can be used to specify a list of filters that are attached to IrObservers. A filter limits
 * the events that are passed to the observers specified in the "max.ir.observers" property. It accepts a
 * comma-separated list of class names in the same manner. For example, to apply the {@link IrMethodFilter}, one can
 * specify:
 * <p>
 *
 * <pre>
 *     -Dmax.ir.observer.filters=IrMethodFilter
 *     -Dmax.ir.observer.method=(method name filters separated by '.')
 * </pre>
 * <p>
 * Where the second system property is used by the IrMethodFilter to determine which methods to trace.
 * <p>
 * As a convenience, all the IR's for a specified set of methods can be traced with their
 * {@linkplain IrMethod#irTraceObserverType() specific IR trace observers} via use of the {@code "max.ir.trace"}
 * property:
 *
 * <pre>
 *     -Dmax.ir.trace=[(trace level):](method name filters separated by '.')
 * </pre>
 * For example, to trace all IR's when compiling a method who name includes the string {@code "getClass"} or {@code "hasNext"}:
 * <pre>
 *     -Dmax.ir.trace=getClass,hasNext
 * </pre>
 * To constrain this tracing to only those with level of 3 or below:
 * <pre>
 *     -Dmax.ir.trace=3:getClass,hasNext
 * </pre>
 *
 * @author Ben L. Titzer
 */
public class IrObserverConfiguration {

    static final Package _package = new Package();

    /**
     * This method attaches all globally-defined observers to a new generator instance.
     *
     * @param generator the new generator to which to attach the global observers
     */
    public static void attach(Sequence<IrGenerator> generators) {
        final String[] methodsToBeTraced = parseMaxIrTraceProperty();
        final String irObservers = addTraceObservers(System.getProperty("max.ir.observers", ""), methodsToBeTraced);
        final String irFilters = addTraceFilters(System.getProperty("max.ir.observer.filters", ""), methodsToBeTraced);

        final String[] observerNames = irObservers.split(",");
        final String[] filterNames = irFilters.split(",");
        for (String className : observerNames) {
            if (className.length() > 0) {
                final IrObserver observer = attachFilters(instantiateObserver(className), filterNames);
                for (IrGenerator generator : generators) {
                    if (observer.attach(generator)) {
                        generator.addIrObserver(observer);
                    }
                }
            }
        }
    }

    private static String[] parseMaxIrTraceProperty() {
        String irTraceValue = System.getProperty("max.ir.trace", "");
        String[] methodsToBeTraced = {};
        if (!irTraceValue.isEmpty()) {
            final int indexOfColon = irTraceValue.indexOf(':');
            if (indexOfColon != -1) {
                System.setProperty(IrTraceObserver.PROPERTY_TRACE_LEVEL, irTraceValue.substring(0, indexOfColon));
                irTraceValue = irTraceValue.substring(indexOfColon + 1);
            }
            methodsToBeTraced = irTraceValue.split(",");
        }
        return methodsToBeTraced;
    }

    private static String addTraceObservers(String irObservers, String[] methodsToBeTraced) {
        if (methodsToBeTraced.length > 0) {
            if (!irObservers.contains(IrTraceObserverDispatcher.class.getSimpleName())) {
                return Strings.concat(irObservers, IrTraceObserverDispatcher.class.getName(), ",");
            }
        }
        return irObservers;
    }

    private static String addTraceFilters(String irFilters, String[] methodsToBeTraced) {
        String extendedIrFilters = irFilters;
        if (methodsToBeTraced.length > 0) {
            if (!extendedIrFilters.contains(IrMethodFilter.class.getSimpleName())) {
                extendedIrFilters = Strings.concat(extendedIrFilters, IrMethodFilter.class.getName(), ",");
            }
            String filterProperty = System.getProperty(IrMethodFilter.PROPERTY_FILTER, "");
            for (String methodName : methodsToBeTraced) {
                filterProperty = Strings.concat(filterProperty, methodName, ",");
            }
            System.setProperty(IrMethodFilter.PROPERTY_FILTER, filterProperty);
        }
        return extendedIrFilters;
    }

    private static IrObserver attachFilters(IrObserver observer, String[] filterNames) {
        IrObserver result = observer;
        for (String className : filterNames) {
            if (className.length() == 0) {
                continue;
            }
            // attach the filters in a chain to the specified observer
            try {
                final Class<Class<? extends IrObserver>> type = null;
                final Class<? extends IrObserver> irObserverClass = StaticLoophole.cast(type, getObserver(className));
                result = irObserverClass.getConstructor(IrObserver.class).newInstance(result);
            } catch (ClassCastException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            } catch (ClassNotFoundException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            } catch (InstantiationException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            } catch (IllegalAccessException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            } catch (InvocationTargetException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            } catch (NoSuchMethodException e) {
                throw ProgramError.unexpected("Could not initialize IR filter implemented by " + className, e);
            }
        }
        return result;
    }

    private static Class<?> getObserver(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // try again by adding the default package name to the specified class
            return Class.forName(_package.name() + "." + className);
        }
    }

    private static IrObserver instantiateObserver(String className) {
        try {
            final Class<Class<? extends IrObserver>> type = null;
            final Class<? extends IrObserver> irObserverClass = StaticLoophole.cast(type, getObserver(className));
            return irObserverClass.newInstance();
        } catch (ClassCastException e) {
            throw ProgramError.unexpected("Could not initialize IR observer implemented by " + className, e);
        } catch (ClassNotFoundException e) {
            throw ProgramError.unexpected("Could not initialize IR observer implemented by " + className, e);
        } catch (InstantiationException e) {
            throw ProgramError.unexpected("Could not initialize IR observer implemented by " + className, e);
        } catch (IllegalAccessException e) {
            throw ProgramError.unexpected("Could not initialize IR observer implemented by " + className, e);
        }
    }
}
