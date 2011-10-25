package com.sun.max.ins.method;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.NativeCodeMaps.*;
import com.sun.max.lang.*;

public class NativeFunctionSearchDialog extends FilteredListDialog<Info> {

    @Override
    protected Info noSelectedObject() {
        return null;
    }

    @Override
    protected Info convertSelectedItem(Object listItem) {
        return (Info) listItem;
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filter = filterText.toLowerCase();
        for (Info info : NativeCodeMaps.map.values()) {
            if (filter.endsWith(" ")) {
                if (info.name.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    listModel.addElement(info);
                }
            } else if (info.name.toLowerCase().contains(filter)) {
                listModel.addElement(info);
            }
        }
    }

    private NativeFunctionSearchDialog(Inspection inspection, String title, String actionName, boolean multiSelection) {
        super(inspection, title == null ? "Select Native Function" : title, "Filter text", actionName, multiSelection);
        rebuildList();
    }

    /**
     * Displays a dialog to let the use select one or more native functions in the tele VM.
     *
     * @param title for dialog window
     * @param actionName name to appear on button
     * @param multi allow multiple selections if true
     * @return references to the selected instances of {@link NativeCodeMaps.Info}, null if user canceled.
     */
    public static List<NativeCodeMaps.Info> show(Inspection inspection, String title, String actionName, boolean multi) {
        final NativeFunctionSearchDialog dialog = new NativeFunctionSearchDialog(inspection, title, actionName, multi);
        dialog.setVisible(true);
        return dialog.selectedObjects();
    }
}
