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
/*VCSID=c8a4c0cb-56a7-4758-8bc3-5cf7c83a9fd8*/
package test.com.sun.max.ins.interpreter;


/**
 *  @author Athul Acharya
 */
public class InterpreterTestChildClass extends InterpreterTestClass implements InterpreterTestInterface {
    @Override
    public int virtual_overriden(int a, int b, int c) {
        return a + b + c;
    }

    public int invokespecial_super(int a, int b, int c) {
        return super.virtual_overriden(a, b, c);  //always returns 3!
    }

    public int interfacemethod(int a, int b) {
        return a + b;
    }
}
