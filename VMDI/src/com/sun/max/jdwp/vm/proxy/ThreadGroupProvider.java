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
/*VCSID=a7ecff6e-5c0c-4bb3-8f61-57afaea7d728*/
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a group of thread. This is a construct that is in the JDWP protocol and many Java IDEs (e.g. NetBeans, Eclipse) support grouped views of
 * thread based on this concept.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface ThreadGroupProvider extends ObjectProvider {

    /**
     * @return the display name of the thread group
     */
    @ConstantReturnValue
    String getName();

    /**
     * @return the parent thread group or null if this is a top level thread group
     */
    ThreadGroupProvider getParent();

    /**
     * @return an array of child thread groups
     */
    ThreadGroupProvider[] getThreadGroupChildren();

    /**
     * @return an array of threads that are children of this thread group
     */
    ThreadProvider[] getThreadChildren();
}
