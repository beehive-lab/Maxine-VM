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
/*VCSID=a711641f-4e78-4090-af51-b9c489c47245*/
/**
 * Representations of target machine code and associated objects.
 * 
 * A "target method" is a heap object aggregating all the parts and pieces constituting an executable binary of a Java method.
 * It is reachable via the corresponding class method actor's method dock.
 * 
 * Each target method has references to co-located objects with fixed addresses in a code region.
 * These objects contain: machine code, literals, auxiliary exception handling data and auxiliary trampoline data.
 * The code region allocation unit (cell) containing these objects is called a "target bundle".
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.vm.compiler.target;
