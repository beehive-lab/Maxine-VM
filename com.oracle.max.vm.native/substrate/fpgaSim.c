#include "log.h"
#include "jni.h"
#include <stdlib.h>
#include <stdio.h>

extern void enableFPGA(int val);
extern int initialiseMemoryCluster();
extern int initialiseTimingModel();
extern int reportTimingCounters();
static FILE *simFile = (0);

void maxine_close() {
    if(simFile != (0)) {
        fclose(simFile);
    }
}

void init_FPGA_Sim() {
    initialiseMemoryCluster();
    enableFPGA(1);
    initialiseTimingModel();
}

void exit_FPGA_Sim() {
    reportTimingCounters();
    maxine_close();
}

jint maxine_fpga_instrumentation_buffer() {
#ifdef ENABLE_APT_SIM
    if (simPtr != 0) {
        log_exit(1, "Error: Multiple initializations of simptr in substrate!");
    }
    simPtr = (unsigned int *) malloc (sizeof(unsigned int) * 4096);
    *(simPtr + 1023) = (unsigned int) simPtr;
    return (jint) simPtr;
#else
    log_exit(1, "Error: Instrumentation for simulation implemented only for ARMV7 platforms!");
    return (jint) 0;
#endif
}

jint maxine_flush_instrumentation_buffer() {
#ifdef arm
    return (jint) maxine_fpga_instrumentation_buffer;
#else
    log_exit(1, "Error: Instrumentation for simulation implemented only for ARMV7 platforms!");
    return (jint) 0;
#endif
}
