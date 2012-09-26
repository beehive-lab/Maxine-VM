/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import static com.sun.max.tele.MaxProcessState.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.ins.view.*;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;

// TODO (mlvdv) try to make columns narrow
// TODO (mlvdv) Parameter for object search extent

/**
 * An view that displays the contents of a region of memory in the VM, word aligned, one word per row.
 */
public final class MemoryView extends AbstractView<MemoryView> {

    private static final int TRACE_VALUE = 2;
    private static final ViewKind VIEW_KIND = ViewKind.MEMORY;
    private static final String SHORT_NAME = "Memory";
    private static final String LONG_NAME = "Memory View";
    private static final String UNKNOWN_REGION_NAME = "unknown region";

    private static MemoryViewManager viewManager;

    public static MemoryViewManager makeViewManager(Inspection inspection) {
        if (viewManager == null) {
            viewManager = new MemoryViewManager(inspection);
        }
        return viewManager;
    }

    public static final class MemoryViewManager extends AbstractMultiViewManager<MemoryView> implements MemoryViewFactory {

        private final InspectorAction interactiveMakeViewAction;
        private final List<InspectorAction> makeViewActions;

        private final class InteractiveViewRegionInfoByAddressAction extends InspectorAction {
            InteractiveViewRegionInfoByAddressAction() {
                super(inspection(),  "View RegionInfo for address...");
            }

            @Override
            protected void procedure() {
                new AddressInputDialog(inspection(), Address.zero(), "View RegionInfo for address...", "View") {
                    @Override
                    public void entered(Address address) {
                        MaxMemoryManagementInfo info = vm().getMemoryManagementInfo(address);
                        // TODO: revisit this.
                        if (info.status().equals(MaxMemoryStatus.LIVE)) {
                            final MaxObject object = info.tele();
                            focus().setHeapObject(object);
                        } else {
                            gui().errorMessage("Heap Region Info not found for address "  + address.to0xHexString());
                        }
                    }
                };
            }
        }

        protected MemoryViewManager(final Inspection inspection) {
            super(inspection, VIEW_KIND, SHORT_NAME, LONG_NAME);
            Trace.begin(1, tracePrefix() + "initializing");
            interactiveMakeViewAction = new InspectorAction(inspection(), "View memory at address...") {

                @Override
                protected void procedure() {
                    new AddressInputDialog(inspection, inspection.vm().bootImageStart(), "View memory at address...", "View") {

                        @Override
                        public void entered(Address address) {
                            final AbstractMemoryRegion newRegion = new FixedMemoryRegion(inspection, null, address, inspection.vm().platform().nBytesInWord() * 10);
                            makeView(newRegion, null).highlight();
                        }
                    };
                }
            };
            makeViewActions = new ArrayList<InspectorAction>(1);
            makeViewActions.add(interactiveMakeViewAction);
            Trace.end(1, tracePrefix() + "initializing");
        }

        public MemoryView makeView(MaxMemoryRegion memoryRegion, String regionName) {
            final MemoryView memoryView = new MemoryView(inspection(), memoryRegion, regionName, memoryRegion.start(), ViewMode.WORD, null);
            notifyAddingView(memoryView);
            return memoryView;
        }

        public MemoryView makeView(MaxObject object) {
            MemoryView memoryView = null;
            final TeleFixedMemoryRegion objectMemoryRegion = object.objectMemoryRegion();
            if (objectMemoryRegion == null) {
                gui().warningMessage("Unable to determine memory occupied by object");
            }  else {
                memoryView = new MemoryView(inspection(), objectMemoryRegion, null, object.origin(), object.status().isDead() ? ViewMode.WORD : ViewMode.OBJECT, null);
                notifyAddingView(memoryView);
            }
            return memoryView;
        }

        public MemoryView makeView(Address address) {
            final MemoryView memoryView = new MemoryView(inspection(), new FixedMemoryRegion(inspection(), "", address, vm().platform().nBytesInWord()), null, address, ViewMode.WORD, null);
            notifyAddingView(memoryView);
            return memoryView;
        }

