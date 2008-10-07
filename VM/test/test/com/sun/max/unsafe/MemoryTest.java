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
/*VCSID=bca85594-aa21-4fcf-9423-fb82cf86f740*/
package test.com.sun.max.unsafe;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;

public class MemoryTest extends WordTestCase {

    public MemoryTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MemoryTest.class);
    }

    public void test_allocate() {
        Pointer pointer = Memory.allocate(_sizeLow);
        assertTrue(pointer.compareTo(_address0) > 0);
        switch (wordWidth()) {
            case BITS_64:
                assertTrue(pointer.toLong() <= Long.MAX_VALUE - _low);
                break;
            case BITS_32:
                assertTrue(pointer.toLong() <= Integer.MAX_VALUE - _low);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
        Memory.deallocate(pointer);

        pointer = Memory.mustAllocate(_size0);
        assertTrue(pointer.compareTo(_address0) > 0);
        Memory.deallocate(pointer);

        pointer = Memory.mustAllocate(_size1);
        assertTrue(pointer.compareTo(_address0) > 0);
        Memory.deallocate(pointer);

        try {
            pointer = Memory.mustAllocate(_sizeMax);
        } catch (OutOfMemoryError outOfMemoryError) {
        }
    }

    private void check_badDeallocate(Pointer badPointer) {
        try {
            Memory.deallocate(badPointer);
            fail();
        } catch (IllegalArgumentException illegalArgumentException) {
        }
    }

    public void notest_deallocate() {
        check_badDeallocate(Pointer.zero());
        check_badDeallocate(Pointer.fromInt(128 * Ints.M)); // nothing allocated yet

        final int n = 100;
        final Pointer[] pointers = new Pointer[n];
        for (int i = 0; i < n; i++) {
            final Pointer pointer = Memory.mustAllocate(Size.fromInt((i * 991) + 4));
            pointers[i] = Memory.mustAllocate(Size.fromInt((i * 507) + 3));
            check_badDeallocate(pointer.plus(4));
            Memory.deallocate(pointer);
        }
        check_badDeallocate(Pointer.fromInt(12345));
        check_badDeallocate(_addressMax.asPointer());
        for (int j = 0; j < n; j++) {
            check_badDeallocate(pointers[j].plus(100));
            Memory.deallocate(pointers[j]);
        }
    }

    public void test_copyBytes() {
    }

}
