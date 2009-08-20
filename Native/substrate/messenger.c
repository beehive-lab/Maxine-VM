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
#include <stdlib.h>

#include "image.h"
#include "word.h"

#include "messenger.h"

#define DATA_SIZE (256 * 1024)

/**
 * Creates and initializes the memory block for a ring buffer.
 * The first two words must be pointers to the third word.
 *
 * @see RingBufferPipe.java
 */
static void *createRingBufferData() {
	Address *data = (Address *) malloc(DATA_SIZE);
	Address buffer = (Address) &data[2];
	data[0] = buffer;
	data[1] = buffer;
	return data;
}

static int _debugger_attached = false;

void messenger_initialize() {
    messenger_Info info = image_read_value(messenger_Info, messengerInfoOffset);
	if (info != 0) {
		info = malloc(sizeof(messenger_InfoStruct));
		info->dataSize = (Size) DATA_SIZE;
		info->inData = (Address) createRingBufferData();
		info->outData = (Address) createRingBufferData();
		image_write_value(messenger_Info, messengerInfoOffset, info);
		_debugger_attached = true;
	}
}

int debugger_attached() {
    return _debugger_attached;
}
