/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is
 * described in this document. In particular, and without limitation, these intellectual property rights may include one
 * or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent
 * applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks are used under license and
 * are trademarks or registered trademarks of SPARC International, Inc. in the U.S. and other countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open Company, Ltd.
 */

package com.sun.mockvm;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;

import com.sun.cri.ci.CiConstant;
import com.sun.cri.ri.RiConstantPool;
import com.sun.cri.ri.RiField;
import com.sun.cri.ri.RiMethod;
import com.sun.cri.ri.RiSignature;
import com.sun.cri.ri.RiType;

/**
 * 
 * @author Thomas Wuerthinger
 * 
 */
public class MockConstantPool implements RiConstantPool {

    private ConstantPool cp;

    public MockConstantPool(ConstantPool cp) {
        this.cp = cp;
    }

    @Override
    public CiConstant encoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupConstant(int cpi) {

        Constant c = cp.getConstant(cpi);
        if (c instanceof ConstantString) {
            return CiConstant.forObject(((ConstantString) c).getBytes(cp));
        } else if (c instanceof ConstantInteger) {
            return CiConstant.forInt(((ConstantInteger) c).getBytes());
        } else if (c instanceof ConstantFloat) {
            return CiConstant.forFloat(((ConstantFloat) c).getBytes());
        } else if (c instanceof ConstantDouble) {
            return CiConstant.forDouble(((ConstantDouble) c).getBytes());
        } else {
            System.out.println(cp.getConstant(cpi));
            throw new UnsupportedOperationException();
        }
    }

    private RiMethod lookupInvoke(int cpi, int opcode) {
        ConstantMethodref c = (ConstantMethodref) cp.getConstant(cpi);
        MockType type = (MockType) lookupType((char) c.getClassIndex(), opcode);
        ConstantNameAndType methodConstant = (ConstantNameAndType) cp.getConstant(c.getNameAndTypeIndex());
        String methodName = methodConstant.getName(cp);
        String methodSignature = methodConstant.getSignature(cp);
        final RiMethod result = type.lookupMethod(methodName, methodSignature);
        return result;
    }

    public RiMethod lookupMethod(int cpi, int opcode) {
        return lookupInvoke(cpi, opcode);
    }

    public RiField lookupField(int cpi, int opcode) {
        ConstantFieldref c = (ConstantFieldref) cp.getConstant(cpi);
        MockType type = (MockType) lookupType((char) c.getClassIndex(), opcode);
        ConstantNameAndType methodConstant = (ConstantNameAndType) cp.getConstant(c.getNameAndTypeIndex());
        String fieldName = methodConstant.getName(cp);
        String fieldSignature = methodConstant.getSignature(cp);
        final RiField result = type.lookupField(fieldName, fieldSignature);
        return result;
    }

    @Override
    public RiSignature lookupSignature(int cpi) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RiType lookupType(int cpi, int opcode) {
        Constant c = cp.getConstant(cpi);
        ConstantClass cc = (ConstantClass) c;
        int nameIndex = cc.getNameIndex();
        Constant name = cp.getConstant(nameIndex);
        ConstantUtf8 utf = (ConstantUtf8) name;
        String klassName = utf.getBytes().replace('/', '.');
        return MockUniverse.lookupType(klassName);
    }
}
