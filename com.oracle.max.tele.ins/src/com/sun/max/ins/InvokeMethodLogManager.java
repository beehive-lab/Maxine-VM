/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;

import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.util.*;
import com.sun.max.program.option.*;

/**
 * Singleton manager for the Inspector's method invocation log.
 * <br>
 * TODO This was cloned from {@link NotePadManager} and should be refactored.
 */
public final class InvokeMethodLogManager extends AbstractInspectionHolder {

    // Key used for making data persistent
    private static final String PREFIX_KEY = "invokeMethodLogContents";
    private static final String DEFAULT_INVOKE_METHOD_KEY = "default";
    private static final String EMPTY_CONTENTS = "<empty>";

    /**
     * A simple invoke method log implementation that holds text and can
     * save its contents with other persistent inspection settings.
     *
     */
    private final class InvokeMethodLogImpl extends AbstractSaveSettingsListener implements InspectorInvokeMethodLog {
        private final InspectionSettings inspectionSettings;
        private final InvokeMethodLogManager invokeMethodLogManager;
        private final String key;

        // Contents are never null until disposed, at which point the log is useless.
        private String contents = EMPTY_CONTENTS;

        public InvokeMethodLogImpl(InspectionSettings inspectionSettings, InvokeMethodLogManager invokeMethodLogManager, String key) {
            super(PREFIX_KEY);
            this.inspectionSettings = inspectionSettings;
            this.invokeMethodLogManager = invokeMethodLogManager;
            this.key = key;
            // Register with the persistence service; must do this before load.
            inspectionSettings.addSaveSettingsListener(this);
            // Load contents of this invoke log, if exists, from persistent settings
            contents = inspectionSettings.get(this, key, OptionTypes.STRING_TYPE, EMPTY_CONTENTS);
        }

        public String getName() {
            return key;
        }

        public String getContents() {
            return contents;
        }

        public void setContents(String contents) throws IllegalArgumentException {
            if (contents == null) {
                throw new IllegalArgumentException();
            }
            this.contents = contents;
            inspectionSettings.save();
        }

        public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            // Should not be receiving save events if disposed.
            assert contents != null;
            saveSettingsEvent.save(key, contents);
        }

        public void dispose() {
            assert contents != null;
            inspectionSettings.removeSaveSettingsListener(this);
            this.contents = null;
            invokeMethodLogManager.dispose(this);
        }
    }

    // Only a single log supported at present
    private final InvokeMethodLogImpl defaultInvokeMethodLog;

    public InvokeMethodLogManager(Inspection inspection) {
        super(inspection);
        // Create the default log, a singleton for now, but there could be more.
        this.defaultInvokeMethodLog = new InvokeMethodLogImpl(inspection.settings(), this, DEFAULT_INVOKE_METHOD_KEY);
    }

    /**
     * @return the default log.
     */
    public InspectorInvokeMethodLog getInvokeMethodLog() {
        return defaultInvokeMethodLog;
    }

    /**
     * Notifies this manager that a invoke log has been disposed and should be forgotten.
     *
     * @param invokeMethodLog the invoke log being disposed.
     */
    private void dispose(InspectorInvokeMethodLog invokeMethodLog) {
        InspectorError.unimplemented("invoke method log disposal not yet supported");
        // Remove the associated entry from the current inspection settings
    }

}