        public MemoryView makePageView(Address address) {
            final MemoryView memoryView = new MemoryView(inspection(), new FixedMemoryRegion(inspection(), "", address, vm().platform().nBytesInPage()), null, address, ViewMode.PAGE, null);
            notifyAddingView(memoryView);
            return memoryView;
        }

        public InspectorAction makeViewAction() {
            return interactiveMakeViewAction;
        }

        public InspectorAction makeViewAction(final MaxMemoryRegion memoryRegion, final String regionName, String actionTitle) {
            final InspectorAction inspectorAction = new InspectorAction(inspection(), actionTitle == null ? "View memory for \"" + regionName + "\"" : actionTitle) {

                @Override
                protected void procedure() {
                    makeView(memoryRegion, regionName);
                }
            };
            inspectorAction.setEnabled(memoryRegion.isAllocated());
            return inspectorAction;
        }

        public InspectorAction makeViewAction(final MaxObject object, String actionTitle) {
            return new InspectorAction(inspection(), actionTitle == null ? "View memory" : actionTitle) {

                @Override
                protected void procedure() {
                    makeView(object);
                }
            };
        }

        public InspectorAction makeViewAction(final Address address, String actionTitle) {
            return new InspectorAction(inspection(), actionTitle == null ? "View memory" : actionTitle) {

                @Override
                protected void procedure() {
                    if (vm().state().findMemoryRegion(address) == null) {
                        final String message = "Address " + address.to0xHexString() + " not in any known memory allocation, try anyway?";
                        if (!gui().yesNoDialog(message)) {
                            return;
                        }
                    }
                    makeView(address);
                }
            };
        }

        @Override
        protected List<InspectorAction> makeViewActions() {
            return makeViewActions;
        }

    }


    public static enum ViewMode {
        WORD("Word", "Grows the visible region a word at a time and  navigates to the new location",
            "Grow the visible region upward (lower address) by one word", "Grow the visible region downward (higher address) by one word"),
        OBJECT("Obj", "Move to next/previous object origin",
            "View memory for previous object", "View memory for next object"),
        PAGE("Page", "Move to next/previous page origin and display entire page",
            "View previous memory page", "View next memory page");

        private final String label;
        private final String description;
        private final String previousToolTip;
        private final String nextToolTip;

        /**
         * @param label the label that identifies the mode
         * @param description description of the mode
         * @param previousToolTip description of the move backwards action in this mode
         * @param nextToolTip description of the move forward action in this mode
         */
        private ViewMode(String label, String description, String previousToolTip, String nextToolTip) {
            this.label = label;
            this.description = description;
            this.previousToolTip = previousToolTip;
            this.nextToolTip = nextToolTip;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public String previousToolTip() {
            return previousToolTip;
        }

        public String nextToolTip() {
            return nextToolTip;
        }

    }

    private static MemoryViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing these tables..
     */
    public static MemoryViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MemoryViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String MEMORY_COLUMN_PREFERENCE = "memoryViewColumn";

    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    private static class MemoryViewPreferences extends TableColumnVisibilityPreferences<MemoryColumnKind> {

        private final MemoryView memoryView;

        /**
         * Creates global preferences for this view.
         */
        private MemoryViewPreferences(Inspection inspection) {
            super(inspection, MEMORY_COLUMN_PREFERENCE, MemoryColumnKind.values());
            this.memoryView = null;
        }

        /**
         * A per-instance set of view preferences, initialized to the global preferences.
         * @param globalPreferences the global defaults for this kind of view
         * @param memoryView the view
         */
        public MemoryViewPreferences(MemoryViewPreferences globalPreferences, MemoryView memoryView) {
            super(globalPreferences);
            this.memoryView = memoryView;
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }

        @Override
        public void setIsVisible(MemoryColumnKind columnKind, boolean visible) {
            super.setIsVisible(columnKind, visible);
            if (memoryView != null) {
                memoryView.reconstructView();
            }
        }

        @Override
        public MemoryViewPreferences clone() {
            return new MemoryViewPreferences(this, memoryView);
        }
    }

