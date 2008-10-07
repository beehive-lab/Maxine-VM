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
/*VCSID=bb2fbe63-ba3f-43ca-91e9-2b1563bee96d*/
/**
 * This package implements an Origin-Header-Mixed (OHM) object layout.
 * <p>
 * Tuples object are packed for minimal space consumption, observing alignment
 * restrictions. They have a 2 word header and are layed out as shown below:
 * 
 * <p><blockquote><pre>
 * cell/origin --> +-------------+
 *                 |    class    |
 *                 +-------------+
 *                 |   extras    |  // monitor and hashcode
 *        data --> +=============+
 *                 |             |
 *                 :   fields    :  // mixed reference and scalar data
 *                 |             |
 *                 +-------------+
 * </pre></blockquote></p>
 * 
 * Array objects have a 3 word header and are layed out as shown below:
 * 
 * <p><blockquote><pre>
 * cell/origin --> +-------------+
 *                 |    class    |
 *                 +-------------+
 *                 |   extras    |  // monitor and hashcode
 *                 +-------------+
 *                 |   length    |
 *        data --> +=============+
 *                 |             |
 *                 :  elements   :
 *                 |             |
 *                 +-------------+
 * </pre></blockquote></p>
 * 
 * Unless stated otherwise, all offsets mentioned in this layout implementation are in terms of
 * bytes and are relative to the origin pointer.
 * 
 * TODO This description of an OHM layout is only currently true for a 32-bit system. It needs to be updated
 *      for a 64-bit system where the 'extras' and 'length' components (which are always 4 bytes) are
 *      packed into a single 8 byte word.
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.vm.layout.ohm;
