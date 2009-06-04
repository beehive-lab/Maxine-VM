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
 * The <code>CiRuntime</code> class provides an implementation of a number of runtime calls that are
 * used by C1X. C1X may insert calls to the implementation of these methods into compiled code,
 * typically as the slow path.
 *
 * @author Ben L. Titzer
 */
public interface CiRuntime {
    public CiConstantPool getConstantPool(CiMethod method);
    public CiOsrFrame getOsrFrame(CiMethod method, int bci);

    public boolean mustInline(CiMethod method);
    public boolean mustNotInline(CiMethod method);
    public boolean mustNotCompile(CiMethod method);

    public boolean instanceOf(Object object, CiType type);
    public Object checkCast(Object object, CiType type);
    public Object allocateObject(CiType type);
    public Object allocateArray(CiType type);
    public CiType resolveType(String name);
    public CiType getType(Class<?> javaClass);

}
