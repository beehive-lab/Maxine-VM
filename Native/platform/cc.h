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
/*VCSID=93ceb100-8edc-4652-9fc2-77cdf94203e2*/
/**
 * @author Bernd Mathiske
 */
#ifndef __cc_h__
#define __cc_h__ 1

#include "c.h"

#define cc_GNU  0
#define cc_SUN  0

#ifdef __GNUC__
#   undef cc_GNU
#   define cc_GNU        1
#   define cc_IDENTIFIER gnu
#elif defined(sun)
#   undef cc_SUN
#   define cc_SUN        1
#   define cc_IDENTIFIER sun
#else
#   error
#endif

#if cc_SUN
#   define cc_HEADER_FILE_NAME(baseName) \
        STRINGIZE(CONCATENATE(baseName##_sun).h)
#else
#   define cc_HEADER_FILE_NAME(baseName) \
        STRINGIZE(CONCATENATE(baseName##_, cc_IDENTIFIER).h)
#endif

#endif /*__cc_h__*/
