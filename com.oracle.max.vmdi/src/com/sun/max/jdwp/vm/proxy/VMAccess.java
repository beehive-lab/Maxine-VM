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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * This class specifies the interface to the VM that is used for JDWP.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface VMAccess {

    /**
     * @return the display name of the VM
     */
    @ConstantReturnValue
    String getName();

    /**
     * @return the display version of the VM
     */
    @ConstantReturnValue
    String getVersion();

    /**
     * @return a textual description of the VM
     */
    @ConstantReturnValue
    String getDescription();

    /**
     * Disposes the VM. After calling this functions, the VM should no longer be accessed.
     */
    void dispose();

    /**
     * Suspends all threads in the VM.
     */
    void suspend();

    /**
     * Resumes all threads in the VM.
     */
    void resume();

    /**
     * Exits the VM with the specified exit code.
     *
     * @param code the exit code
     */
    void exit(int code);

    /**
     * @return the class path of the VM as a String array
     */
    @ConstantReturnValue
    String[] getClassPath();

    /**
     * @return the boot class path of the VM as a String array
     */
    @ConstantReturnValue
    String[] getBootClassPath();

    /**
     * This method is used to create a String object in the VM.
     *
     * @param string the value that should be used for initializing the String object
     * @return the newly created String object
     */
    StringProvider createString(String string);

    /**
     * This method can be used to quickly access all reference types that match a certain signature.
     *
     * @param signature the signature that should be matched
     * @return all reference types matching the given signature
     */
    ReferenceTypeProvider[] getReferenceTypesBySignature(String signature);

    /**
     * @return an array of all current threads
     */
    ThreadProvider[] getAllThreads();

    /**
     * @return an array of all known reference types
     */
    ReferenceTypeProvider[] getAllReferenceTypes();

    /**
     * Adds a listener that wants to be informed about VM events.
     *
     * @param listener the listener to be added
     */
    void addListener(VMListener listener);

    /**
     * Removes a formerly added listener.
     * @param listener the listener to be removed
     */
    void removeListener(VMListener listener);

    /**
     * Adds a breakpoint at a specified code location.
     *
     * @param codeLocation the code location at which the breakpoint should be added
     * @param suspendAll whether to suspend all threads when the breakpoint is hit or not
     */
    void addBreakpoint(JdwpCodeLocation codeLocation, boolean suspendAll);

    /**
     * Removes a formerly added breakpoint.
     * @param codeLocation the code location identifying the breakpoint
     */
    void removeBreakpoint(JdwpCodeLocation codeLocation);

    /**
     * @return the top level thread groups
     */
    ThreadGroupProvider[] getThreadGroups();

    /**
     * This method looks up a JDWP reference type based on a Java class object.
     *
     * @param javaClass the java class object whose reference type should be looked up
     * @return the looked up reference type
     */
    ReferenceTypeProvider getReferenceType(Class javaClass);

    /**
     * @return a VMValue object representing void
     */
    @ConstantReturnValue
    VMValue getVoidValue();

    /**
     * @param b a byte value that should be converted into a VMValue object
     * @return a VMValue object representing the specified byte
     */
    VMValue createByteValue(byte b);

    /**
     * @param c a char value that should be converted into a VMValue object
     * @return a VMValue object representing the specified character
     */
    VMValue createCharValue(char c);

    /**
     * @param s a short value that should be converted into a VMValue object
     * @return a VMValue object representing the specified short value
     */
    VMValue createShortValue(short s);

    /**
     * @param i an integer value that should be converted into a VMValue object
     * @return a VMValue object representing the specified integer value
     */
    VMValue createIntValue(int i);

    /**
     * @param f a float value that should be converted into a VMValue object
     * @return a VMValue object representing the specified short value
     */
    VMValue createFloatValue(float f);

    /**
     * @param d a double value that should be converted into a VMValue object
     * @return a VMValue object representing the specified short value
     */
    VMValue createDoubleValue(double d);

    /**
     * @param l a long value that should be converted into a VMValue object
     * @return a VMValue object representing the specified long value
     */
    VMValue createLongValue(long l);

    /**
     * @param b a boolean value that should be converted into a VMValue object
     * @return a VMValue object representing the specified boolean value
     */
    VMValue createBooleanValue(boolean b);

    /**
     * @param p a Provider value that should be converted into a VMValue object
     * @return a VMValue object representing the specified Provider value
     */
    VMValue createObjectProviderValue(ObjectProvider p);

    /**
     * This method wraps a Java object such that it can be represented as a VMValue object and transmitted over JDWP.
     *
     * @param object the object to wrap into a VMValue object
     * @param expectedClass the expected class of the object that should be seen over JDWP
     * @return a VMValue object that wraps the given Java object
     */
    VMValue createJavaObjectValue(Object object, Class expectedClass);
    JdwpCodeLocation createCodeLocation(MethodProvider method, long position, boolean isMachineCode);

    /**
     * Accesses raw memory in the VM.
     *
     * @param start the starting address of the accessed memory
     * @param length how many bytes to access
     * @return a byte array that contains the current data at the specified address
     */
    @JDWPPlus
    byte[] accessMemory(long start, int length);

    /**
     * This method checks the given addresses whether there exists a target method for them. The parameter and result are specified as arrays for performance reasons.
     *
     * @param address an array of addresses to check for target methods
     * @return an array that contains target method object or null if no target method can be found for the corresponding address
     */
    @JDWPPlus
    TargetMethodAccess[] findTargetMethods(long[] address);
}
