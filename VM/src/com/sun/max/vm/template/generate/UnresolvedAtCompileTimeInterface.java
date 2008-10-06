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
/*VCSID=14d437ba-71f2-44b0-a5d9-21887f31df49*/
package com.sun.max.vm.template.generate;

import com.sun.max.annotate.*;


/**
 * The UnresolvedInterfaceAtCompileTime interface is used for generating template that makes no assumption about the initialization/loading state of a classes.
 * The interface is placed in a package that escapes the package loader when building a VM prototype, so that when templates are
 * generated, the UnresolvedAtCompileTime class is seen as not loaded nor initialized by the prototype's optimizing compiler.
 * 
 * @author Laurent Daynes
 */
@PROTOTYPE_ONLY
public interface UnresolvedAtCompileTimeInterface {
    void parameterlessUnresolvedInterfaceMethod();
}