    private final int nBytesInWord;
    private final int nBytesInPage;
    private final int nWordsInPage;

    //  View specifications from when the view was created
    private final MemoryWordRegion originalMemoryWordRegion;
    private final ViewMode originalViewMode;
    private final String originalRegionName;
    private final Address originalOrigin;

    // Current view specifications.
    private MemoryWordRegion memoryWordRegion;

    /**
     * The name of the region to display.  If {@code null}, always use the name of the region.  If the empty string, then synthesize a name.
     */
    private String regionName;


    // Current view mode held in the ComboBox, which gets retained and reused across view reconstructions.

    // Address of word 0 for the purposes of the Offset columns.
    private Address origin;

    private MemoryWordsTable table;
    private InspectorScrollPane scrollPane;

    private JToolBar toolBar;
    private final AddressInputField.Hex originField;
    private final AddressInputField.Decimal wordCountField;
    private final InspectorComboBox viewModeComboBox;  // TODO (mlvdv) generic in Java 7
    private final JLabel viewModeComboBoxRenderer;  // Holds current view mode, even across view reconstructions.
    private final InspectorButton previousButton;
    private final InspectorButton nextButton;
    private final InspectorButton findButton;
    private final InspectorButton prefsButton;
    private final InspectorButton homeButton;
    private final InspectorButton cloneButton;
    private final MemoryViewPreferences instanceViewPreferences;

    private final Rectangle originalFrameGeometry;

