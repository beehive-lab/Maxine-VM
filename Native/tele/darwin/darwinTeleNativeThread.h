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
#ifndef __darwinTeleNativeThread_h__
#define __darwinTeleNativeThread_h__ 1

#include <mach/mach_types.h>
#include <mach/mach_error.h>
#include <mach/mach_init.h>
#include <mach/mach_vm.h>
#include <mach/vm_map.h>

#include "isa.h"

#if isa_AMD64
#   include <mach/x86_64/thread_act.h>
//#   include <mach/i386/thread_status.h>

#   define INTEGER_REGISTER_COUNT x86_THREAD_STATE64_COUNT
#   define STATE_REGISTER_COUNT x86_THREAD_STATE64_COUNT
#   define FLOATING_POINT_REGISTER_COUNT x86_FLOAT_STATE64_COUNT
#   define THREAD_STATE_COUNT x86_THREAD_STATE64_COUNT

#   define INTEGER_REGISTER_FLAVOR x86_THREAD_STATE64
#   define STATE_REGISTER_FLAVOR x86_THREAD_STATE64
#   define FLOAT_REGISTER_FLAVOR x86_FLOAT_STATE64
#   define THREAD_STATE_FLAVOR x86_THREAD_STATE64

    typedef _STRUCT_X86_THREAD_STATE64 OsIntegerRegistersStruct;
    typedef _STRUCT_X86_THREAD_STATE64 OsStateRegistersStruct;
    typedef _STRUCT_X86_FLOAT_STATE64 OsFloatingPointRegistersStruct;
    typedef x86_thread_state64_t ThreadState;
#else
#   error "Only x64 is supported on Darwin for now"
#endif

extern boolean thread_read_registers(thread_t thread,
        isa_CanonicalIntegerRegistersStruct *canonicalIntegerRegisters,
        isa_CanonicalFloatingPointRegistersStruct *canonicalFloatingPointRegisters,
        isa_CanonicalStateRegistersStruct *canonicalStateRegisters);


#endif /*__darwinTeleNativeThread_h__*/
