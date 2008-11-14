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
    public final int _modifiers;

    /**
     * The name of the class to be assembled.
     */
    public final String _name;

    private final int _thisClassIndex;
    private final int _superClassIndex;

    private static String slashified(String name) {
        return name.replace('.', '/');
    }

    /**
     * Create a mill class as a handle for a subsequent incremental assembly.
     * Set basic information about the class (file data) to be generated later.
     * 
     * @param modifiers
     *            The modifiers of the class to be assembled.
     * @param modifiers
     *            The name of the class to be assembled.
     * @param modifiers
     *            The name of super class of the class to be assembled.
     * 
     * @see java.lang.reflect.Modifier
     */
    public MillClass(int modifiers, String name, String superClassName) {
        _modifiers = modifiers;
        _name = slashified(name);
        _thisClassIndex = makeClassConstant(_name)._index;
        _superClassIndex = makeClassConstant(slashified(superClassName))._index;
    }

    private MillConstant _constantList = null;
    private int _constantIndex = 0;
    private int _numberOfConstantBytes = 0;

    private final Map<MillConstant, MillConstant> _constants = new HashMap<MillConstant, MillConstant>();

    private MillConstant unique(MillConstant c) {
        final MillConstant u =  _constants.get(c);
        if (u != null) {
            return u;
        }
        c._next = _constantList;
        _constantList = c;
        c._index = ++_constantIndex;
        _numberOfConstantBytes += c._numberOfBytes;
        _constants.put(c, c);
        return c;
    }

    public MillIntConstant makeIntConstant(int value) {
        return (MillIntConstant) unique(new MillIntConstant(value));
    }

    /**
     * Create a string constant that will be stored in the constant pool in Utf8
     * format.
     * 
     * @param string
     *            The string that the constant will represent.
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

    private MillNameAndTypeConstant makeNameAndTypeConstant(String name, String descriptor) {
        return (MillNameAndTypeConstant) unique(new MillNameAndTypeConstant(makeUtf8Constant(name), makeUtf8Constant(descriptor)));
    }

    /**
     * Create a field reference constant in the constant pool.
     * 
     * @param className
     *            The name of the class that contains the referenced field.
     * @param fieldName
     *            The name of the referenced field.
     * @param fieldDescriptor
     *            The type descriptor of the referenced field.
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
     * @param className
     *            The name of the class that contains the referenced method.
     * @param methodName
     *            The name of the referenced method.
     * @param methodDescriptor
     *            The type signature descriptor of the referenced method.
     * @return A class constant handle.
     * 
     * @see MillMethodRefConstant
     * @see org.opj.util.Descriptor
     */
    public MillMethodRefConstant makeMethodRefConstant(String className, String methodName, String methodDescriptor) {
        return (MillMethodRefConstant) unique(new MillMethodRefConstant(makeClassConstant(className), makeNameAndTypeConstant(methodName, methodDescriptor)));
    }

    MillInterface _interfaceList = null;
    private int _numberOfInterfaces = 0;

    /**
     * Add the name of an implemented interface to the class to be generated.
     * 
     * @param name
     *            The name of the interface.
     */
    public void addInterface(String name) {
        new MillInterface(this, name);
        _numberOfInterfaces++;
    }

    MillField _fieldList = null;
    private int _numberOfFields = 0;

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
        _numberOfFields++;
    }

    MillMethod _methodList = null;
    private int _numberOfMethods = 0;
    private int _numberOfMethodBytes = 0;
    private int _codeNameIndex = -1;
    private int _exceptionsNameIndex = -1;

    /**
     * Add a method to the class to be generated.
     * 
     * @param modifiers
     *            The method's modifiers.
     * @param name
     *            The method name.
     * @param _descriptor
     *            The type signature descriptor of the method.
     * @param code
     *            The byte code to be generated for the method.
     * @param exceptions
     *            An array with all the class names of the exceptions that the
     *            method potentially throws.
     * 
     * @see java.lang.reflect.Modifier
     * @see MillCode
     * @see MillClassConstant
     */
    public MillMethod addMethod(int modifiers, String name, SignatureDescriptor signatureDescriptor, MillCode code, MillClassConstant[] exceptions) {
        if (_codeNameIndex <= 0) {
            _codeNameIndex = makeUtf8Constant("Code")._index;
        }
        if (exceptions.length > 0 && _exceptionsNameIndex <= 0) {
            _exceptionsNameIndex = makeUtf8Constant("Exceptions")._index;
        }
        final MillMethod method = new MillMethod(this, modifiers, name, signatureDescriptor, code, exceptions);
        _numberOfMethodBytes += method._numberOfBytes;
        _numberOfMethods++;
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
     * @param modifiers
     *            The method's modifiers.
     * @param name
     *            The method name.
     * @param _descriptor
     *            The type signature descriptor of the method.
     * @param code
     *            The byte code to be generated for the method.
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
        final int nBytes = nBytesBeforeConstantPool + _numberOfConstantBytes + 8 + (2 * _numberOfInterfaces) + 2 + (8 * _numberOfFields) + 2 + _numberOfMethodBytes + 2;
        final byte[] b = new byte[nBytes];
        b[i++] = (byte) 0xca; // u4 magic
        b[i++] = (byte) 0xfe;
        b[i++] = (byte) 0xba;
        b[i++] = (byte) 0xbe;
        b[i++] = (byte) 0x00; // u2 minor_version
        b[i++] = (byte) 0x03;
        b[i++] = (byte) 0x00; // u2 major_version
        b[i++] = (byte) 0x2d;
        final int nConstants = _constantIndex + 1;
        b[i++] = (byte) (nConstants >> 8); // u2 constant_pool_count
        b[i++] = (byte) (nConstants & 0xff);
        i = nBytesBeforeConstantPool + _numberOfConstantBytes;
        MillConstant constant = _constantList;
        while (constant != null) {
            switch (constant._tag) {
                case MillConstant.CONSTANT_Integer: {
                    final MillIntConstant intConstant = (MillIntConstant) constant;
                    b[--i] = MillWord.byte0(intConstant._value);
                    b[--i] = MillWord.byte1(intConstant._value);
                    b[--i] = MillWord.byte2(intConstant._value);
                    b[--i] = MillWord.byte3(intConstant._value);
                    break;
                }
                case MillConstant.CONSTANT_Utf8: {
                    final MillUtf8Constant c = (MillUtf8Constant) constant;
                    final int length = c._string.length();
                    for (int k = length - 1; k >= 0; k--) {
                        b[--i] = (byte) c._string.charAt(k);
                    }
                    b[--i] = MillWord.byte0(length);
                    b[--i] = MillWord.byte1(length);
                    break;
                }
                case MillConstant.CONSTANT_Class: {
                    final MillClassConstant c = (MillClassConstant) constant;
                    b[--i] = MillWord.byte0(c._nameIndex);
                    b[--i] = MillWord.byte1(c._nameIndex);
                    break;
                }
                case MillConstant.CONSTANT_FieldRef:
                case MillConstant.CONSTANT_MethodRef: {
                    final MillRefConstant c = (MillRefConstant) constant;
                    b[--i] = MillWord.byte0(c._nameAndTypeIndex);
                    b[--i] = MillWord.byte1(c._nameAndTypeIndex);
                    b[--i] = MillWord.byte0(c._classIndex);
                    b[--i] = MillWord.byte1(c._classIndex);
                    break;
                }
                case MillConstant.CONSTANT_NameAndType: {
                    final MillNameAndTypeConstant c = (MillNameAndTypeConstant) constant;
                    b[--i] = MillWord.byte0(c._descriptorIndex);
                    b[--i] = MillWord.byte1(c._descriptorIndex);
                    b[--i] = MillWord.byte0(c._nameIndex);
                    b[--i] = MillWord.byte1(c._nameIndex);
                    break;
                }
                default:
                    throw new Error();
            }
            b[--i] = constant._tag;
            constant = constant._next;
        }
        i = nBytesBeforeConstantPool + _numberOfConstantBytes;
        b[i++] = MillWord.byte1(_modifiers);
        b[i++] = MillWord.byte0(_modifiers);
        b[i++] = MillWord.byte1(_thisClassIndex);
        b[i++] = MillWord.byte0(_thisClassIndex);
        b[i++] = MillWord.byte1(_superClassIndex);
        b[i++] = MillWord.byte0(_superClassIndex);
        b[i++] = MillWord.byte1(_numberOfInterfaces);
        b[i++] = MillWord.byte0(_numberOfInterfaces);
        MillInterface millInterface = _interfaceList;
        while (millInterface != null) {
            b[i++] = MillWord.byte1(millInterface._classIndex);
            b[i++] = MillWord.byte0(millInterface._classIndex);
            millInterface = millInterface._next;
        }
        b[i++] = MillWord.byte1(_numberOfFields);
        b[i++] = MillWord.byte0(_numberOfFields);
        MillField field = _fieldList;
        while (field != null) {
            b[i++] = MillWord.byte1(field._modifiers);
            b[i++] = MillWord.byte0(field._modifiers);
            b[i++] = MillWord.byte1(field._nameIndex);
            b[i++] = MillWord.byte0(field._nameIndex);
            b[i++] = MillWord.byte1(field._descriptorIndex);
            b[i++] = MillWord.byte0(field._descriptorIndex);
            b[i++] = 0x00; // u2 attributes_count
            b[i++] = 0x00;
            field = field._next;
        }
        b[i++] = MillWord.byte1(_numberOfMethods);
        b[i++] = MillWord.byte0(_numberOfMethods);
        MillMethod method = _methodList;
        while (method != null) {
            b[i++] = MillWord.byte1(method._modifiers);
            b[i++] = MillWord.byte0(method._modifiers);
            b[i++] = MillWord.byte1(method._nameIndex);
            b[i++] = MillWord.byte0(method._nameIndex);
            b[i++] = MillWord.byte1(method._descriptorIndex);
            b[i++] = MillWord.byte0(method._descriptorIndex);
            b[i++] = 0x00; // u2 attributes_count
            final int nExceptions = method._exceptions.length;
            if (nExceptions > 0) {
                b[i++] = 0x02;
            } else {
                b[i++] = 0x01;
            }

            // code attribute:
            b[i++] = MillWord.byte1(_codeNameIndex);
            b[i++] = MillWord.byte0(_codeNameIndex);
            int nAttributeBytes = 8 + method._code.nBytes() + 4;
            b[i++] = MillWord.byte3(nAttributeBytes);
            b[i++] = MillWord.byte2(nAttributeBytes);
            b[i++] = MillWord.byte1(nAttributeBytes);
            b[i++] = MillWord.byte0(nAttributeBytes);
            b[i++] = MillWord.byte1(method._code._numberOfMaxStackWords);
            b[i++] = MillWord.byte0(method._code._numberOfMaxStackWords);
            final int nMaxLocals = 1 + method._code._numberOfLocals;
            b[i++] = MillWord.byte1(nMaxLocals);
            b[i++] = MillWord.byte0(nMaxLocals);
            final int codeLength = method._code.nBytes();
            b[i++] = MillWord.byte3(codeLength);
            b[i++] = MillWord.byte2(codeLength);
            b[i++] = MillWord.byte1(codeLength);
            b[i++] = MillWord.byte0(codeLength);
            method._code.assemble(b, i);
            i += method._code.nBytes();
            b[i++] = 0x00; // u2 exception_table_length
            b[i++] = 0x00;
            b[i++] = 0x00; // u2 attributes_count
            b[i++] = 0x00;
            if (nExceptions > 0) { // exceptions attribute:
                b[i++] = MillWord.byte1(_exceptionsNameIndex);
                b[i++] = MillWord.byte0(_exceptionsNameIndex);
                nAttributeBytes = 2 + (2 * nExceptions);
                b[i++] = MillWord.byte3(nAttributeBytes);
                b[i++] = MillWord.byte2(nAttributeBytes);
                b[i++] = MillWord.byte1(nAttributeBytes);
                b[i++] = MillWord.byte0(nAttributeBytes);
                b[i++] = MillWord.byte1(nExceptions);
                b[i++] = MillWord.byte0(nExceptions);
                for (int j = 0; j < nExceptions; j++) {
                    b[i++] = MillWord.byte1(method._exceptions[j]._index);
                    b[i++] = MillWord.byte0(method._exceptions[j]._index);
                }
            }
            method = method._next;
        }
        b[i++] = 0x00; // u2 attributes_count
        b[i++] = 0x00;
        return b;
    }

}
