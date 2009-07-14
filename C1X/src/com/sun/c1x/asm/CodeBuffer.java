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
package com.sun.c1x.asm;

/**
 * The <code>CodeBuffer</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class CodeBuffer {

    public enum Type {

        // Here is the list of all possible sections, in order of ascending address.
        SECT_INSTS, // Executable instructions.
        SECT_STUBS, // Outbound trampolines for supporting call sites.
        SECT_CONSTS; // Non-instruction data: Floats, jump tables, etc.

        // TODO: implement
        public final int value = 0;
    };

    int codeSize;

    public CodeSection insts() {
        // TODO Auto-generated method stub
        return null;
    }

    public OopRecorder oopRecorder() {
        // TODO Auto-generated method stub
        return null;
    }

    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    public void decode() {
        // TODO Auto-generated method stub

    }

    public BufferBlob blob() {
        // TODO Auto-generated method stub
        return null;
    }

    public CodeSection stubs() {
        // TODO Auto-generated method stub
        return null;
    }

    public CodeSection consts() {
        // TODO Auto-generated method stub
        return null;
    }

    public void blockComment(int offset, char comment) {
        // TODO Auto-generated method stub

    }

    public static int locator(int offset, int sect) {
        // TODO Auto-generated method stub
        return 0;
    }

    // TODO to be implemented
}
