/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * A manager for saving and restoring of settings between Inspection sessions, with some
 * specialized machinery to support the saving of window frame geometry (location, size)
 * for both the Inspector's main frame and for views within it.
 */
public class InspectionSettings {

    private static final int TRACE_VALUE = 2;

    private static final String VM_SETTINGS_KEY = "vm";
    private static final String VM_BOOT_IMAGE_FORMAT_VERSION_KEY = "bootImageFormatVersion";
    private static final String VM_BOOT_IMAGE_ID_KEY = "bootImageId";

    private static final String INSPECTOR_SETTINGS_KEY = "inspector";
    private static final String INSPECTOR_SETTINGS_VERSION_KEY = "settingsVersion";

    // This should be incremented any time that any of the persistent settings keys changes.
    private static final String INSPECTOR_SETTINGS_VERSION = "3";

    private static final String COMPONENT_X_KEY = "x";
    private static final String COMPONENT_Y_KEY = "y";
    private static final String COMPONENT_HEIGHT_KEY = "height";
    private static final String COMPONENT_WIDTH_KEY = "width";
    private static final Point defaultInspectorLocation = new Point(100, 100);

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
    public interface SaveSettingsListener {
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
         * The view responsible for the displaying this object in a GUI. If this object does not have a visual
         * presentation, then this method should return null.
         *
         * In addition to the other listener specific settings, the {@linkplain Component#getBounds() geometry} of the
         * {@linkplain #view() view} responsible for the displaying the object are automatically saved
         * whenever the inspection settings are {@linkplain InspectionSettings#save() persisted}. The inspection
         * settings are persisted any time the inspection is
         * {@linkplain ComponentListener#componentMoved(ComponentEvent) moved} or
         * {@linkplain ComponentListener#componentResized(ComponentEvent) resized} and whenever the component is made
         * {@linkplain ComponentListener#componentShown(ComponentEvent) visible}, its bounds are updated from the
         * settings.
         */
        InspectorView view();

        /**
         * @return geometry to apply to a newly shown component when there have been no geometry settings saved.
         */
        Rectangle defaultGeometry();
    }

    /**
     * A convenience class that simplifies implementing the {@link SaveSettingsListener} interface.
     */
    public abstract static class AbstractSaveSettingsListener implements SaveSettingsListener {
        protected final String name;
        protected final InspectorView view;
        protected final Rectangle defaultGeometry;

        private AbstractSaveSettingsListener(String name, InspectorView view, Rectangle defaultGeometry) {
            this.name = name;
            this.view = view;
            this.defaultGeometry = defaultGeometry;
        }

        public AbstractSaveSettingsListener(String name, InspectorView view) {
            this(name, view, null);
        }

        protected AbstractSaveSettingsListener(String name) {
            this (name, null, null);
        }

        public InspectorView view() {
            return view;
        }

        public String name() {
            return name;
        }

        public Rectangle defaultGeometry() {
            return defaultGeometry;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private final Inspection inspection;

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
            InspectorWarning.message(inspection, tracePrefix() + "Error while loading settings from " + settingsFile, ioException);
        }

        // Check that the settings keys haven't changed since the previous session;
        // if so warn and ignore the old settings.
        final SaveSettingsListener inspectorClient = new AbstractSaveSettingsListener(INSPECTOR_SETTINGS_KEY) {
            public void saveSettings(SaveSettingsEvent settings) {
                settings.save(INSPECTOR_SETTINGS_VERSION_KEY, String.valueOf(INSPECTOR_SETTINGS_VERSION));
            }
        };
        addSaveSettingsListener(inspectorClient);
        final int settingsVersion = get(inspectorClient, INSPECTOR_SETTINGS_VERSION_KEY, OptionTypes.INT_TYPE, 0);
        if (settingsVersion != Integer.valueOf(INSPECTOR_SETTINGS_VERSION)) {
            InspectorWarning.message(inspection, tracePrefix() + "Settings in obsolete format ignored");
            properties.clear();
        }

        // Check to see if the boot image being inspected is the same as the previous inspection session.
        final BootImage bootImage = inspection.vm().bootImage();
        final SaveSettingsListener bootimageClient = new AbstractSaveSettingsListener(VM_SETTINGS_KEY) {
            public void saveSettings(SaveSettingsEvent settings) {
                settings.save(VM_BOOT_IMAGE_FORMAT_VERSION_KEY, String.valueOf(bootImage.header.bootImageFormatVersion));
                settings.save(VM_BOOT_IMAGE_ID_KEY, String.valueOf(bootImage.header.randomID));
            }
        };
        addSaveSettingsListener(bootimageClient);
        final int version = get(bootimageClient, VM_BOOT_IMAGE_FORMAT_VERSION_KEY, OptionTypes.INT_TYPE, 0);
        final int randomID = get(bootimageClient, VM_BOOT_IMAGE_ID_KEY, OptionTypes.INT_TYPE, 0);
        bootImageChanged = version != bootImage.header.bootImageFormatVersion || randomID != bootImage.header.randomID;
        bootimageClient.saveSettings(new SaveSettingsEvent(bootimageClient, properties));

        saver = new Saver();
    }

    /**
     * Add a listener that will be notified when a "save event" is triggered, so that settings
     * can be saved.
     * <p>
     * If the listener has an associated {@link AbstractView}, then additional services are provided
     * to automate the saving and restoring of window frame geometry (size, location):
     * <ol>
     * <li>During the execution of this method call, the view's frame is positioned and
     * sized according to the following search:
     * <ul>
     * <li>Use saved geometry settings for this view, if they exist;</li>
     * <li>If no settings saved, then use default geometry for this kind of view, if it exists;</li>
     * <li>Otherwise use a generic default geometry.</li>
     * </ul>
     * </li>
     * <li>The listener has associated code that will automatically save the view's geometry
     * upon every "save event".</li>
     * </ol>
     * @param saveSettingsListener a listener for events that should cause important settings to be
     * saved
     */
    public synchronized void addSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        final SaveSettingsListener oldClient = clients.put(saveSettingsListener.name(), saveSettingsListener);
        assert oldClient == null || oldClient == saveSettingsListener;

        final InspectorView view = saveSettingsListener.view();
        if (view != null) {
            view.getJComponent().addComponentListener(new ComponentListener() {
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

    public synchronized void removeSaveSettingsListener(final SaveSettingsListener saveSettingsListener) {
        clients.remove(saveSettingsListener.name());
    }

    /**
     * Repositions a view's location from its previous settings, or
     * if none, from the default.  Ensures that the ultimate location is visible.
     *
     * @param saveSettingsListener a listener that has an associated view
     */
    private void repositionInspectorFromSettings(SaveSettingsListener saveSettingsListener) {
        final InspectorView view = saveSettingsListener.view();
        final Rectangle oldGeometry = view.getJComponent().getBounds();
        Rectangle newGeometry = saveSettingsListener.defaultGeometry();
        // Check to see if we have geometry settings for this component.
        // We used to check to see if X location was set, with a default of -1 meaning
        // "not set", but we then discovered some apparently legitimate minus
        // values (for reasons unknown) on the Darwin platform (at least).

        if (get(saveSettingsListener, COMPONENT_WIDTH_KEY, OptionTypes.INT_TYPE, -1) >= 0) {
            newGeometry = new Rectangle(
                Math.max(get(saveSettingsListener, COMPONENT_X_KEY, OptionTypes.INT_TYPE, oldGeometry.x), 0),
                Math.max(get(saveSettingsListener, COMPONENT_Y_KEY, OptionTypes.INT_TYPE, oldGeometry.y), 0),
                get(saveSettingsListener, COMPONENT_WIDTH_KEY, OptionTypes.INT_TYPE, oldGeometry.width),
                get(saveSettingsListener, COMPONENT_HEIGHT_KEY, OptionTypes.INT_TYPE, oldGeometry.height));
        }
        if (newGeometry == null) {
            view.getJComponent().setLocation(defaultInspectorLocation);
        } else if (!newGeometry.equals(oldGeometry)) {
            view.getJComponent().setBounds(newGeometry);
        }
        inspection.gui().moveToFullyVisible(view);
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
     * and should be saved to a {@linkplain #settingsFile file}. The actual writing
     * to the file happens asynchronously.
     */
    public void save() {
        needsSaving = true;
    }

    /**
     * Writes the persistent settings represented by this object to a {@linkplain #settingsFile file}.
     */
    private synchronized void doSave() {
        updateSettings();
        try {
            final FileWriter fileWriter = new FileWriter(settingsFile);
            properties.store(fileWriter, null);
            fileWriter.close();
        } catch (IOException ioException) {
            InspectorWarning.message(inspection, tracePrefix() + "Error while saving settings to " + settingsFile, ioException);
        }

    }

    private void updateSettings() {
        final Properties newProperties = new SortedProperties();
        Trace.line(TRACE_VALUE, tracePrefix() + "saving settings to: " + settingsFile.toString());
        for (SaveSettingsListener saveSettingsListener : clients.values()) {
            final SaveSettingsEvent saveSettingsEvent = new SaveSettingsEvent(saveSettingsListener, newProperties);
            saveSettingsListener.saveSettings(saveSettingsEvent);
            final InspectorView view = saveSettingsListener.view();
            if (view != null) {

                final Rectangle geometry = view.getJComponent().getBounds();
                saveSettingsEvent.save(COMPONENT_X_KEY, geometry.x);
                saveSettingsEvent.save(COMPONENT_Y_KEY, geometry.y);
                saveSettingsEvent.save(COMPONENT_WIDTH_KEY, geometry.width);
                saveSettingsEvent.save(COMPONENT_HEIGHT_KEY, geometry.height);
                Trace.line(TRACE_VALUE, tracePrefix() + "saving locn=(" + geometry.x + "," + geometry.y + "),size=(" + geometry.width + "," + geometry.height + ") for " + view.getClass().getSimpleName());

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
            InspectorWarning.message(inspection, "Failed to write settings to console", e);
        }
    }
}
