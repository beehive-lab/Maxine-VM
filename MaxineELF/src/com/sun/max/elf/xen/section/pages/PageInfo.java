/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 *
 */
package com.sun.max.elf.xen.section.pages;

import com.sun.max.elf.xen.XenCoreDumpELFReader;


/**
 * This maps to to the xen_cumpcore_p2m struct. An array of this struct (array length = no of pages) is contained in the {@link XenCoreDumpELFReader#P2M_SECTION_NAME} section of the core dump. The {@link XenCoreDumpELFReader#PFN_SECTION_NAME} is for fully virtualized or ia64 domain both of which we done support.
 * @author Puneeet Lakhina
 *
 */
public class PageInfo {

    private long pfn;
    private long gmfn;

    /**
     * @return the pfn
     */
    public long getPfn() {
        return pfn;
    }

    /**
     * @param pfn the pfn to set
     */
    public void setPfn(long pfn) {
        this.pfn = pfn;
    }

    /**
     * @return the gmfn
     */
    public long getGmfn() {
        return gmfn;
    }

    /**
     * @param gmfn the gmfn to set
     */
    public void setGmfn(long gmfn) {
        this.gmfn = gmfn;
    }

    @Override
    public String toString() {
        return String.format("pfn=%s , gfn = %s ",Long.toHexString(pfn),Long.toHexString(gmfn));
    }

    @Override
    public int hashCode() {
        //This is based on Long.hashCode + Iconstant and iTotal from apache commons-lang
        return (17*37 + ((int)(this.gmfn ^ (this.gmfn >>> 32)))) + (17*37 + ((int)(this.pfn ^ (this.pfn >>> 32))));
    }

    @Override
    public boolean equals(Object other) {
        if(other != null && other instanceof PageInfo) {
            PageInfo otherPage = (PageInfo)other;
            return this.isValid() && otherPage.isValid() && this.pfn == otherPage.pfn && this.gmfn == otherPage.gmfn;
        }
        return false;
    }

    public boolean isValid() {
      return !(this.gmfn == (~0) || this.pfn == (~0));
    }

}
