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
/*VCSID=123ebba5-714e-4bf3-825a-6f12f17da28e*/
package com.sun.max.vm.classfile.constant;

/**
 * A key that represents a constant pool constant in a map.
 * <p>
 * The recursive generic type definition in this type and in {@link PoolConstant} basically ensure that each "real" pool
 * constant type (i.e. a non-generic type that extends or implements {@link PoolConstant}) defines its own key type.
 * <p>
 * A useful paper describing these kind of recursive types can be found at http://www.ejournal.unam.mx/compuysistemas/vol07-02/CYS07205.pdf
 * 
 * @author Doug Simon
 */
public interface PoolConstantKey<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> {
}
