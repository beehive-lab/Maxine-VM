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
 * JDK implementation support.
 * 
 * We substitute certain methods in the JDK with our own implementation in Java,
 * without touching any JDK source code.
 * For each class, in which this occurs, we create a parallel class,
 * in a parallel package structure underneath here.
 * We name the substitute class like the original class,
 * for easy recognition, but with with "_" appended for better distinction.
 * We annotate the substitute class with '@METHOD_ANNOTATIONS',
 * passing the original class as annotation parameter.
 * Then we annotate the methods in our substitute class,
 * which substitute original methods with the '@SUBSTITUTE' annotation.
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.vm.jdk;
