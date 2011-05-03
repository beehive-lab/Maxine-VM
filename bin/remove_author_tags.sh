#!/bin/bash
#
# Script to remove @author tags from the files supplied as args to this script.
#

files="$@"
for f in $files; do
    awk <$f >$f.tmp '
/\* @author [A-Za-z ]*$/ { next; }
                         { print; } '
    diff $f $f.tmp >/dev/null
    if [ $? -ne 0 ]; then
        mv $f $f.orig
        mv $f.tmp $f
        echo "Modified $f - original in $f.orig"
    else
        rm $f.tmp
    fi
done