    /**
     * @param memoryRegion the region to view
     * @param regionName name to use for the region, use region's name if {@code null}, synthesize a name if {@code ""}
     * @param origin where to set the origin of the view, not necessarily at the start
     * @param viewMode initial mode for the view
     * @param instanceViewPreferences preferences to use for this view
     */
    @SuppressWarnings("unchecked")
    private MemoryView(Inspection inspection, final MaxMemoryRegion memoryRegion, String regionName, Address origin, ViewMode viewMode, MemoryViewPreferences instanceViewPreferences) {
        super(inspection, VIEW_KIND, null);
        assert viewMode != null;
        Trace.begin(1, tracePrefix() + " creating for region:  " + memoryRegion.toString());

        nBytesInWord = inspection.vm().platform().nBytesInWord();
        nBytesInPage = inspection.vm().platform().nBytesInPage();
        nWordsInPage = nBytesInPage / nBytesInWord;

        if (instanceViewPreferences == null) {
            // Clone the global preferences
            this.instanceViewPreferences = new MemoryViewPreferences(globalPreferences(inspection()), this);
        } else {
            // Clone another set of instance preferences
            this.instanceViewPreferences = new MemoryViewPreferences(instanceViewPreferences, this);
        }
        Address start = memoryRegion.start();
        if (start.isWordAligned()) {
            this.originalMemoryWordRegion = new MemoryWordRegion(inspection(), memoryRegion);
        } else {
            final Address alignedStart = start.alignUp(nBytesInWord);
            start = (start.equals(alignedStart)) ? start : alignedStart.minus(nBytesInWord);
            final long wordCount = wordsInRegion(memoryRegion);
            this.originalMemoryWordRegion = new MemoryWordRegion(null, start, wordCount);
        }
        this.memoryWordRegion = originalMemoryWordRegion;
        this.originalOrigin = (origin == null) ? start : origin;
        this.originalRegionName = regionName;
        this.regionName = regionName;
        this.originalViewMode = viewMode;

        this.origin = originalOrigin;

        originField = new AddressInputField.Hex(inspection, this.origin) {
            @Override
            public void update(Address value) {
                if (!value.equals(MemoryView.this.origin)) {
                    // User model policy:  any adjustment to the region drops into generic word mode
                    clearViewMode();
                    setOrigin(value.alignUp(nBytesInWord));
                    setTitle();
                }
            }
        };

        wordCountField = new AddressInputField.Decimal(inspection, Address.fromLong(memoryWordRegion.nWords())) {
            @Override
            public void update(Address value) {
                final long newWordCount = value.toLong();
                final long oldWordCount = memoryWordRegion.nWords();
                if (newWordCount <= 0) {
                    // Bogus; reset to prior value
                    wordCountField.setValue(Address.fromLong(oldWordCount));
                } else if (newWordCount != oldWordCount) {
                    // User model policy:  any adjustment to the region drops into generic word mode
                    clearViewMode();
                    setMemoryRegion(new MemoryWordRegion(inspection(), memoryRegion.start(), newWordCount));
                    setTitle();
                }
            }
        };

        // The combo box holds the current view mode
        viewModeComboBox = new InspectorComboBox(inspection, ViewMode.values());
        viewModeComboBox.setSelectedItem(originalViewMode);
        // Add the listener after the initial selection is set; we're not ready for an update yet.
        viewModeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateViewMode();
            }
        });
        viewModeComboBoxRenderer = new JLabel();
        viewModeComboBox.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final ViewMode viewMode = (ViewMode) value;
                viewModeComboBoxRenderer.setText(viewMode.label());
                return viewModeComboBoxRenderer;
            }
        });

        previousButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveBack();
            }
        });
        final InspectorStyle style = preference().style();
        previousButton.setIcon(style.navigationBackIcon());

        nextButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveForward();
            }
        });
        nextButton.setIcon(style.navigationForwardIcon());

        prefsButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                new SimpleDialog(inspection(), MemoryView.this.instanceViewPreferences.getPanel(), "View Preferences", true);
            }
        });
        prefsButton.setText(null);
        prefsButton.setToolTipText("Column view options");
        prefsButton.setIcon(style.generalPreferencesIcon());

        findButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                gui().informationMessage("memory \"Find\" is unimplemented");
            }
        });
        findButton.setIcon(style.generalFindIcon());
        findButton.setToolTipText("Find (UNIMPLEMENTED)");
        findButton.setEnabled(false);

        homeButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                resetToOriginal();
            }
        });
        homeButton.setIcon(style.navigationHomeIcon());
        homeButton.setToolTipText("Return displayed region to original");

        cloneButton = new InspectorButton(inspection(), cloneAction);
        cloneButton.setText(null);
        cloneButton.setToolTipText("Create a cloned copy of this memory view");
        cloneButton.setIcon(style.generalCopyIcon());

        createFrame(true);
        gui().setLocationRelativeToMouse(this, inspection.preference().geometry().newFrameDiagonalOffset());
        originalFrameGeometry = getGeometry();
        table.scrollToOrigin();
       // table.setPreferredScrollableViewportSize(new Dimension(-1, preferredTableHeight()));

        Trace.end(1, tracePrefix() + " creating for region:  " + memoryRegion.toString());
    }

    @Override
    protected void createViewContent() {

        table = new MemoryWordsTable(inspection(), this, memoryWordRegion, origin, instanceViewPreferences, setOriginToSelectionAction);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());

        toolBar = new InspectorToolBar(inspection());
        toolBar.setBorder(preference().style().defaultPaneBorder());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(new JLabel("Origin:"));
        toolBar.add(originField);
        toolBar.add(new JLabel("Words:"));
        toolBar.add(wordCountField);
        toolBar.add(previousButton);
        toolBar.add(viewModeComboBox);
        toolBar.add(nextButton);
        toolBar.add(findButton);
        toolBar.add(homeButton);
        toolBar.add(cloneButton);
        toolBar.add(prefsButton);
        panel.add(toolBar, BorderLayout.NORTH);

        scrollPane = new SizedScrollPane(inspection(), table);
        panel.add(scrollPane, BorderLayout.CENTER);
        setContentPane(panel);
        // Force everything into consistency with the current view mode.
        updateViewMode();

        // Populate menu bar
        final InspectorMenu defaultMenu = makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(views().deactivateOtherViewsAction(ViewKind.MEMORY, this));
        defaultMenu.add(views().deactivateAllViewsAction(ViewKind.MEMORY));

        final InspectorMenu memoryMenu = makeMenu(MenuKind.MEMORY_MENU);
        setOriginToSelectionAction.refresh(true);
        memoryMenu.add(setOriginToSelectionAction);
        memoryMenu.add(inspectBytesAction);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        memoryMenu.add(views().activateSingletonViewAction(ViewKind.ALLOCATIONS));

        final InspectorMenu viewMenu = makeMenu(MenuKind.VIEW_MENU);
        viewMenu.add(scrollToFocusAction);
        viewMenu.add(defaultMenuItems(MenuKind.VIEW_MENU));

        // When user grows window height beyond table size, expand region being viewed.
        final JViewport viewport = scrollPane.getViewport();
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                super.componentResized(componentEvent);
                final Rectangle bounds = viewport.getBounds();
