/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems, licensed from the University of California. UNIX is a
 * registered trademark in the U.S. and in other countries, exclusively licensed through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the
 * U.S. and other countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in
 * other countries. Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether
 * direct or indirect, are strictly prohibited. Export or reexport to countries subject to U.S. embargo or to entities
 * identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 */
package com.sun.max.elf.xen.section.notes;

import com.sun.max.elf.xen.section.notes.NotesSection.DescriptorType;

/**
 * The Xen Version descriptor from the notes section of the ELF dump
 *
 * @author Puneeet Lakhina
 *
 */
public class XenVersionDescriptor extends NotesSectionDescriptor {

    public XenVersionDescriptor() {
        super(DescriptorType.XEN_VERSION);
    }

    public static final int EXTRA_VERSION_LENGTH = 16;
    public static final int CAPABILITIES_LENGTH = 1024;
    public static final int CHANGESET_LENGTH = 64;
    private long majorVersion;
    private long minorVersion;
    private String extraVersion;
    private CompileInfo compileInfo;
    private String capabilities;
    private String changeSet;
    private long platformParamters;
    private long pageSize;

    static class CompileInfo {
        public static final int COMPILE_INFO_COMPILER_LENGTH = 64;
        public static final int COMPILE_INFO_COMPILE_BY_LENGTH = 16;
        public static final int COMPILE_INFO_COMPILER_DOMAIN_LENGTH = 32;
        public static final int COMPILE_INFO_COMPILE_DATE_LENGTH = 32;
        private String compiler;
        private String compiledBy;
        private String compileDomain;
        private String compileDate;

    }



    public void setCompileInfo(String compiler,String compiledby,String compiledomain,String compileDate) {
        compileInfo = new CompileInfo();
        compileInfo.compileDate = compileDate;
        compileInfo.compiler=compiler;
        compileInfo.compileDomain=compiledomain;
        compileInfo.compiledBy=compiledby;
    }




    /**
     * @return the majorVersion
     */
    public long getMajorVersion() {
        return majorVersion;
    }




    /**
     * @param majorVersion the majorVersion to set
     */
    public void setMajorVersion(long majorVersion) {
        this.majorVersion = majorVersion;
    }




    /**
     * @return the minorVersion
     */
    public long getMinorVersion() {
        return minorVersion;
    }




    /**
     * @param minorVersion the minorVersion to set
     */
    public void setMinorVersion(long minorVersion) {
        this.minorVersion = minorVersion;
    }




    /**
     * @return the extraVersion
     */
    public String getExtraVersion() {
        return extraVersion;
    }




    /**
     * @param extraVersion the extraVersion to set
     */
    public void setExtraVersion(String extraVersion) {
        this.extraVersion = extraVersion;
    }




    /**
     * @return the capabilities
     */
    public String getCapabilities() {
        return capabilities;
    }




    /**
     * @param capabilities the capabilities to set
     */
    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }




    /**
     * @return the changeSet
     */
    public String getChangeSet() {
        return changeSet;
    }




    /**
     * @param changeSet the changeSet to set
     */
    public void setChangeSet(String changeSet) {
        this.changeSet = changeSet;
    }




    /**
     * @return the platformParamters
     */
    public long getPlatformParamters() {
        return platformParamters;
    }




    /**
     * @param platformParamters the platformParamters to set
     */
    public void setPlatformParamters(long platformParamters) {
        this.platformParamters = platformParamters;
    }




    /**
     * @return the pageSize
     */
    public long getPageSize() {
        return pageSize;
    }




    /**
     * @param pageSize the pageSize to set
     */
    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }




    /**
     * @return the compileInfo
     */
    public CompileInfo getCompileInfo() {
        return compileInfo;
    }


}
