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
package com.sun.max.elf.xen.section.notes;

import com.sun.max.elf.xen.section.notes.NotesSection.DescriptorType;


/**
 * @author Puneeet Lakhina
 *
 */
public class HeaderNoteDescriptor extends NotesSectionDescriptor {

    static enum DomainType {
        PARAVIRTUALIZED, FULL_VIRTUALIZED;

        private static final long PVDOMAIN_MAGIC_NUMBER = 0xF00FEBEDL;
        private static final long FVDOMAIN_MAGIC_NUMBER = 0xF00FEBEEL;

        public static DomainType getType(long magicNumber) {
            if (magicNumber == PVDOMAIN_MAGIC_NUMBER) {
                return PARAVIRTUALIZED;
            } else if(magicNumber == FVDOMAIN_MAGIC_NUMBER) {
                return FULL_VIRTUALIZED;
            }else {
                throw new IllegalArgumentException("Improper Magic Number");
            }
        }
    };

    /*
     * The domain type depends on the magic number.
     *
     */
    private long magicnumber;
    //vpus is uint64 but we use int here.
    private int vcpus;
    private long noOfPages;
    private long pageSize;
    public HeaderNoteDescriptor() {
        super(DescriptorType.HEADER);
    }




    /**
     * @return the magicnumber
     */
    public long getMagicnumber() {
        return magicnumber;
    }




    /**
     * @param magicnumber the magicnumber to set
     */
    public void setMagicnumber(long magicnumber) {
        this.magicnumber = magicnumber;
    }




    /**
     * @return the domainType
     */
    public DomainType getDomainType() {
        return DomainType.getType(magicnumber);
    }

    /**
     * @return the vcpus
     */
    public int getVcpus() {
        return vcpus;
    }




    /**
     * @param vcpus the vcpus to set
     */
    public void setVcpus(int vcpus) {
        this.vcpus = vcpus;
    }




    /**
     * @return the noOfPages
     */
    public long getNoOfPages() {
        return noOfPages;
    }




    /**
     * @param noOfPages the noOfPages to set
     */
    public void setNoOfPages(long noOfPages) {
        this.noOfPages = noOfPages;
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



    @Override
    public String toString() {
        return "Domain Type: [" + getDomainType() +"] , No of Pages: [" + noOfPages + "], Page Size: [" + pageSize + "],  VCpus: [" + vcpus + "]";
    }

}
