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
package com.sun.max.vm.cps.ir.observer;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.ir.*;

/**
 * This class implements global configuration for attaching observers to any newly created IrGenerator instances. It
 * relies on two system properties: "max.ir.observers" and "max.ir.observer.filters".
 * <p>
 * The first system property can be used to specify a list of IrObservers that are attached to each IrGenerator instance
 * at bootstrapping and startup time. It accepts a comma-separated list of class names. The class names can either be
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
 *     -Dmax.ir.observer.method=(method name filters separated by ',')
 * </pre>
 * <p>
 * Where the second system property is used by the IrMethodFilter to determine which methods to trace.
 * <p>
 * As a convenience, all the IR's for a specified set of methods can be traced with their
 * {@linkplain IrMethod#irTraceObserverType() specific IR trace observers} via use of the {@link #IR_TRACE_PROPERTY}
 * system property:
 *
 * <pre>
 *     -Dmax.ir.trace=[(trace level):](method name filters separated by ',')
 * </pre>
 * For example, to trace all IR's when compiling a method whose name includes the string {@code "getClass"} or {@code "hasNext"}:
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

    /**
     * The name of the system property that can be set to control IR tracing for one or more methods as they are compiled.
     */
    public static final String IR_TRACE_PROPERTY = "max.ir.trace";

    static final String packageName = Classes.getPackageName(IrObserverConfiguration.class.getName());

    /**
     * This method attaches all globally-defined observers to a new generator instance.
     *
     * @param generator the new generator to which to attach the global observers
     */
    public static void attach(List<IrGenerator> generators) {
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
        String irTraceValue = System.getProperty(IR_TRACE_PROPERTY);
        if (irTraceValue == null) {
            return null;
        }
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
        if (methodsToBeTraced != null) {
            if (!irObservers.contains(IrTraceObserverDispatcher.class.getSimpleName())) {
                return Strings.concat(IrTraceObserverDispatcher.class.getName(), irObservers, ",");
            }
        }
        return irObservers;
    }

    private static String addTraceFilters(String irFilters, String[] methodsToBeTraced) {
        String extendedIrFilters = irFilters;
        if (methodsToBeTraced != null) {
            if (!extendedIrFilters.contains(IrMethodFilter.class.getSimpleName())) {
                extendedIrFilters = Strings.concat(extendedIrFilters, IrMethodFilter.class.getName(), ",");
            }
            String filterProperty = System.getProperty(IrMethodFilter.PROPERTY_FILTER, "");
            if (methodsToBeTraced.length == 0) {
                filterProperty = Strings.concat(filterProperty, "", ",");
            } else {
                for (String methodName : methodsToBeTraced) {
                    filterProperty = Strings.concat(filterProperty, methodName, ",");
                }
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
                final Class<? extends IrObserver> irObserverClass = Utils.cast(type, getObserver(className));
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
            return Class.forName(packageName + "." + className);
        }
    }

    private static IrObserver instantiateObserver(String className) {
        try {
            final Class<Class<? extends IrObserver>> type = null;
            final Class<? extends IrObserver> irObserverClass = Utils.cast(type, getObserver(className));
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
