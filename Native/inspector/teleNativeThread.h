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
 * @author Doug Simon
 */
#ifndef __teleNativeThread_h__
#define __teleNativeThread_h__ 1

/*
 * This enum must be kept in sync the one of the same named defined in com.sun.max.tele.debug.TeleNativeThread.java.
 */
typedef enum ThreadState {
	TS_MONITOR_WAIT,
	TS_NOTIFY_WAIT,
	TS_JOIN_WAIT,
	TS_SLEEPING,
	TS_BREAKPOINT,
	TS_SUSPENDED,
	TS_DEAD,
	TS_RUNNING
} ThreadState_t;

#endif /*__teleNativeThread_h__*/
