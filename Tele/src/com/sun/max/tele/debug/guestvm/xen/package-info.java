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
 * @author Grzegorz Milos
 * @author Mick Jordan
 *
 * The Guest VM specific implementation the "tele" layer for the Maxine Inspector.
 * Several implementations are provided and selected at runtime by {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.GuestVMXenDBChannel}.
 * <ul>
 * <li>Direct connection via {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.db.DBProtocol}.</li>
 * <li>Indirection connection via TCP using {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.tcp.TCPProtocol}, to an agent running in dom0
 * using {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.db.DBProtocol}.</li>
 * <li>Indirection connection via TCP using {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.tcp.TCPProtocol}, to an agent running in dom0
 * using the "gdbsx" agwnt (TBD).
 * <li>Connection to a Xen dump file using {@link com.sun.max.tele.debug.guestvm.xen.dbchannel.dump.DumpProtocol}.</li>
 * </ul>
 */
package com.sun.max.tele.debug.guestvm.xen;
