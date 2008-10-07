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
/*VCSID=34487ca6-8e09-499e-9c43-b252be899809*/
package test.com.sun.max.vm.prototype;

import com.sun.max.asm.*;
import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;

/**
 *
 * @author Doug Simon
 */
public class BinaryImageGeneratorTest extends MaxTestCase {

    public BinaryImageGeneratorTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BinaryImageGeneratorTest.class);
    }


    public void test_AMD64PRODUCTImage() {
        final String[] arguments = new String[]{
            "-trace=1",
            "-layout=" + new com.sun.max.vm.layout.ohm.Package(),
            "-os=" + OperatingSystem.LINUX,
            "-heap=" + new com.sun.max.vm.heap.sequential.semiSpace.Package(),
            "-align=4",
            "-compiler=" + new com.sun.max.vm.compiler.b.c.d.e.amd64.target.Package(),
            "-build=" + BuildLevel.PRODUCT,
            "-grip=" + new com.sun.max.vm.grip.direct.Package(),
            "-cpu=" + ProcessorModel.AMD64,
            "-page=4096",
            "-endianness=" + Endianness.LITTLE,
            "-reference=" + new com.sun.max.vm.reference.plain.Package(),
            "-isa=" + InstructionSet.AMD64,
            "-bits=" + WordWidth.BITS_64
        };
        new BinaryImageGenerator(arguments);
    }

    // Only one execution of the BinaryImageGenerator is possible per VM instance due to all the static state
    public void notest_AMD64DEBUGImage() {
        final String[] arguments = new String[]{
            "-trace=1",
            "-layout=" + new com.sun.max.vm.layout.ohm.Package(),
            "-os=" + OperatingSystem.LINUX,
            "-heap=" + new com.sun.max.vm.heap.sequential.semiSpace.Package(),
            "-align=4",
            "-compiler=" + new com.sun.max.vm.compiler.b.c.d.e.amd64.target.Package(),
            "-build=" + BuildLevel.DEBUG,
            "-grip=" + new com.sun.max.vm.grip.direct.Package(),
            "-cpu=" + ProcessorModel.AMD64,
            "-page=4096",
            "-endianness=" + Endianness.LITTLE,
            "-reference=" + new com.sun.max.vm.reference.plain.Package(),
            "-isa=" + InstructionSet.AMD64,
            "-bits=" + WordWidth.BITS_64
        };
        new BinaryImageGenerator(arguments);
    }
}
