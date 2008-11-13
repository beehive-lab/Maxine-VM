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
/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */

#if !os_GUESTVMXEN
#   include <signal.h>
#   include <stdlib.h>
#   include <string.h>
#   include <ucontext.h>
#   include <unistd.h>
#endif

#include "threads.h"
#include "virtualMemory.h"
#include "debug.h"
#include "jni.h"
#include "os.h"
#include "isa.h"
#include "image.h"

#if os_SOLARIS && isa_SPARC
    /* Get STACK_BIAS definition for Solaris / SPARC */
#      include <sys/stack.h>
       typedef struct rwindow * RegisterWindow;
#endif

#if os_GUESTVMXEN
#   include <guestvmXen.h>
#else
    typedef ucontext_t UContext;
    typedef siginfo_t SigInfo;
#endif

/*
 * Important: The values defined here must correspond to those of the same name
 *            defined in the TrapNumber class in Trap.java.
 */
#define MEMORY_FAULT 0
#define STACK_FAULT 1
#define ILLEGAL_INSTRUCTION 2
#define ARITHMETIC_EXCEPTION 3
#define ASYNC_INTERRUPT 4

int getTrapNumber(int signal) {
    switch (signal) {
    case SIGSEGV:
    case SIGBUS:
        return MEMORY_FAULT;
    case SIGILL:
        return ILLEGAL_INSTRUCTION;
    case SIGFPE:
        return ARITHMETIC_EXCEPTION;
    case SIGUSR1:
        return ASYNC_INTERRUPT;
    }
    return signal;
}

static Address _javaTrapStub;

void setHandler(int signal, void *handler) {
#if os_GUESTVMXEN
    register_fault_handler(signal, (fault_handler_t)handler);
#else

    struct sigaction newSigaction;
    struct sigaction oldSigaction;

    memset((char *) &newSigaction, 0, sizeof(newSigaction));
    sigemptyset(&newSigaction.sa_mask);
    newSigaction.sa_flags = SA_SIGINFO | SA_RESTART | SA_ONSTACK;
    newSigaction.sa_sigaction = handler;
    newSigaction.sa_handler = handler;

    if (sigaction(signal, &newSigaction, &oldSigaction) != 0) {
        debug_exit(1, "sigaction failed");
    }
#endif
}

static Address getInstructionPointer(UContext *ucontext) {
#if os_SOLARIS
    return ucontext->uc_mcontext.gregs[REG_PC];
#elif os_LINUX
#   if isa_AMD64
    return ucontext->uc_mcontext.gregs[REG_RIP];
#   elif isa_IA32
    return ucontext->uc_mcontext.gregs[REG_EIP];
#   endif
#elif os_DARWIN
    return ucontext->uc_mcontext->__ss.__rip;
#elif os_GUESTVMXEN
        return ucontext->rip;
#else
#   error Unimplemented
#endif
}

static void setInstructionPointer(UContext *ucontext, Address stub) {
#if os_SOLARIS
#   if isa_SPARC
     /* Make the stub look like it is called by the method that caused the exception.
      * Stubs create a frame on entry. To enable stack walking we need to make believe
      * that a stub was properly called from the method that caused the exception.
      * We set O7 to the current's PC before (O7 is temp and is not expected to survive
      * any implicit exception.
      */
  /* ucontext->uc_mcontext.gregs[REG_O7] = ucontext->uc_mcontext.gregs[REG_PC];
      ucontext->uc_mcontext.gregs[REG_O7] = 0; */
    ucontext->uc_mcontext.gregs[REG_nPC] = (greg_t) (stub + 4);
#   endif
    ucontext->uc_mcontext.gregs[REG_PC] = (greg_t) stub;
#elif os_DARWIN
    ucontext->uc_mcontext->__ss.__rip = stub;
#elif os_LINUX
#   if isa_AMD64
     ucontext->uc_mcontext.gregs[REG_RIP] = (greg_t) stub;
#   elif isa_IA32
     ucontext->uc_mcontext.gregs[REG_EIP] = (greg_t) stub;
#   endif
#elif os_GUESTVMXEN
    ucontext->rip = (unsigned long) stub;
#else
#   error Unimplemented
#endif
}

static Address getFramePointer(UContext *ucontext) {
#if os_SOLARIS
#   if isa_SPARC /* 64-bit SPARC*/
        Address sp = ucontext->uc_mcontext.gregs[REG_SP];
        RegisterWindow rwin = (RegisterWindow) (sp + STACK_BIAS);
        Address fp = (Address) rwin->rw_fp;
        return fp;
#   elif isa_AMD64 || isa_IA32
        return ucontext->uc_mcontext.gregs[REG_FP];
#   else
#       error Unimplemented
#   endif
#elif os_LINUX
#   if isa_AMD64
        return ucontext->uc_mcontext.gregs[REG_RBP];
#   elif isa_IA32
        return ucontext->uc_mcontext.gregs[REG_EBP];
#   else
#       error Unimplemented
#   endif
#elif os_DARWIN
    return ucontext->uc_mcontext->__ss.__rbp;
#elif os_GUESTVMXEN
    return ucontext->rbp;
#else
#   error Unimplemented
#endif
}

