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
/*VCSID=8f29a477-fe9b-4e56-b7ec-88c403c2c906*/
/**
 * @author Bernd Mathiske
 */
#ifndef __os_h__
#define __os_h__ 1

#include "c.h"

#define os_SOLARIS  0
#define os_LINUX    0
#define os_WINDOWS  0
#define os_DARWIN   0
#define os_GUESTVMXEN    0

#if defined(GUESTVMXEN)
#   undef os_GUESTVMXEN
#   define os_GUESTVMXEN   1
#elif defined(__sun) || defined(SOLARIS)
#   undef os_SOLARIS
#   define os_SOLARIS    1
#   define os_IDENTIFIER SOLARIS
#elif defined(linux) || defined(__linux) || defined(__linux__)
#   undef os_LINUX
#   define os_LINUX      1
#   define os_IDENTIFIER LINUX
#elif defined(__CYGWIN__) || defined(__CYGWIN32__) || defined(WINDOWS)
#   undef os_WINDOWS
#   define os_WINDOWS    1
#   define os_IDENTIFIER WINDOWS
#elif defined(__APPLE__)
#   undef os_DARWIN
#   define os_DARWIN     1
#   define os_IDENTIFIER DARWIN

#define os_STACK_ALIGNMENT    16

#else
#   error
#endif

#define os_STRING   STRINGIZE(os_IDENTIFIER)

#define os_HEADER_FILE_NAME(baseName) \
    STRINGIZE(os/os_IDENTIFIER/CONCATENATE(baseName##_, os_IDENTIFIER).h)

#endif /*__os_h__*/
