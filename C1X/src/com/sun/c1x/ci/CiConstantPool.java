/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ci;

/**
 * The <code>CiConstantPool</code> class provides the interface to the constant pool that is
 * used by C1X. The <code>lookup</code> methods look up a constant pool entry without performing
 * resolution, and are used during compilation. The <code>resolve</code> methods are used
 * for resolving constant pool entries at run time, and calls to these methods are inserted
 * by C1X for unresolved entries.
 *
 * @author Ben L. Titzer
 */
public interface CiConstantPool {

    CiField resolveGetField(char cpi);
    CiField resolvePutField(char cpi);
    CiField resolveGetStatic(char cpi);
    CiField resolvePutStatic(char cpi);

    CiMethod resolveInvokeVirtual(char cpi);
    CiMethod resolveInvokeSpecial(char cpi);
    CiMethod resolveInvokeInterface(char cpi);
    CiMethod resolveInvokeStatic(char cpi);

    CiType resolveType(char cpi);
    String resolveString(char cpi);
    Class<?> resolveClass(char cpi);

    CiType lookupType(char cpi);
    CiField lookupField(int opcode, char cpi);
    CiMethod lookupMethod(int opcode, char cpi);

    boolean willLinkField(int opcode, char cpi);
    boolean willLinkMethod(int opcode, char cpi);

    CiConstant lookupConstant(char cpi);
    CiExceptionHandler newExceptionHandler(int startBCI, int endBCI, int catchBCI, int classCPI);
}
