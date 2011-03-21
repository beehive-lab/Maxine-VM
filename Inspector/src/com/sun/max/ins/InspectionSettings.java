/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.hosted.*;

/**
 * Manages saving and restoring of settings between Inspection sessions.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public class InspectionSettings {

    private static final int TRACE_VALUE = 2;

    private static final String BOOT_IMAGE_FORMAT_VERSION_KEY = "bootImageFormatVersion";
    private static final String BOOT_IMAGE_ID_KEY = "bootImageId";
    private static final String COMPONENT_X_KEY = "x";
    private static final String COMPONENT_Y_KEY = "y";
    private static final String COMPONENT_HEIGHT_KEY = "height";
    private static final String COMPONENT_WIDTH_KEY = "width";

    private String tracePrefix() {
        return "[InspectionSettings] ";
    }

    /**
     * @return a unique key uniquely identifying a key belonging to a client that stores and retrieves settings.
     */
    private String makeClientKey(SaveSettingsListener saveSettingsListener, String key) {
        return saveSettingsListener.name() + "." + key;
    }

    /**
     * An event requesting that a {@link SaveSettingsListener} write its settings to a persistent store. The
     * event object includes a method for a listener to {@linkplain #save(String, Object) save} its settings.
     */
    public final class SaveSettingsEvent {
        private final Properties props;
        private final SaveSettingsListener client;

        /**
         * Creates an object that will populate a given properties object with the settings of a given client.
         *
         * @param saveSettingsListener a settings listener that will save it settings via this object
         * @param properties the properties object into which the settings will be written
         */
        private SaveSettingsEvent(SaveSettingsListener saveSettingsListener, Properties properties) {
            props = properties;
            client = saveSettingsListener;
        }

        /**
         * Saves a value for a setting corresponding to a given name.
         *
         * @param key the name of the setting
         * @param value the value of the setting. The value saved is the result of calling {@link String#valueOf(Object)} on this value.
         */
        public void save(String key, Object value) {
            final String clientKey = makeClientKey(client, key);
            props.setProperty(clientKey, String.valueOf(value));
        }
    }

    /**
     * An object that uses settings to configure itself and therefore wants to participate in the saving of settings to
     * a persistent store.
     */
    public static interface SaveSettingsListener {
        /**
         * Notifies this object that it should save it settings.
         *
         * @param saveSettingsEvent an object to which this client should write its persistent settings
         */
        void saveSettings(SaveSettingsEvent saveSettingsEvent);

        /**
         * Gets a name for this client that is unique amongst all other clients. This name forms the
         * prefix of the key under which any persistent setting of this client is saved.
         */
        String name();

        /**
         * The Inspector responsible for the displaying this object in a GUI. If this object does not have a visual
         * presentation, then this method should return null.
         *
         * In addition to the other listener specific settings, the {@linkplain Component#getBounds() geometry} of the
         * {@linkplain #inspector() inspector} responsible for the displaying the object are automatically saved
         * whenever the inspection settings are {@linkplain InspectionSettings#save() persisted}. The inspection
         * settings are persisted any time the inspection is
         * {@linkplain ComponentListener#componentMoved(ComponentEvent) moved} or
         * {@linkplain ComponentListener#componentResized(ComponentEvent) resized} and whenever the component is made
         * {@linkplain ComponentListener#componentShown(ComponentEvent) visible}, its bounds are updated from the
         * settings.
         */
        Inspector inspector();

        /**
         * @return geometry to apply to a newly shown component when there have been no geometry settings saved.
         */
        Rectangle defaultBounds();
    }

    /**
     * A convenience class that simplifies implementing the {@link SaveSettingsListener} interface.
     */
    public abstract static class AbstractSaveSettingsListener implements SaveSettingsListener {
        protected final String name;
        protected final Inspector inspector;
        protected final Rectangle defaultBounds;

        private AbstractSaveSettingsListener(String name, Inspector inspector, Rectangle defaultBounds) {
            this.name = name;
            this.inspector = inspector;
            this.defaultBounds = defaultBounds;
        }

        public AbstractSaveSettingsListener(String name, Inspector inspector) {
            this(name, inspector, null);
        }

        protected AbstractSaveSettingsListener(String name) {
            this (name, null, null);
        }

        public Inspector inspector() {
            return inspector;
        }

        public String name() {
            return name;
        }

        public Rectangle defaultBounds() {
            return defaultBounds;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private final Inspection inspection;
    private final SaveSettingsListener bootimageClient;
    private final Properties properties = new SortedProperties();
    private final File settingsFile;
    private final boolean bootImageChanged;
    private final Map<String, SaveSettingsListener> clients;

    public InspectionSettings(Inspection inspection, File settingsFile) {
        this.inspection = inspection;
        this.settingsFile = settingsFile;
        clients = new IdentityHashMap<String, SaveSettingsListener>();
        try {
            final FileReader fileReader = new FileReader(settingsFile);
            Trace.begin(1, tracePrefix() + "loading preferences from: " + settingsFile.toString());
            properties.load(fileReader);
            Trace.end(1, tracePrefix() + "loading preferences from: " + settingsFile.toString());
            fileReader.close();
        } catch (FileNotFoundException ioException) {
        } catch (IOException ioException) {
            InspectorWarning.message(tracePrefix() + "Error while loading settings from " + settingsFile, ioException);
        }

        final BootImage bootImage = inspection.vm().bootImage();
        bootimageClient = new AbstractSaveSettingsListener("bootimage") {
            public void saveSettings(SaveSettingsEvent settings) {
                settings.save(BOOT_IMAGE_FORMAT_VERSION_KEY, String.valueOf(bootImage.header.bootImageFormatVersion));
                settings.save(BOOT_IMAGE_ID_KEY, String.valueOf(bootImage.header.randomID));
            }
        };

        addSaveSettingsListener(bootimageClient);
        final int version = get(bootimageClient, BOOT_IMAGE_FORMAT_VERSION_KEY, OptionTypes.INT_TYPE, 0);
        final int randomID = get(bootimageClient, BOOT_IMAGE_ID_KEY, OptionTypes.INT_TYPE, 0);
        bootImageChanged = version != bootImage.header.bootImageFormatVersion || randomID != bootImage.header.randomID;
        bootimageClient.saveSettings(new SaveSettingsEvent(bootimageClient, properties));
        saver = new Saver();
    }

    public void addSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        final SaveSettingsListener oldClient = clients.put(saveSettingsListener.name(), saveSettingsListener);
        assert oldClient == null || oldClient == saveSettingsListener;

        final Inspector inspector = saveSettingsListener.inspector();
        if (inspector != null) {
            inspector.getJComponent().addComponentListener(new ComponentListener() {
                public void componentHidden(ComponentEvent e) {
                }
                public void componentMoved(ComponentEvent e) {
                    save();
                }
                public void componentResized(ComponentEvent e) {
                    save();
                }
                public void componentShown(ComponentEvent e) {
                    repositionInspectorFromSettings(saveSettingsListener);
                }
            });
            repositionInspectorFromSettings(saveSettingsListener);
        }
    }

    public void removeSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        clients.remove(saveSettingsListener.name());
    }

    /**
     * Repositions an Inspector's location from its previous settings, or
     * if none, from the default.  Ensures that the ultimate location is visible.
     *
     * @param saveSettingsListener a listener that has an associated Inspector
     */
    private void repositionInspectorFromSettings(SaveSettingsListener saveSettingsListener) {
        final Inspector inspector = saveSettingsListener.inspector();
        final Rectangle oldBounds = inspector.getJComponent().getBounds();
        Rectangle newBounds = saveSettingsListener.defaultBounds();
        // Check to see if we have geometry settings for this component.
        // We used to check to see if X location was set, with a default of -1 meaning
        // "not set", but we then discovered some apparently legitimate minus
        // values (for reasons unknown) on the Darwin platform (at least).

        if (get(saveSettingsListener, COMPONENT_WIDTH_KEY, OptionTypes.INT_TYPE, -1) >= 0) {
            newBounds = new Rectangle(
                Math.max(get(saveSettingsListener, COMPONENT_X_KEY, OptionTypes.INT_TYPE, oldBounds.x), 0),
                Math.max(get(saveSettingsListener, COMPONENT_Y_KEY, OptionTypes.INT_TYPE, oldBounds.y), 0),
                get(saveSettingsListener, COMPONENT_WIDTH_KEY, OptionTypes.INT_TYPE, oldBounds.width),
                get(saveSettingsListener, COMPONENT_HEIGHT_KEY, OptionTypes.INT_TYPE, oldBounds.height));
        }
        if (newBounds != null && !newBounds.equals(oldBounds)) {
            inspector.getJComponent().setBounds(newBounds);
        }
        // We've forced locations to be >=0, even if on some platforms (Darwin?) we sometimes see
        // negative locations when a window is positioned close to either the left or top border.
        // The following test checks to see if the Inspector is completely within the boundaries
        // of the Inspector's frame, and those negative values would make it appear to be outside.
        inspection.gui().moveToMiddleIfNotVisble(inspector);
    }

    /**
     * Determines if the boot image identified in the file from which these settings were loaded
     * is different from the boot image of the current VM.
     */
    public boolean bootImageChanged() {
        return bootImageChanged;
    }

    /**
     * Gets the value for a setting corresponding to a given name.
     *
     * @param key the name of the setting for which the value is required
     * @param type an object that encodes the (boxed) type of the value as well as the capability for parsing a string
     *            into a value of the type
     * @param defaultValue the value to return if there is not value in this object corresponding to {@code key}
     * @throws Option.Error if there is an error parsing the string representation of the value into a value of the
     *             expected type
     */
    public <Value_Type> Value_Type get(SaveSettingsListener saveSettingsListener, String key, Option.Type<Value_Type> type, Value_Type defaultValue) {
        if (!clients.containsKey(saveSettingsListener.name())) {
            throw new IllegalArgumentException("Unregistered settings client: " + saveSettingsListener.name());
        }
        final String clientKey = makeClientKey(saveSettingsListener, key);
        final String value = properties.getProperty(clientKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            return type.parseValue(value);
        } catch (Option.Error optionError) {
            throw new Option.Error(String.format("Problem occurred while parsing %s for %s setting:%n%s", clientKey, value, optionError.getMessage()));
        }
    }

    private boolean needsSaving;

    /**
     * A helper task that is run in a separate thread for writing the persistent settings to a file.
     * Performing this task asynchronously means that a number of changes can be batched to the
     * file.
     */
    private class Saver implements Runnable {
        public void run() {
            while (!done) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (!done && needsSaving) {
                    doSave();
                    needsSaving = false;
                }
            }
        }

        boolean done;
        void quit() {
            done = true;
            doSave();
        }

        Saver() {
            new Thread(this, "SettingsSaver").start();
        }
    }

    private final Saver saver;

    /**
     * Determines if there is a setting in this object named by a given key.
     */
    public boolean containsKey(SaveSettingsListener saveSettingsListener, String key) {
        final String clientKey = makeClientKey(saveSettingsListener, key);
        return properties.containsKey(clientKey);
    }

    public synchronized void quit() {
        saver.quit();
    }

    /**
     * Indicates that the persistent settings represented by this object have changed
     * and should be saved to a {@linkplain #settingsFile() file}. The actual writing
     * to the file happens asynchronously.
     */
    public void save() {
        needsSaving = true;
    }

    /**
     * Writes the persistent settings represented by this object to a {@linkplain #settingsFile() file}.
     */
    private synchronized void doSave() {
        updateSettings();
        try {
            final FileWriter fileWriter = new FileWriter(settingsFile);
            properties.store(fileWriter, null);
            fileWriter.close();
        } catch (IOException ioException) {
            InspectorWarning.message(tracePrefix() + "Error while saving settings to " + settingsFile, ioException);
        }

    }

    private void updateSettings() {
        final Properties newProperties = new SortedProperties();
        Trace.line(TRACE_VALUE, tracePrefix() + "saving settings to: " + settingsFile.toString());
        for (SaveSettingsListener saveSettingsListener : clients.values()) {
            final SaveSettingsEvent saveSettingsEvent = new SaveSettingsEvent(saveSettingsListener, newProperties);
            saveSettingsListener.saveSettings(saveSettingsEvent);
            final Inspector inspector = saveSettingsListener.inspector();
            if (inspector != null) {
                final Rectangle bounds = inspector.getJComponent().getBounds();
                saveSettingsEvent.save(COMPONENT_X_KEY, bounds.x);
                saveSettingsEvent.save(COMPONENT_Y_KEY, bounds.y);
                saveSettingsEvent.save(COMPONENT_WIDTH_KEY, bounds.width);
                saveSettingsEvent.save(COMPONENT_HEIGHT_KEY, bounds.height);
            }
        }
        properties.putAll(newProperties);
    }

    /**
     * Writes a summary, in alphabetical order, of the current settings being saved.
     */
    public void writeSummary(PrintStream printStream) {
        updateSettings();
        try {
            properties.store(printStream, "Inspection settings");
        } catch (IOException e) {
            InspectorWarning.message("Failed to write settings to console", e);
        }
    }
}
