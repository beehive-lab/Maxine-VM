/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.classfile.create;

import java.util.*;

import com.sun.max.vm.type.*;

/**
 * A {@code MillClass} assembles fields, code and constant pool entries
 * and produces Java(TM) class file data, i.e. this is practically a Java(TM)
 * byte code assembler.
 *
 * This class acts as a factory for the constant pool entry classes named
 * {@code Mill...Constant} listed under "See Also".
 *
 * @see MillClassConstant
 * @see MillFieldRefConstant
 * @see MillMethodRefConstant
 * @see MillUtf8Constant
 * @see MillCode
 *
 * @author Bernd Mathiske
 * @version 1.0
 */
public class MillClass {

    /**
     * The modifiers of the class to be assembled.
     */
    public final int modifiers;

    /**
     * The name of the class to be assembled.
     */
    public final String name;

    private final int thisClassIndex;
    private final int superClassIndex;

    private static String slashified(String name) {
        return name.replace('.', '/');
    }

    /**
     * Create a mill class as a handle for a subsequent incremental assembly. Set basic information about the class
     * (file data) to be generated later.
     *
     * @param modifiers The modifiers of the class to be assembled.
     * @param modifiers The name of the class to be assembled.
     * @param modifiers The name of super class of the class to be assembled.
     *
     * @see java.lang.reflect.Modifier
     */
    public MillClass(int modifiers, String name, String superClassName) {
        this.modifiers = modifiers;
        this.name = slashified(name);
        this.thisClassIndex = makeClassConstant(this.name).index;
        this.superClassIndex = makeClassConstant(slashified(superClassName)).index;
    }

    private MillConstant constantList = null;
    private int constantIndex = 0;
    private int numberOfConstantBytes = 0;

    private final Map<MillConstant, MillConstant> constants = new HashMap<MillConstant, MillConstant>();

    private MillConstant unique(MillConstant c) {
        final MillConstant u =  constants.get(c);
        if (u != null) {
            return u;
        }
        c.next = constantList;
        constantList = c;
        c.index = ++constantIndex;
        numberOfConstantBytes += c.numberOfBytes;
        constants.put(c, c);
        return c;
    }

    public MillIntConstant makeIntConstant(int value) {
        return (MillIntConstant) unique(new MillIntConstant(value));
    }

    /**
     * Create a string constant that will be stored in the constant pool in Utf8 format.
     *
     * @param string The string that the constant will represent.
     * @return A Utf8 constant handle.
     *
     * @see MillUtf8Constant
     */
    public MillUtf8Constant makeUtf8Constant(String string) {
        return (MillUtf8Constant) unique(new MillUtf8Constant(string));
    }

    /**
     * Create a class constant in the constant pool.
     *
     * @param className
     *            The name of the class.
     * @return A class constant handle.
     *
     * @see MillClassConstant
     */
    public MillClassConstant makeClassConstant(String className) {
        return (MillClassConstant) unique(new MillClassConstant(makeUtf8Constant(className)));
    }

    private MillNameAndTypeConstant makeNameAndTypeConstant(String n, String descriptor) {
        return (MillNameAndTypeConstant) unique(new MillNameAndTypeConstant(makeUtf8Constant(n), makeUtf8Constant(descriptor)));
    }

    /**
     * Create a field reference constant in the constant pool.
     *
     * @param className The name of the class that contains the referenced field.
     * @param fieldName The name of the referenced field.
     * @param fieldDescriptor The type descriptor of the referenced field.
     * @return A class constant handle.
     *
     * @see MillFieldRefConstant
     * @see org.opj.util.Descriptor
     */
    public MillFieldRefConstant makeFieldRefConstant(String className, String fieldName, String fieldDescriptor) {
        return (MillFieldRefConstant) unique(new MillFieldRefConstant(makeClassConstant(className), makeNameAndTypeConstant(fieldName, fieldDescriptor)));
    }

    /**
     * Create a method reference constant in the constant pool.
     *
     * @param className The name of the class that contains the referenced method.
     * @param methodName The name of the referenced method.
     * @param methodDescriptor The type signature descriptor of the referenced method.
     * @return A class constant handle.
     *
     * @see MillMethodRefConstant
     * @see org.opj.util.Descriptor
     */
    public MillMethodRefConstant makeMethodRefConstant(String className, String methodName, String methodDescriptor) {
        return (MillMethodRefConstant) unique(new MillMethodRefConstant(makeClassConstant(className), makeNameAndTypeConstant(methodName, methodDescriptor)));
    }

    MillInterface interfaceList = null;
    private int numberOfInterfaces = 0;

    /**
     * Add the name of an implemented interface to the class to be generated.
     *
     * @param name
     *            The name of the interface.
     */
    public void addInterface(String name) {
        new MillInterface(this, name);
        numberOfInterfaces++;
    }

    MillField fieldList = null;
    private int numberOfFields = 0;

    /**
     * Add a field to the class to be generated.
     *
     * @param modifiers
     *            The field's modifiers.
     * @param name
     *            The field name.
     * @param descriptor
     *            The type descriptor of the field.
     *
     * @see java.lang.reflect.Modifier
     */
    public void addField(int modifiers, String name, String descriptor) {
        new MillField(this, modifiers, name, descriptor);
        numberOfFields++;
    }

