/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ci;

import com.sun.c1x.ri.RiMethod;

/**
 * This class represents a code site (i.e. a chain of inlined methods with bytecode
 * locations) that is communicated from the compiler to the runtime system. A code site
 * can be used by the runtime system to reconstruct a source-level stack trace
 * for exceptions and to create stack frames for deoptimization (switching from
 * optimized code to interpreted code).
 *
 * @author Ben L. Titzer
 */
public class CiCodeSite {
    /**
     * The site where this site has been inlined, {@code null} if none.
     */
    public final CiCodeSite parent;

    /**
     * The runtime interface method for this site.
     */
    public final RiMethod method;

    /**
     * The location within the method, as a bytecode index. The constant
     * {@code -1} may be used to indicate the location is unknown, for example
     * within code synthesized by the compiler.
     */
    public final int bci;

    /**
     * Constructs a new site with the given site as the parent, the given method, and the given
     * bytecode index.
     * @param parent the parent site
     * @param method the method
     * @param bci the bytecode index within the method
     */
    public CiCodeSite(CiCodeSite parent, RiMethod method, int bci) {
        this.parent = parent;
        this.method = method;
        this.bci = bci;
    }

    /**
     * Converts this code site to a string representation.
     * @return a string representation of this code site
     */
    @Override
    public String toString() {
        return append(new StringBuilder(100)).toString();
    }

    private StringBuilder append(StringBuilder buf) {
        if (parent != null) {
            parent.append(buf);
            buf.append(" -> ");
        }
        buf.append(method);
        buf.append("@");
        buf.append(bci);
        return buf;
    }
}
