package com.sun.max.ins.debug;


public interface ColumnKind {
    String label();
    String toolTipText();
    String toString();
    boolean canBeMadeInvisible();
    boolean defaultVisibility();
    int minWidth();
    int ordinal();
}