//                System.out.println(bounds.toString());
//                System.out.println("Header=" + table.getTableHeader().getHeight());
//                System.out.println("Row height=" + table.getRowHeight());
//                System.out.println("Avail=" + ((bounds.height - table.getTableHeader().getHeight()) - (MemoryView.this.memoryWordRegion.wordCount * table.getRowHeight())));
                final long rowCapacity = ((bounds.height - table.getTableHeader().getHeight()) - (MemoryView.this.memoryWordRegion.nWords() * table.getRowHeight())) / table.getRowHeight();
//                System.out.println("Capacity =" + rowCapacity);
//                System.out.println("Preferred=" + preferredTableHeight());
                if (rowCapacity > 0) {
                    Trace.line(TRACE_VALUE, tracePrefix() + "growing viewport rows by " + rowCapacity);
                    growRegionDown(rowCapacity);
                }
            }
        });

    }

    private final class SizedScrollPane extends InspectorScrollPane {

        private final InspectorTable inspectorTable;

        /**
         * Creates a scrollable pane containing the {@link InspectorTable}, with preferred height set to match the size
         * of the table up to a specified limit.
         */
        public SizedScrollPane(Inspection inspection, InspectorTable inspectorTable) {
            super(inspection, inspectorTable);
            this.inspectorTable = inspectorTable;
            // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
            // this is empirical, based only the fuzziest notion of how these dimensions work
            final int displayRows = Math.min(preference().style().memoryTableMaxDisplayRows(), inspectorTable.getRowCount()) + 2;
            final int preferredHeight = displayRows * (inspectorTable.getRowHeight() + inspectorTable.getRowMargin()) +
                                                          inspectorTable.getRowMargin()  + inspectorTable.getTableHeader().getHeight();
            final int preferredWidth = inspectorTable.getPreferredScrollableViewportSize().width;
            inspectorTable.setPreferredScrollableViewportSize(new Dimension(preferredWidth, preferredHeight));
        }

        @Override
        public void redisplay() {
            inspectorTable.redisplay();
        }

        @Override
        public void refresh(boolean force) {
            inspectorTable.refresh(force);
        }

    }

    private int preferredTableHeight() {
        // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
        // this is empirical, based only the fuzziest notion of how these dimensions work
        final int displayRows = Math.min(preference().style().memoryTableMaxDisplayRows(), table.getRowCount());
        final int rowHeight = table.getRowHeight();
        final int rowMargin = table.getRowMargin();
        final int headerHeight = table.getTableHeader().getHeight();
        final int preferredHeight = displayRows * (rowHeight + rowMargin) + rowMargin  + headerHeight;
//        Trace.line(TRACE_VALUE, tracePrefix() + "preferredHeight=" + preferredHeight + "[ rows=" + displayRows + ", rowHeight=" + rowHeight
//                + ", rowMargin=" + rowMargin + ", headerHeight=" + headerHeight + "]");
        return preferredHeight;
    }

    /**
     * Gets current view mode.
     */
    private ViewMode viewMode() {
        return (ViewMode) viewModeComboBox.getSelectedItem();
    }

    /**
     * Sets current view mode and updates related state.
     */
    private void setViewMode(ViewMode viewMode) {
        viewModeComboBox.setSelectedItem(viewMode);
        updateViewMode();
    }

    /**
     * Sets the current view parameters to the default state.
     */
    private void clearViewMode() {
        setViewMode(ViewMode.WORD);
        regionName = null;
    }

    /**
     * Updates state related to current view mode.
     */
    private void updateViewMode() {
        previousButton.setToolTipText(viewMode().previousToolTip());
        nextButton.setToolTipText(viewMode().nextToolTip());
        switch (viewMode()) {
            case OBJECT:
                moveToCurrentObject();
                break;
            case PAGE:
                moveToCurrentPage();
                break;
            case WORD:
                break;
            default:
                InspectorError.unknownCase();
        }
        setTitle();
    }

    @Override
    public Rectangle defaultGeometry() {
        return originalFrameGeometry;
    }

    @Override
    public String getTextForTitle() {

        // Locate the first and last memory locations with respect to regions
        // allocated by the VM, and build a description of the memory range
        final MaxMemoryRegion startRegion = vm().state().findMemoryRegion(memoryWordRegion.start());
        final MaxMemoryRegion lastRegion = vm().state().findMemoryRegion(memoryWordRegion.end().minus(1));
        final String startRegionName = startRegion == null ? UNKNOWN_REGION_NAME : startRegion.regionName();
        final String lastRegionName = lastRegion == null ? UNKNOWN_REGION_NAME : lastRegion.regionName();
        final String regionDescription = " in "
            + (startRegionName.equals(lastRegionName) ? startRegionName : (startRegionName + " - " + lastRegionName));
        final StringBuilder titleBuilder = new StringBuilder();
        switch(viewMode()) {
            case OBJECT:
                MaxObject object = null;
                try {
                    object = vm().objects().findAnyObjectAt(origin);
                } catch (MaxVMBusyException e) {
                }
                if (object == null) {
                    titleBuilder.append("Memory object: ").append(memoryWordRegion.start().toHexString());
                } else {
                    titleBuilder.append("Memory: object ").append(memoryWordRegion.start().toHexString()).append(inspection().nameDisplay().referenceLabelText(object));
                }
                titleBuilder.append(regionDescription);
                break;
            case PAGE:
                titleBuilder.append("Memory: page ").append(memoryWordRegion.start().toHexString());
                titleBuilder.append(regionDescription);
                break;
            case WORD:
                if (regionName == null) {
                    titleBuilder.append(memoryWordRegion.regionName());
                } else if (regionName.equals("")) {
                    titleBuilder.append("Memory: ").append(memoryWordRegion.start().toHexString()).append("--").append(memoryWordRegion.end().toHexString());
                    titleBuilder.append(regionDescription);
                } else {
                    titleBuilder.append("Memory: region ").append(regionName);
                    // No suffix; we already know the name of the region
                }
                break;
            default:
                InspectorError.unknownCase();
        }
        return titleBuilder.toString();
    }

    @Override
    protected void refreshState(boolean force) {
        table.refresh(force);
        setTitle();
    }

    /**
     * @return the number of words contained in region of VM memory.
     */
    private long wordsInRegion(MaxMemoryRegion memoryRegion) {
        return memoryRegion.nBytes() / nBytesInWord;
    }

    /**
     * Sets the view to the parameters specified when the view was created.
     */
    private void resetToOriginal() {
        setOrigin(originalOrigin);
        setMemoryRegion(originalMemoryWordRegion);
        setViewMode(originalViewMode);
        table.setPreferredScrollableViewportSize(new Dimension(-1, preferredTableHeight()));
        table.scrollToBeginning();
        validate();
        regionName = originalRegionName;
        setTitle();
    }

    /**
     * Changes the viewing origin and updates related state; does not change the region being viewed.
     */
    private void setOrigin(Address origin) {
        this.origin = origin;
        originField.setText(origin.toUnsignedString(16));
        table.setOrigin(this.origin);
    }

    /**
     * Changes the viewed memory region and updates related state; does not change the origin.
     */
    private void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
        this.memoryWordRegion = memoryWordRegion;
        wordCountField.setValue(Address.fromLong(memoryWordRegion.nWords()));
        table.setMemoryRegion(memoryWordRegion);

    }
    private int firstVisibleRow() {
        return table.rowAtPoint(new Point(0, scrollPane.getViewport().getViewRect().y));
    }

    private int lastVisibleRow() {
        final Rectangle visible = scrollPane.getViewport().getViewRect();
        return table.rowAtPoint(new Point(0, visible.y + visible.height - 10));
    }

    private void scrollToRowCentered(int row) {
        final int nRows = lastVisibleRow() - firstVisibleRow();
        table.scrollToRows(row - nRows / 3, row + 2 * nRows / 3);
    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveBack() {
        switch (viewMode()) {
            case OBJECT:
                moveToPreviousObject();
                break;
            case PAGE:
                moveToPreviousPage();
                break;
            case WORD:
                growRegionUp(1);
                break;
            default:
                InspectorError.unknownCase();
        }
    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveForward() {
        switch (viewMode()) {
            case OBJECT:
                moveToNextObject();
                break;
            case PAGE:
                moveToNextPage();
                break;
            case WORD:
                growRegionDown(1);
                break;
            default:
                InspectorError.unknownCase();
        }
    }

    /**
     * Grows the viewed region at the top (lowest address).
     */
    private void growRegionUp(long addedRowCount) {
        final long newWordCount = memoryWordRegion.nWords() + addedRowCount;
        final Address newStart = memoryWordRegion.start().minus(nBytesInWord * addedRowCount);
        setMemoryRegion(new MemoryWordRegion(inspection(), newStart, newWordCount));
        table.scrollToBeginning();
        // User model policy:  any adjustment to the region drops into generic word mode
        clearViewMode();
        setTitle();
    }

    /**
     * Grows the viewed region at the bottom (highest address).
     */
    private void growRegionDown(long rowCapacity) {
        final long newWordCount = memoryWordRegion.nWords() + rowCapacity;

        setMemoryRegion(new MemoryWordRegion(inspection(), memoryWordRegion.start(), newWordCount));
        table.scrollToEnd();
        // User model policy:  any adjustment to the region drops into generic word mode
        clearViewMode();
        setTitle();
    }

    private void moveToCurrentObject() {
        MaxObject object = null;
        try {
            object = vm().objects().findAnyObjectAt(origin);
        } catch (MaxVMBusyException e) {
        }
        if (object != null) {
            MaxMemoryRegion objectMemoryRegion = object.objectMemoryRegion();
            final Address start = objectMemoryRegion.start().alignUp(nBytesInWord);
            // User model policy, grow the size of the viewing region if needed, but never shrink it.
            final long newWordCount = Math.max(wordsInRegion(objectMemoryRegion), memoryWordRegion.nWords());
            setMemoryRegion(new MemoryWordRegion(inspection(), start, newWordCount));
            setOrigin(object.origin());
            table.scrollToOrigin();
            setTitle();
        } else {
            moveToPreviousObject();
        }
    }

    private void moveToPreviousObject() {
        final MaxObject object = vm().objects().findAnyObjectPreceding(origin, 1000000);
        if (object != null) {
            MaxMemoryRegion objectMemoryRegion = object.objectMemoryRegion();
            final Address start = objectMemoryRegion.start().alignUp(nBytesInWord);
            // User model policy, grow the size of the viewing region if needed, but never shrink it.
            final long newWordCount = Math.max(wordsInRegion(objectMemoryRegion), memoryWordRegion.nWords());
            setMemoryRegion(new MemoryWordRegion(inspection(), start, newWordCount));
            setOrigin(object.origin());
            table.scrollToOrigin();
            setTitle();
        }
    }

    private void moveToNextObject() {
        final MaxObject object = vm().objects().findAnyObjectFollowing(origin, 1000000);
        if (object != null) {
            final MaxMemoryRegion objectMemoryRegion = object.objectMemoryRegion();
            // Start stays the same
            final Address start = memoryWordRegion.start();
            // Default is to leave the viewed size the same
            long newWordCount = memoryWordRegion.nWords();
            if (!memoryWordRegion.contains(objectMemoryRegion.end())) {
                // Grow the end of the viewed region if needed to include the newly found object
                newWordCount = objectMemoryRegion.end().minus(start).dividedBy(nBytesInWord).toInt();
            }
            setMemoryRegion(new MemoryWordRegion(inspection(), start, newWordCount));
            setOrigin(object.origin());
            // Scroll so that whole object is visible if possible
            table.scrollToRange(origin, objectMemoryRegion.end().minus(nBytesInWord));
            setTitle();
        }
    }

    private void moveToCurrentPage() {
        Address newOrigin = origin.alignUp(nBytesInPage);
        if (!newOrigin.equals(origin)) {
            // We're not at a page boundary, so set to the beginning of the current one.
            newOrigin = newOrigin.minus(nBytesInPage);
        }
        setOrigin(newOrigin);
        setMemoryRegion(new MemoryWordRegion(inspection(), newOrigin, nWordsInPage));
        table.scrollToBeginning();
        setTitle();
    }

    private void moveToNextPage() {
        Address nextOrigin = origin.alignUp(nBytesInPage);
        if (origin.equals(nextOrigin)) {
            // Already at beginning of a page; jump to next.
            nextOrigin = nextOrigin.plus(nBytesInPage);
        }
        setOrigin(nextOrigin);
        setMemoryRegion(new MemoryWordRegion(inspection(), nextOrigin, nWordsInPage));
        table.scrollToBeginning();
        setTitle();
    }

    private void moveToPreviousPage() {
        final Address newOrigin = origin.alignUp(nBytesInPage).minus(nBytesInPage);
        setOrigin(newOrigin);
        setMemoryRegion(new MemoryWordRegion(inspection(), newOrigin, nWordsInPage));
        table.scrollToBeginning();
        setTitle();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                final MemoryViewPreferences globalPreferences = globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MemoryColumnKind>(inspection(), "View Options", instanceViewPreferences, globalPreferences);
            }
        };
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Memory view displays are sensitive to the current thread selection (for register values)
        forceRefresh();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        forceRefresh();
    }

    @Override
    public void viewClosing() {
        // Unsubscribe to view preferences, when we get them.
        super.viewClosing();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() != TERMINATED) {
            forceRefresh();
        }
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

    /**
     * @return the region of VM memory current in view in this view.
     */
    public MaxMemoryRegion getCurrentMemoryRegion() {
        return memoryWordRegion;
    }

    private InspectorAction cloneAction = new InspectorAction(inspection(), "Clone") {
        @Override
        protected void procedure() {
            final InspectorView view = new MemoryView(inspection(), memoryWordRegion, regionName, origin, viewMode(), instanceViewPreferences);
            view.highlight();
        }
    };

    private InspectorAction setOriginToSelectionAction = new InspectorAction(inspection(), "Set Origin to selected location") {
        @Override
        protected void procedure() {
            setOrigin(focus().address());
            MemoryView.this.forceRefresh();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(memoryWordRegion.contains(focus().address()));
        }
    };

    private InspectorAction scrollToFocusAction = new InspectorAction(inspection(), "Scroll to selected memory location") {
        @Override
        protected void procedure() {
            scrollToRowCentered(table.findRow(focus().address()));
            MemoryView.this.forceRefresh();
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(memoryWordRegion.contains(focus().address()));
        }
    };

    private InspectorAction inspectBytesAction = new InspectorAction(inspection(), "View memory at Origin as bytes") {
        @Override
        protected void procedure() {
            views().memoryBytes().makeView(origin);
        }
    };

}
