define run_test
    si 5

    set disassemble-next-line on
    show disassemble-next-line

    set $n = 1000
    set $i = 0
    while ($i < $n)
        set $i = $i + 1
        printf "step #%d\n", $i
        si
        info registers
    end

    quit
end
