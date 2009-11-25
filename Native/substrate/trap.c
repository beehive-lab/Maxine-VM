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
 * @author Paul Caprioli
 */

#if !os_GUESTVMXEN
#   include <signal.h>
#   include <stdlib.h>
#   include <string.h>
#   include <sys/ucontext.h>
#   include <unistd.h>
#endif

#include "threads.h"
#include "virtualMemory.h"
#include "log.h"
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
#define STACK_FATAL 2
#define ILLEGAL_INSTRUCTION 3
#define ARITHMETIC_EXCEPTION 4
#define ASYNC_INTERRUPT 5

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

static Address theJavaTrapStub;
static boolean traceTraps = false;

#if os_GUESTVMXEN
#define SignalHandlerFunction fault_handler_t
#else
typedef void (*SignalHandlerFunction)(int signal, SigInfo *signalInfo, void *ucontext);
#endif

void setHandler(int signal, SignalHandlerFunction handler) {
#if os_GUESTVMXEN
	guestvmXen_register_fault_handler(signal, handler);
#else

    struct sigaction newSigaction;
    struct sigaction oldSigaction;

    memset((char *) &newSigaction, 0, sizeof(newSigaction));
    sigemptyset(&newSigaction.sa_mask);
    newSigaction.sa_flags = SA_SIGINFO | SA_RESTART | SA_ONSTACK;
#if os_SOLARIS || os_LINUX || os_DARWIN
    if (signal == SIGUSR1) {
        newSigaction.sa_flags = SA_SIGINFO | SA_ONSTACK;
    }
#endif
    newSigaction.sa_sigaction = handler;

    if (sigaction(signal, &newSigaction, &oldSigaction) != 0) {
        log_exit(1, "sigaction failed");
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
        c_UNIMPLEMENTED();
#endif
}

static void setInstructionPointer(UContext *ucontext, Address stub) {
#if os_SOLARIS
#   if isa_SPARC
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
    c_UNIMPLEMENTED();
#endif
}

#if 0
static Address getStackPointer(UContext *ucontext) {
#if os_SOLARIS
  return ucontext->uc_mcontext.gregs[REG_SP];
#elif os_LINUX
#   if isa_AMD64
        return ucontext->uc_mcontext.gregs[REG_RSP];
#   elif isa_IA32
        return ucontext->uc_mcontext.gregs[REG_UESP];
#   else
        c_UNIMPLEMENTED();
#   endif
#elif os_DARWIN
    return ucontext->uc_mcontext->__ss.__rsp;
#elif os_GUESTVMXEN
        return ucontext->rsp;
#else
        c_UNIMPLEMENTED();
#endif
}
#endif

static Address getFaultAddress(SigInfo * sigInfo, UContext *ucontext) {
#if (os_DARWIN || os_SOLARIS || os_LINUX )
    return (Address) sigInfo->si_addr;
#elif (os_GUESTVMXEN)
    return (Address) sigInfo;
#endif
}

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

static void blueZoneTrap(NativeThreadLocals ntl) {
#if os_GUESTVMXEN
	guestvmXen_blue_zone_trap(ntl);
#endif
}

static void globalSignalHandler(int signal, SigInfo *signalInfo, UContext *ucontext) {
    int primordial = 0;
    char *sigName;
    if (traceTraps || log_TRAP) {
        sigName = signalName(signal);
        if (sigName == NULL) {
            sigName = "<unknown>";
        }
        log_println("SIGNAL: %0d [%s]", signal, sigName);
    }
    ThreadLocals tl = threadLocals_current();
    NativeThreadLocals ntl = nativeThreadLocals_current();
    if (ntl == 0) {
        log_exit(-22, "could not find native thread locals in trap handler");
    }
    int trapNumber = getTrapNumber(signal);
    Address faultAddress = getFaultAddress(signalInfo, ucontext);
    ThreadLocals disabled_tl = getThreadLocal(ThreadLocals, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS);

    if (getThreadLocal(int, tl, ID) == 0) {
        sigName = signalName(signal);
        if (sigName == NULL) {
            sigName = "<unknown>";
        }
        log_println("Trap taken on primordial thread (this is usually bad)!");
        log_println("thread handle=%p, id=%d: %s", thread_self(), getThreadLocal(int, disabled_tl, ID), sigName);
        log_println("trapInfo[0] (trap number)         = %p", trapNumber);
        log_println("trapInfo[1] (instruction pointer) = %p", getInstructionPointer(ucontext));
        log_println("trapInfo[2] (fault address)       = %p", faultAddress);
        log_println("trapInfo[3] (safepoint latch)     = %p", getThreadLocal(Address, disabled_tl, TRAP_LATCH_REGISTER));
        log_println("Java trap stub 0x%0lx", theJavaTrapStub);
        primordial = 1;
    }

    if (disabled_tl == 0) {
        log_exit(-21, "could not find disabled VM thread locals in trap handler");
    }

    if (faultAddress >= ntl->stackRedZone && faultAddress < ntl->stackBase + ntl->stackSize && !primordial) {
        if (faultAddress < ntl->stackYellowZone) {
            /* The faultAddress is in the red zone; we shouldn't be alive */
            virtualMemory_unprotectPages(ntl->stackRedZone, STACK_RED_ZONE_PAGES);
            trapNumber = STACK_FATAL;
        } else if (faultAddress < ntl->stackYellowZone + virtualMemory_getPageSize()) {
            /* the faultAddress is in the yellow zone; assume this is a stack fault. */
            virtualMemory_unprotectPages(ntl->stackYellowZone, STACK_YELLOW_ZONE_PAGES);
            trapNumber = STACK_FAULT;
        } else {
            blueZoneTrap(ntl);
            return;
        }
    }

    /* save the trap information in the disabled VM thread locals */
    setThreadLocal(disabled_tl, TRAP_NUMBER, trapNumber);
    setThreadLocal(disabled_tl, TRAP_INSTRUCTION_POINTER, getInstructionPointer(ucontext));
    setThreadLocal(disabled_tl, TRAP_FAULT_ADDRESS, faultAddress);

#if os_SOLARIS && isa_SPARC
	/* save the value of the safepoint latch at the trapped instruction */
    setThreadLocal(disabled_tl, TRAP_LATCH_REGISTER, ucontext->uc_mcontext.gregs[REG_G2]);
    /* set the safepoint latch register of the trapped frame to the disabled state */
    ucontext->uc_mcontext.gregs[REG_G2] = (Address) disabled_tl;
#elif isa_AMD64 && (os_SOLARIS || os_LINUX)
    setThreadLocal(disabled_tl, TRAP_LATCH_REGISTER, ucontext->uc_mcontext.gregs[REG_R14]);
    ucontext->uc_mcontext.gregs[REG_R14] = (Address) disabled_tl;
#elif isa_AMD64 && os_DARWIN
    setThreadLocal(disabled_tl, TRAP_LATCH_REGISTER, ucontext->uc_mcontext->__ss.__r14);
    ucontext->uc_mcontext->__ss.__r14 = (Address) disabled_tl;
#elif isa_AMD64 && os_GUESTVMXEN
    setThreadLocal(disabled_tl, TRAP_LATCH_REGISTER, ucontext->r14);
    ucontext->r14 = (Address) disabled_tl;
#else
    c_UNIMPLEMENTED();
#endif

    if (traceTraps || log_TRAP) {
        if (sigName != NULL) {
            log_println("thread handle=%p, id=%d: %s", thread_self(), getThreadLocal(int, disabled_tl, ID), sigName);
            log_println("trapInfo[0] (trap number)         = %p", getThreadLocal(Address, disabled_tl, TRAP_NUMBER));
            log_println("trapInfo[1] (instruction pointer) = %p", getThreadLocal(Address, disabled_tl, TRAP_INSTRUCTION_POINTER));
            log_println("trapInfo[2] (fault address)       = %p", getThreadLocal(Address, disabled_tl, TRAP_FAULT_ADDRESS));
            log_println("trapInfo[3] (safepoint latch)     = %p", getThreadLocal(Address, disabled_tl, TRAP_LATCH_REGISTER));
        }
        log_println("SIGNAL: returning to Java trap stub 0x%0lx", theJavaTrapStub);
    }
    setInstructionPointer(ucontext, theJavaTrapStub);
}

Address nativeInitialize(Address javaTrapStub) {
    theJavaTrapStub = javaTrapStub;
    setHandler(SIGSEGV, (SignalHandlerFunction) globalSignalHandler);
    setHandler(SIGBUS, (SignalHandlerFunction) globalSignalHandler);
    setHandler(SIGILL, (SignalHandlerFunction) globalSignalHandler);
    setHandler(SIGFPE, (SignalHandlerFunction) globalSignalHandler);
    setHandler(SIGUSR1, (SignalHandlerFunction) globalSignalHandler);
    return (Address) &traceTraps;
}


