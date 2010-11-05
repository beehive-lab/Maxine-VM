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

#ifndef __auth_h__
#define __auth_h__ 1

/**
 * Attempts to acquire the system.privilege.taskport right for the current process. This
 * right is required for using the task_for_pid() system call.
 *
 * TODO: While the acquisition of this right appears to succeed (according to /var/log/secure.log),
 * the call to task_for_pid() still fails. Until we can ascertain what extra steps are need to
 * authorize the Inspector process for use of this system call, the Inspector must be run as root.
 *
 * @see /etc/authorization
 * @see man taskgated
 */
extern int acquireTaskportRight();

#endif /* __auth_h__ */
