/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

/**
 * Factory for controlling which implementation of {@link AdviceRecordFlusher} is
 * used at runtime.
 *
 */
public class AdviceRecordFlusherFactory {
    public static final String ADVICE_RECORD_FLUSHER_PROPERTY = "max.vma.record.flusher";

    public static AdviceRecordFlusher create() {
        AdviceRecordFlusher result = null;
        final String flusherClassName = System.getProperty(ADVICE_RECORD_FLUSHER_PROPERTY);
        if (flusherClassName == null) {
            result = new LoggingAdviceRecordFlusher();
        } else {
            try {
                result = (AdviceRecordFlusher) Class.forName(flusherClassName).newInstance();
            } catch (Exception exception) {
                System.err.println("Error instantiating " + flusherClassName
                        + ": " + exception);
            }

        }
        return result;
    }
}
