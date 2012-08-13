/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/**
 * Everything in this subtree is related to the logging of advice-generated data using datatypes that can be stored
 * outside of the VM without any special mechanisms (e.g. object serialialization). The
 * {@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLog} interface defines an essentially parallel API to
 * {@link com.oracle.max.vm.ext.vma.VMAdviceHandler} but using {@link String} and scalar data types. The majority of the
 * interface is automatically generated from {@link com.oracle.max.vm.ext.vma.VMAdviceHandler} by
 * {@link com.oracle.max.vm.ext.vma.tools.gen.VMAdviceHandlerLogGenerator}, so can easily be regenerated if necessary.
 * This strategy of automatic generation is used throughout the system and the generators can be found in
 * {@code com.oracle.max.vm.ext.vma.tools.gen} subpackages.
 * <p>
 * The actual candidate handlers are in packages suffixed by {@code h}
 * to distinguish them from the support code. E.g., {@link com.oracle.max.vm.ext.vma.handlers.log.sync.h.SyncLogVMAdviceHandler}.
 * In addition the convention is to name them using the pattern {@code NameVMAdviceHandler}.
 * <p>
 * The choice of log implementation is made by {@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLogFactory} and
 * defaults to {@link com.oracle.max.vm.ext.vma.handlers.log.txt.sbps.SBPSCompactTextVMAdviceHandlerLog}, which uses a compact
 * textual representation and buffers output using a {@link java.lang.StringBuilder}, flushing periodically to a file
 * that is chosen with {@link com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLogFile} and defaults to
 * {@value com.oracle.max.vm.ext.vma.handlers.log.VMAdviceHandlerLogFile#DEFAULT_LOGFILE}.
 */
package com.oracle.max.vm.ext.vma.handlers.log;
