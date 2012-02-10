/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * This package implements a Header-Origin-Mixed (HOM) object layout.
 * <p>
 * Tuples object are packed for minimal space consumption, observing alignment
 * restrictions. They have a 2 word header and are layed out as shown below:
 *
 * <p><hr><blockquote><pre>
 *        cell --> +-------------+
 *                 |    misc     |  // monitor and hashcode
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
 *                 |    misc     |  // monitor and hashcode
 *                 +-------------+
 *                 |   class     |
 * origin/data --> +=============+
 *                 |             |
 *                 :  elements   :
 *                 |             |
 *                 +-------------+
 * </pre></blockquote><hr></p>
 *
 * The first word of the header (i.e. {@code misc} word for tuple objects and {@code length}
 * word for arrays) encodes the type of the cell (tuple or array) in its low-order bit.
 * If the bit is set, the cell contains an array otherwise it contains a tuple.
 * <p>
 * Unless stated otherwise, all offsets mentioned in this layout implementation are in terms of
 * bytes and are relative to the origin pointer.
 */
package com.sun.max.vm.layout.hom;
