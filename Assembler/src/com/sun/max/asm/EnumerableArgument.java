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
/*VCSID=70754259-0f29-4d28-b285-b890b2eafa2b*/
package com.sun.max.asm;

import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 * @author David Liu
 */
public interface EnumerableArgument<Argument_Type extends Enum<Argument_Type> & EnumerableArgument<Argument_Type>> extends Enumerable<Argument_Type>, SymbolicArgument {
    /**
     * Example argument value that may be regularly associated with this type in examples. For example, the ESI register in IA32 is commonly used as a source index register.
     * @return example argument value, or null if there is no suitable example value
     */
    Argument_Type exampleValue();
}
