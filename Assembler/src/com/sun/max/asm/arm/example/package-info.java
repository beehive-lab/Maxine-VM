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
/**
 * This package presents an example of how to generate an assembler for a subset of the instructions in an ISA
 * specification. This example is in the context of the ARM ISA specification but the mechanism can be just as easily
 * applied to any other ISA.
 * <p>
 * The approach is to use an interface that specifies a subset of the methods generated in the complete assembler for
 * the ISA in question. The interface could be written by hand or produced using the "extract interface" refactoring
 * available in many modern Java IDEs. In this example, we used Eclipse to extract the
 * {@link ExampleARMAssemblerSpecification} interface from a random subset of the methods in {@link ARMRawAssembler} and
 * {@link ARMLabelAssembler}. Next, the class into which the assembler methods will be generated must be written.
 * The only constraint on this class (which respect to generation of the assembler methods) is that it has these
 * delimiter lines some where within its declaration:
 * <pre>
 * // START GENERATED RAW ASSEMBLER METHODS
 * // END GENERATED RAW ASSEMBLER METHODS
 * 
 * // START GENERATED LABEL ASSEMBLER METHODS
 * // END GENERATED LABEL ASSEMBLER METHODS
 * </pre>
 * In this example, the {@link ExampleARMAssembler} serves this purpose.
 * <p>
 * Lastly, the {@linkplain ARMAssemblerGenerator ARM assembler generator}
 * needs to be run with program arguments specifying which class to update and what
 * interface specifies the ISA subset. In this example, the execution of the
 * generator is done programatically by {@link ExampleARMAssemblerSpecification.Generator}.
 * 
 * @author Doug Simon
 */
package com.sun.max.asm.arm.example;

