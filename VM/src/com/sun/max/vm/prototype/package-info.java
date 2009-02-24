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
 * A "prototype" is a representation of the entirety of our VM before bootstrap,
 * in some other format than the eventual fully bootstrapped executable.
 * The PrototypeGenerator leads us through several prototype stages:
 *   1. JavaPrototype     - all configured VM packages loaded, actors created for all constituents
 *   2. CompiledPrototype - all "necessary" methods compiled
 *   3. GraphPrototype    - a collection containing every object that will be part of the boot image
 * 
 * An "image generator" writes a graph prototype to a "boot image".
 * We consider these variants:
 * - BinaryImageGenerator: creates a binary image file that will be mmap-ped by the substrate
 * - MemoryImageGenerator: creates a binary image in main memory - for testing purposes
 * 
 * A boot image is a binary representation of the VM,
 * including all objects and all precompiled machine code.
 * 
 * @see PrototypeGenerator
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
package com.sun.max.vm.prototype;