static Address getStackPointer(UContext *ucontext) {
#if os_SOLARIS
  return ucontext->uc_mcontext.gregs[REG_SP];
#elif os_LINUX
#   if isa_AMD64
        return ucontext->uc_mcontext.gregs[REG_RSP];
#   elif isa_IA32
        return ucontext->uc_mcontext.gregs[REG_UESP];
#   else
#       error Unimplemented
#   endif
#elif os_DARWIN
    return ucontext->uc_mcontext->__ss.__rsp;
#elif os_GUESTVMXEN
        return ucontext->rsp;
#else
#   error Unimplemented
#endif
}

static Address getFaultAddress(SigInfo * sigInfo, UContext *ucontext) {
#if (os_DARWIN || os_SOLARIS || os_LINUX )
    return (Address) sigInfo->si_addr;
#elif (os_GUESTVMXEN)
    return (Address) sigInfo;
#endif
}

#if debug_TRAP
char *signalName(int signal) {
    switch (signal) {
    case SIGSEGV: return "SIGSEGV";
    case SIGFPE: return "SIGFPE";
    case SIGILL: return "SIGILL";
    case SIGUSR1: return "SIGUSR1";
    case SIGBUS: return "SIGBUS";
    }
    return NULL;
}
#endif

static int isInGuardZone(Address address, Address zoneBegin) {
    /* return true if the address is in the zone, or is within N pages of the zone */
    return address >= zoneBegin && (address - zoneBegin) < ((1 + STACK_GUARD_PAGES) * getPageSize());
}

static void globalSignalHandler(int signal, SigInfo *signalInfo, UContext *ucontext) {
#if debug_TRAP
    debug_println("SIGNAL: %0d", signal);
#endif
    thread_Specifics *threadSpecifics = (thread_Specifics *) thread_currentSpecifics();
    if (threadSpecifics == 0) {
        debug_exit(-22, "could not find native thread locals in trap handler");
    }

    Address disabledVmThreadLocals = threadSpecifics->disabledVmThreadLocals;

    if (disabledVmThreadLocals == 0) {
        debug_exit(-21, "could not find disabled VM thread locals in trap handler");
    }

    int trapNumber = getTrapNumber(signal);
    Word *stackPointer = (Word *)getStackPointer(ucontext);

    if (isInGuardZone((Address)stackPointer, threadSpecifics->stackYellowZone)) {
        /* if the stack pointer is in the yellow zone, assume this is a stack fault */
        unprotectPage(threadSpecifics->stackYellowZone);
        trapNumber = STACK_FAULT;
    } else if (isInGuardZone((Address)stackPointer, threadSpecifics->stackRedZone)) {
        /* if the stack pointer is in the red zone, (we shouldn't be alive) */
        debug_exit(-20, "SIGSEGV: (stack pointer is in fatal red zone)");
    } else if (stackPointer == 0) {
        /* if the stack pointer is zero, (we shouldn't be alive) */
        debug_exit(-19, "SIGSEGV: (stack pointer is zero)");
    }

    /* save the trap information in the disabled vm thread locals */
    Address *trapInfo = (Address*) (disabledVmThreadLocals + image_header()->vmThreadLocalsTrapNumberOffset);
    trapInfo[0] = trapNumber;
    trapInfo[1] = getInstructionPointer(ucontext);
    trapInfo[2] = getFaultAddress(signalInfo, ucontext);
#if os_SOLARIS && isa_SPARC
	 /* set the safepoint latch register of the trapped frame to the disable state */
	 ucontext->uc_mcontext.gregs[REG_G2] = disabledVmThreadLocals;
#else
    /* note: overwrite the stack top with a pointer to the vm thread locals for the java stub to pick up */
    trapInfo[3] = (Address)*stackPointer;
    *stackPointer = (Word)disabledVmThreadLocals;
#endif

#if debug_TRAP
    char *sigName = signalName(signal);
    if (sigName != NULL) {
        debug_println("thread %d: %s (trapInfo @ %p)", threadSpecifics->id, sigName, trapInfo);
        debug_println("trapInfo[0] (trap number)         = %p", trapInfo[0]);
        debug_println("trapInfo[1] (instruction pointer) = %p", trapInfo[1]);
        debug_println("trapInfo[2] (fault address)       = %p", trapInfo[2]);
#if !(os_SOLARIS && isa_SPARC)
        debug_println("trapInfo[3] (stack top value)     = %p", trapInfo[3]);
#endif
    }
    debug_println("SIGNAL: returning to java trap stub 0x%0lx\n", _javaTrapStub);
#endif
    setInstructionPointer(ucontext, _javaTrapStub);
}

void nativeInitialize(Address javaTrapStub) {
    _javaTrapStub = javaTrapStub;
    setHandler(SIGSEGV, globalSignalHandler);
    setHandler(SIGBUS, globalSignalHandler);
    setHandler(SIGILL, globalSignalHandler);
    setHandler(SIGFPE, globalSignalHandler);
    setHandler(SIGUSR1, globalSignalHandler);
}