    MillMethod methodList = null;
    private int numberOfMethods = 0;
    private int numberOfMethodBytes = 0;
    private int codeNameIndex = -1;
    private int exceptionsNameIndex = -1;

    /**
     * Add a method to the class to be generated.
     *
     * @param modifiers The method's modifiers.
     * @param name The method name.
     * @param signatureDescriptor The type signature descriptor of the method.
     * @param code The byte code to be generated for the method.
     * @param exceptions An array with all the class names of the exceptions that the method potentially throws.
     *
     * @see java.lang.reflect.Modifier
     * @see MillCode
     * @see MillClassConstant
     */
    public MillMethod addMethod(int modifiers, String name, SignatureDescriptor signatureDescriptor, MillCode code, MillClassConstant[] exceptions) {
        if (codeNameIndex <= 0) {
            codeNameIndex = makeUtf8Constant("Code").index;
        }
        if (exceptions.length > 0 && exceptionsNameIndex <= 0) {
            exceptionsNameIndex = makeUtf8Constant("Exceptions").index;
        }
        final MillMethod method = new MillMethod(this, modifiers, name, signatureDescriptor, code, exceptions);
        numberOfMethodBytes += method.numberOfBytes;
        numberOfMethods++;
        return method;
    }

    /**
     * An empty array of exceptions that is provided for convenience. It can be
     * passed as last argument to {@code addMethod} to indicate that a
     * method throws no exceptions.
     *
     * @see #addMethod
     */
    public static final MillClassConstant[] noExceptions = new MillClassConstant[0];

    /**
     * Add a method that throws no exceptions to the class to be generated.
     *
     * @param modifiers The method's modifiers.
     * @param name The method name.
     * @param signatureDescriptor The type signature descriptor of the method.
     * @param code The byte code to be generated for the method.
     *
     * @see java.lang.reflect.Modifier
     * @see MillCode
     */
    public void addMethod(int modifiers, String name, SignatureDescriptor signatureDescriptor, MillCode code) {
        addMethod(modifiers, name, signatureDescriptor, code, noExceptions);
    }

    private static final int nBytesBeforeConstantPool = 10;

