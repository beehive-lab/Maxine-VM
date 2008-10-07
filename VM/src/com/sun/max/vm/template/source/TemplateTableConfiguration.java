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
package com.sun.max.vm.template.source;

import com.sun.max.lang.*;


public class TemplateTableConfiguration {
    /**
     * Basic set of sources for a template table where templates make no assumptions.
     */
    public static final Class[] UNOPTIMIZED_TEMPLATE_SOURCES = new Class[]{UnoptimizedBytecodeTemplateSource.class, BranchBytecodeSource.class};
    public static final Class[] RESOLVED_TEMPLATE_SOURCES = new Class[]{ResolvedBytecodeTemplateSource.class, ResolvedInvokeTemplateSource.class, ResolvedFieldAccessTemplateSource.class };
    public static final Class[] INITIALIZED_TEMPLATE_SOURCES = new Class[]{InitializedBytecodeTemplateSource.class, InitializedStaticFieldAccessTemplateSource.class };
    public static final Class[] INSTRUMENTED_TEMPLATE_SOURCES = new Class[]{InstrumentedBytecodeSource.class, InstrumentedInvokeTemplateSource.class};
    public static final Class[] TRACED_TEMPLATE_SOURCES = new Class[]{TracedBytecodeTemplateSource.class};
    public static final Class[] OPTIMIZED_TEMPLATE_SOURCES = Arrays.append(
                                                             Arrays.append(
                                                             Arrays.append(
                                                             Arrays.append(UNOPTIMIZED_TEMPLATE_SOURCES, RESOLVED_TEMPLATE_SOURCES),
                                                             INITIALIZED_TEMPLATE_SOURCES),
                                                             INSTRUMENTED_TEMPLATE_SOURCES),
                                                             TRACED_TEMPLATE_SOURCES);

}
