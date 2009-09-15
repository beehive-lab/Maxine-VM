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

#include "auth.h"
#include <Security/Authorization.h>

int acquireTaskportRight() {

    AuthorizationRef authorization;
    OSStatus status;

    status = AuthorizationCreate (NULL, kAuthorizationEmptyEnvironment, kAuthorizationFlagDefaults, &authorization);
    if (status != 0) {
        fprintf(stderr, "Error creating authorization reference\n");
        return -1;
    }
    AuthorizationItem right = { "system.privilege.taskport", 0, 0 , 0 };
    AuthorizationItem items[] = { right };
    AuthorizationRights rights = { sizeof(items) / sizeof(items[0]), items };
    AuthorizationFlags flags = kAuthorizationFlagInteractionAllowed | kAuthorizationFlagExtendRights | kAuthorizationFlagPreAuthorize;

    status = AuthorizationCopyRights (authorization, &rights, kAuthorizationEmptyEnvironment, flags, NULL);
    if (status != 0) {
        fprintf(stderr, "Error authorization current process for right to call task_for_pid\n");
        return -1;
    }
    return 0;
}