    /**
     * Generate class file data from the incremental specifications that have
     * been accumulated by other methods of this class.
     *
     * @return A byte array containing class file data.
     */
    public byte[] generate() {
        int i = 0;
        final int nBytes = nBytesBeforeConstantPool + numberOfConstantBytes + 8 + (2 * numberOfInterfaces) + 2 + (8 * numberOfFields) + 2 + numberOfMethodBytes + 2;
        final byte[] b = new byte[nBytes];
        b[i++] = (byte) 0xca; // u4 magic
        b[i++] = (byte) 0xfe;
        b[i++] = (byte) 0xba;
        b[i++] = (byte) 0xbe;
        b[i++] = (byte) 0x00; // u2 minor_version
        b[i++] = (byte) 0x03;
        b[i++] = (byte) 0x00; // u2 major_version
        b[i++] = (byte) 0x2d;
        final int nConstants = constantIndex + 1;
        b[i++] = (byte) (nConstants >> 8); // u2 constant_pool_count
        b[i++] = (byte) (nConstants & 0xff);
        i = nBytesBeforeConstantPool + numberOfConstantBytes;
        MillConstant constant = constantList;
        while (constant != null) {
            switch (constant.tag) {
                case MillConstant.CONSTANT_Integer: {
                    final MillIntConstant intConstant = (MillIntConstant) constant;
                    b[--i] = MillWord.byte0(intConstant.value);
                    b[--i] = MillWord.byte1(intConstant.value);
                    b[--i] = MillWord.byte2(intConstant.value);
                    b[--i] = MillWord.byte3(intConstant.value);
                    break;
                }
                case MillConstant.CONSTANT_Utf8: {
                    final MillUtf8Constant c = (MillUtf8Constant) constant;
                    final int length = c.string.length();
                    for (int k = length - 1; k >= 0; k--) {
                        b[--i] = (byte) c.string.charAt(k);
                    }
                    b[--i] = MillWord.byte0(length);
                    b[--i] = MillWord.byte1(length);
                    break;
                }
                case MillConstant.CONSTANT_Class: {
                    final MillClassConstant c = (MillClassConstant) constant;
                    b[--i] = MillWord.byte0(c.nameIndex);
                    b[--i] = MillWord.byte1(c.nameIndex);
                    break;
                }
                case MillConstant.CONSTANT_FieldRef:
                case MillConstant.CONSTANT_MethodRef: {
                    final MillRefConstant c = (MillRefConstant) constant;
                    b[--i] = MillWord.byte0(c.nameAndTypeIndex);
                    b[--i] = MillWord.byte1(c.nameAndTypeIndex);
                    b[--i] = MillWord.byte0(c.classIndex);
                    b[--i] = MillWord.byte1(c.classIndex);
                    break;
                }
                case MillConstant.CONSTANT_NameAndType: {
                    final MillNameAndTypeConstant c = (MillNameAndTypeConstant) constant;
                    b[--i] = MillWord.byte0(c.descriptorIndex);
                    b[--i] = MillWord.byte1(c.descriptorIndex);
                    b[--i] = MillWord.byte0(c.nameIndex);
                    b[--i] = MillWord.byte1(c.nameIndex);
                    break;
                }
                default:
                    throw new Error();
            }
            b[--i] = constant.tag;
            constant = constant.next;
        }
        i = nBytesBeforeConstantPool + numberOfConstantBytes;
        b[i++] = MillWord.byte1(modifiers);
        b[i++] = MillWord.byte0(modifiers);
        b[i++] = MillWord.byte1(thisClassIndex);
        b[i++] = MillWord.byte0(thisClassIndex);
        b[i++] = MillWord.byte1(superClassIndex);
        b[i++] = MillWord.byte0(superClassIndex);
        b[i++] = MillWord.byte1(numberOfInterfaces);
        b[i++] = MillWord.byte0(numberOfInterfaces);
        MillInterface millInterface = interfaceList;
        while (millInterface != null) {
            b[i++] = MillWord.byte1(millInterface.classIndex);
            b[i++] = MillWord.byte0(millInterface.classIndex);
            millInterface = millInterface.next;
        }
        b[i++] = MillWord.byte1(numberOfFields);
        b[i++] = MillWord.byte0(numberOfFields);
        MillField field = fieldList;
        while (field != null) {
            b[i++] = MillWord.byte1(field.modifiers);
            b[i++] = MillWord.byte0(field.modifiers);
            b[i++] = MillWord.byte1(field.nameIndex);
            b[i++] = MillWord.byte0(field.nameIndex);
            b[i++] = MillWord.byte1(field.descriptorIndex);
            b[i++] = MillWord.byte0(field.descriptorIndex);
            b[i++] = 0x00; // u2 attributes_count
            b[i++] = 0x00;
            field = field.next;
        }
        b[i++] = MillWord.byte1(numberOfMethods);
        b[i++] = MillWord.byte0(numberOfMethods);
        MillMethod method = methodList;
        while (method != null) {
            b[i++] = MillWord.byte1(method.modifiers);
            b[i++] = MillWord.byte0(method.modifiers);
            b[i++] = MillWord.byte1(method.nameIndex);
            b[i++] = MillWord.byte0(method.nameIndex);
            b[i++] = MillWord.byte1(method.descriptorIndex);
            b[i++] = MillWord.byte0(method.descriptorIndex);
            b[i++] = 0x00; // u2 attributes_count
            final int nExceptions = method.exceptions.length;
            if (nExceptions > 0) {
                b[i++] = 0x02;
            } else {
                b[i++] = 0x01;
            }

            // code attribute:
            b[i++] = MillWord.byte1(codeNameIndex);
            b[i++] = MillWord.byte0(codeNameIndex);
            int nAttributeBytes = 8 + method.code.nBytes() + 4;
            b[i++] = MillWord.byte3(nAttributeBytes);
            b[i++] = MillWord.byte2(nAttributeBytes);
            b[i++] = MillWord.byte1(nAttributeBytes);
            b[i++] = MillWord.byte0(nAttributeBytes);
            b[i++] = MillWord.byte1(method.code.numberOfMaxStackWords);
            b[i++] = MillWord.byte0(method.code.numberOfMaxStackWords);
            final int nMaxLocals = 1 + method.code.numberOfLocals;
            b[i++] = MillWord.byte1(nMaxLocals);
            b[i++] = MillWord.byte0(nMaxLocals);
            final int codeLength = method.code.nBytes();
            b[i++] = MillWord.byte3(codeLength);
            b[i++] = MillWord.byte2(codeLength);
            b[i++] = MillWord.byte1(codeLength);
            b[i++] = MillWord.byte0(codeLength);
            method.code.assemble(b, i);
            i += method.code.nBytes();
            b[i++] = 0x00; // u2 exception_table_length
            b[i++] = 0x00;
            b[i++] = 0x00; // u2 attributes_count
            b[i++] = 0x00;
            if (nExceptions > 0) { // exceptions attribute:
                b[i++] = MillWord.byte1(exceptionsNameIndex);
                b[i++] = MillWord.byte0(exceptionsNameIndex);
                nAttributeBytes = 2 + (2 * nExceptions);
                b[i++] = MillWord.byte3(nAttributeBytes);
                b[i++] = MillWord.byte2(nAttributeBytes);
                b[i++] = MillWord.byte1(nAttributeBytes);
                b[i++] = MillWord.byte0(nAttributeBytes);
                b[i++] = MillWord.byte1(nExceptions);
                b[i++] = MillWord.byte0(nExceptions);
                for (int j = 0; j < nExceptions; j++) {
                    b[i++] = MillWord.byte1(method.exceptions[j].index);
                    b[i++] = MillWord.byte0(method.exceptions[j].index);
                }
            }
            method = method.next;
        }
        b[i++] = 0x00; // u2 attributes_count
        b[i++] = 0x00;
        return b;
    }

}
