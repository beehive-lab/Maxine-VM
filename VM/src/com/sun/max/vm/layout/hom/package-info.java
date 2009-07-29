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
 * This package implements a Header-Origin-Mixed (HOM) object layout.
 * <p>
 * Tuples object are packed for minimal space consumption, observing alignment
 * restrictions. They have a 2 word header and are layed out as shown below:
 * 
 * <p><hr><blockquote><pre>
 *        cell --> +-------------+
 *                 |   extras    |  // monitor and hashcode
 *                 +-------------+
 *                 |   class     |
 * origin/data --> +=============+
 *                 |             |
 *                 :   fields    :  // mixed reference and scalar data
 *                 |             |
 *                 +-------------+
 * </pre></blockquote><hr></p>
 * 
 * Array objects have a 3 word header and are layed out as shown below:
 * 
 * <p><hr><blockquote><pre>
 *        cell --> +-------------+
 *                 |   length    |
 *                 +-------------+
 *                 |   extras    |  // monitor and hashcode
 *                 +-------------+
 *                 |   class     |
 * origin/data --> +=============+
 *                 |             |
 *                 :  elements   :
 *                 |             |
 *                 +-------------+
 * </pre></blockquote><hr></p>
 * 
 * The first word of the header (i.e. {@code extras} word for tuple objects and {@code length}
 * word for arrays) encodes the type of the cell (tuple or array) in its low-order bit.
 * If the bit is set, the cell containes an array otherwise it contains a tuple.
 * <p>
 * Unless stated otherwise, all offsets mentioned in this layout implementation are in terms of
 * bytes and are relative to the origin pointer.
 * 
 * @author Doug Simon
 */
package com.sun.max.vm.layout.hom;
