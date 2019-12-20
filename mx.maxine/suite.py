#
# Copyright (c) 2017-2019, APT Group, School of Computer Science,
# The University of Manchester. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
suite = {
    "mxversion": "5.190.3",
    "name": "maxine",
    "sourceinprojectwhitelist": [],
    "url": "https://github.com/beehive-lab/Maxine-VM",
    "scm": {
        "url": "https://github.com/beehive-lab/Maxine-VM",
        "read": "https://github.com/beehive-lab/Maxine-VM.git",
        "write": "git@github.com:beehive-lab/Maxine-VM.git",
    },
    "outputRoot": "./",
    "javac.lint.overrides": "-rawtypes",

    "imports": {
        "suites": [
            {
                "name": "Maxine-Graal",
                "version": "newmx",
                "urls": [
                    {"url": "https://github.com/beehive-lab/Maxine-Graal.git", "kind": "git"},
                ]
            },

            # {
            #     "name" : "compiler",
            #     "subdir" : True,
            #     "version" : "vm-enterprise-0.30.1",
            #     "urls" : [
            #       {"url" : "https://github.com/graalvm/graal.git", "kind" : "git"},
            #     ]
            # },
        ]
    },

    "licenses": {
        "GPLv2": {
            "name": "GNU General Public License, version 2",
            "url": "https://www.gnu.org/licenses/gpl-2.0.html"
        },
    },

    "defaultLicense": "GPLv2",

    # ------------- JDK Libraries -------------
    "jdklibraries": {
        "JDK_TOOLS": {
            "path": 'lib/tools.jar',
            "sourcePath": "src.zip",
            "jdkStandardizedSince": "999",
        },
    },

    # ------------- Libraries -------------
    "libraries": {

        "JLF_ICONS": {
            "sha1": "b4680aaa71cd947cbd7ce82b0a569fd99b952985",
            "maven": {
                "groupId": "net.java.linoleum",
                "artifactId": "jlfgr",
                "version": "1_0",
            },
        },

        "NB3_BUTTONS": {
            "sha1": "40108ac637c3813fe154eb2d15ba8736fb5805f7",
            "path": "com.sun.max.ins/NB3B.jar",
        },

        "TESTS_JASMIN_CLASSES": {
            "sha1": "cd79f5f455158d6fcbcb066f5175fc96dac9cf71",
            "path": "test/jasmin_classes.jar",
        },

        "ASM": {
            "sha1": "a4581414aab6b45e824d2bcedffad539e74ed29c",
            "path": "lib/asm-6.0-maxine.jar",
        },

        "ASMUTIL": {
            "sha1": "9d5ed2a22332f928c29ce88a2cfb44340313f33e",
            "path": "lib/asm-util-6.0-maxine.jar",
        },

    },

    # ------------- Maxine -------------

    "projects": {

        "com.oracle.max.asm": {
            "sourceDirs": ["src", "test"],
            "dependencies": ["com.oracle.max.cri", "mx:JUNIT"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.cri": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.cri", "com.sun.max.annotate"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.criutils": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.cri"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.elf": {
            "sourceDirs": ["src"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.hcfdis": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.max.asm"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.jdwp": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.max.jdwp.vm"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.ins": {
            "sourceDirs": ["src", "test"],
            "dependencies": [
                "com.sun.max.tele",
                "JLF_ICONS",
                "NB3_BUTTONS",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
            "javac.lint.overrides": "-rawtypes,-serial",
        },

        "com.sun.max.jdwp.maxine": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.sun.max.jdwp",
                "com.sun.max.tele",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.tele": {
            "sourceDirs": ["src", "test"],
            "dependencies": [
                "com.oracle.max.vm.ext.jvmti",
                "com.sun.max.jdwp.vm",
                "com.oracle.max.elf",
                "com.oracle.max.hcfdis",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.tools": {
            "sourceDirs": ["src"],
            "dependencies": [
                "JDK_TOOLS",
                "com.sun.max",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.bctrans": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.max"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.c1x": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.oracle.max.vm.ext.maxri",
                "com.sun.c1x",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.c1xgraal": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.oracle.max.vm.ext.c1x",
                "com.oracle.max.vm.ext.graal",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.graal": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.oracle.max.vm.ext.maxri",
                "Maxine-Graal:GRAAL_MAXINE",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.jjvmti": {
            "sourceDirs": ["src"],
            "dependencies": ["com.oracle.max.vm.ext.jvmti"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.jvmti": {
            "sourceDirs": ["src", "demo"],
            "dependencies": ["com.oracle.max.vm.ext.t1x"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.maxri": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.sun.max",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.t1x": {
            "sourceDirs": ["src"],
            "dependencies": ["com.oracle.max.vm.ext.maxri"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.ext.vma": {
            "sourceDirs": ["src", "test"],
            "dependencies": [
                "com.oracle.max.vm.ext.jjvmti",
                "com.oracle.max.vm.ext.graal",
            ],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.oracle.max.vm.native": {
            "native": True,
        },

        "com.oracle.max.vm.tests": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.oracle.max.vm.ext.t1x",
                "com.oracle.max.vm.ext.c1x",
                "uk.ac.manchester.tests.jdk8",
                "test",
                "jtt",
                "ASM",
                "ASMUTIL",
            ],
            "generatedDependencies": ["uk.ac.manchester.tests.jdk8"],
            "checkstyle": "com.sun.max",
            "TestProject": True,
            "javaCompliance": "1.8",
            "javac.lint.overrides": "-rawtypes,-finally",
        },

        "com.oracle.max.vma.tools": {
            "sourceDirs": ["src"],
            "dependencies": ["com.oracle.max.vm.ext.vma"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.c1x": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.max"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.cri": {
            "sourceDirs": ["src"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max": {
            "sourceDirs": ["src", "test"],
            "dependencies": [
                "com.oracle.max.asm",
                "com.oracle.max.criutils",
                "JDK_TOOLS",
            ],
            "checkstyleVersion" : "8.8",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.annotate": {
            "sourceDirs": ["src"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.asm": {
            "sourceDirs": ["src", "test"],
            "dependencies": ["com.sun.max"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "com.sun.max.jdwp.vm": {
            "sourceDirs": ["src"],
            "checkstyle": "com.sun.max",
            "javaCompliance": "1.7+",
        },

        "jtt": {
            "sourceDirs": ["src"],
            "dependencies": [
                "TESTS_JASMIN_CLASSES",
                "com.sun.max",
            ],
            "checkstyle": "com.sun.max",
            "TestProject": True,
            "javaCompliance": "1.7+",
        },

        "test": {
            "sourceDirs": ["src"],
            "dependencies": ["com.sun.max"],
            "checkstyle": "com.sun.max",
            "TestProject": True,
            "javaCompliance": "1.7+",
        },

        "test.jsr292": {
            "sourceDirs": ["src"],
            "checkstyle": "com.sun.max",
            "TestProject": True,
            "javaCompliance": "1.7+",
        },

        "uk.ac.manchester.tests.jdk8": {
            "sourceDirs": ["src"],
            "dependencies": [
                "com.sun.max",
            ],
            "checkstyle": "com.sun.max",
            "TestProject": True,
            "javaCompliance": "1.8",
        },

    },
}
