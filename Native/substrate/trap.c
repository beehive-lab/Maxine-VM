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

#if os_GUESTVMXEN
#   include <guestvmXen.h>
#else
    typedef ucontext_t UContext;
    typedef siginfo_t SigInfo;
#endif

void setHandler(int signal, void *handler);


static isa_OsSignalIntegerRegisters getIntegerRegisters(UContext *ucontext) {
#if os_SOLARIS || os_LINUX
    return (isa_OsSignalIntegerRegisters) &ucontext->uc_mcontext.gregs;
#elif os_GUESTVMXEN
    return ucontext;
#elif os_DARWIN
    return (isa_OsSignalIntegerRegisters) &ucontext->uc_mcontext->__ss;
#else
#   error Unimplemented
#endif
}

static isa_OsSignalFloatingPointRegisters getFloatingPointRegisters(UContext *ucontext) {
#if os_SOLARIS || os_LINUX
    return (isa_OsSignalFloatingPointRegisters) &ucontext->uc_mcontext.fpregs;
#elif os_GUESTVMXEN
    return ucontext;
#elif os_DARWIN
    return (isa_OsSignalFloatingPointRegisters) &ucontext->uc_mcontext->__fs;
#else
#error Unimplemented
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

#if os_SOLARIS && isa_SPARC
    /* Get STACK_BIAS definition for Solaris / SPARC */
#   include <sys/stack.h>
    typedef struct rwindow * RegisterWindow;
#endif

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

static Address getFaultAddress(SigInfo * sigInfo, UContext *ucontext) {
#if (os_DARWIN || os_SOLARIS || os_LINUX )
	return (Address) sigInfo->si_addr;
#elif (os_GUESTVMXEN)
	return (Address) sigInfo;
#endif
}

typedef Address (*JavaTrapHandler)(Address safepointLatch,
								   Address stackPointer,
                                   Address framePointer,
                                   Address instructionPointer,
                                   isa_CanonicalIntegerRegisters integerRegisters,
                                   isa_CanonicalFloatingPointRegisters floatingPointRegisters,
                                   Address faultAddress);

static JavaTrapHandler _segmentationFaultHandler;
static JavaTrapHandler _stackFaultHandler;
static JavaTrapHandler _divideByZeroHandler;
static JavaTrapHandler _illegalInstructionHandler;
static JavaTrapHandler _interruptHandler;
static JavaTrapHandler _busErrorHandler;

#if DEBUG_TRAP
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

static void trapHandler(int signal, SigInfo *signalInfo, UContext *ucontext) {
#if DEBUG_TRAP
	debug_println("SIGNAL: %0d", signal);
#endif
	isa_OsSignalCanonicalIntegerRegistersStruct integerRegisters;
	isa_CanonicalFloatingPointRegistersStruct floatingPointRegisters;
	Address instructionPointer = getInstructionPointer(ucontext);
	Address stackPointer = getStackPointer(ucontext);
	Address framePointer = getFramePointer(ucontext);
	Address faultAddress = getFaultAddress(signalInfo, ucontext);
	NativeThreadLocals *nativeThreadLocals = (NativeThreadLocals *) thread_getSpecific(nativeThreadLocalsKey);
    if (nativeThreadLocals == 0) {
        debug_exit(-22, "could not find native thread locals in trap handler");
    }

    Address triggeredVmThreadLocals = nativeThreadLocals->triggeredVmThreadLocals;

    if (triggeredVmThreadLocals == 0) {
        debug_exit(-22, "could not find triggered VM thread locals in trap handler");
    }

    isa_canonicalizeSignalIntegerRegisters(getIntegerRegisters(ucontext), &integerRegisters);
    isa_canonicalizeSignalFloatingPointRegisters(getFloatingPointRegisters(ucontext), &floatingPointRegisters);

#if DEBUG_TRAP
    char *sigName = signalName(signal);
    if (sigName != NULL) {
        debug_println("thread %d: %s @ %p, sp = %p, fault = %p", nativeThreadLocals->id, sigName, instructionPointer, stackPointer, faultAddress);
    }
#if DEBUG_TRAP_REGISTERS
    debug_println("Integer registers:");
    isa_printCanonicalIntegerRegisters(&integerRegisters);
#endif
#endif

    if (signal == SIGSEGV || signal == SIGBUS) {
        if (isInGuardZone(stackPointer, nativeThreadLocals->stackYellowZone)) { 
            debug_println("SIGSEGV: (stack fault in yellow zone)");
        	unprotectPage(nativeThreadLocals->stackYellowZone);
        	instructionPointer = (*_stackFaultHandler)(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, &integerRegisters, &floatingPointRegisters, faultAddress);
        } else if (isInGuardZone(stackPointer, nativeThreadLocals->stackRedZone)) { 
            debug_println("SIGSEGV: (stack fault in red zone)");
        	unprotectPage(nativeThreadLocals->stackRedZone);
        	instructionPointer = (*_stackFaultHandler)(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, &integerRegisters, &floatingPointRegisters, faultAddress);
        } else {
        	instructionPointer = (*_segmentationFaultHandler)(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, &integerRegisters, &floatingPointRegisters, faultAddress);
        }
    } else if (signal == SIGFPE) {
	instructionPointer = (*_divideByZeroHandler)(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, &integerRegisters, &floatingPointRegisters, faultAddress);
    } else if (signal == SIGILL) {
        instructionPointer = (*_illegalInstructionHandler)(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, &integerRegisters, &floatingPointRegisters, faultAddress);
    } else if (signal == SIGUSR1) {
    	/* TODO: why does handling this signal not work? */
    }

#if DEBUG_TRAP
    debug_println("SIGNAL: returning to stub 0x%0lx\n", instructionPointer);
#endif
    setInstructionPointer(ucontext, instructionPointer);
}

void native_setBusErrorHandler(JavaTrapHandler javaTrapHandler){
#if DEBUG_TRAP
    debug_println("set SIGBUS: 0x%0lx", javaTrapHandler);
#endif
    _busErrorHandler = javaTrapHandler;
    setHandler(SIGBUS, trapHandler);
}

void native_setSegmentationFaultHandler(JavaTrapHandler javaTrapHandler) {
#if DEBUG_TRAP
    debug_println("set SIGSEGV: 0x%0lx", javaTrapHandler);
#endif
	_segmentationFaultHandler = javaTrapHandler;
    setHandler(SIGSEGV, trapHandler);
}

void native_setStackFaultHandler(JavaTrapHandler javaTrapHandler) {
#if DEBUG_TRAP
    debug_println("set STACKHDLR: 0x%0lx", javaTrapHandler);
#endif
	_stackFaultHandler = javaTrapHandler;
    setHandler(SIGSEGV, trapHandler);
}

void native_setDivideByZeroHandler(JavaTrapHandler javaTrapHandler) {
#if DEBUG_TRAP
    debug_println("set SIGFPE: 0x%0lx", javaTrapHandler);
#endif
	_divideByZeroHandler = javaTrapHandler;
    setHandler(SIGFPE, trapHandler);
}

void native_setIllegalInstructionHandler(JavaTrapHandler javaTrapHandler) {
#if DEBUG_TRAP
    debug_println("set SIGILL: 0x%0lx", javaTrapHandler);
#endif
	_illegalInstructionHandler = javaTrapHandler;
    setHandler(SIGILL, trapHandler);
}

void native_setInterruptHandler(JavaTrapHandler javaTrapHandler) {
#if DEBUG_TRAP
    debug_println("set SIGUSR1: 0x%0lx", javaTrapHandler);
#endif
	_interruptHandler = javaTrapHandler;
#if !os_GUESTVMXEN
    setHandler(SIGUSR1, trapHandler);
#endif
}

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
