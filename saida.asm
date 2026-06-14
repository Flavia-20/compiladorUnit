.data
flag db 0 ; deslocamento 4
x dw 0 ; deslocamento 0
y dw 0 ; deslocamento 2

.text
_start:
    ; Rotinas auxiliares como _print_integer e _read_integer sao externas neste projeto didatico.
    ; Tambem sao consideradas externas: _print_boolean, _print_string, _read_boolean, _read_string, _copy_string.
    mov word ptr [x], 14
    mov word ptr [y], 15
    mov al, 0
    mov byte ptr [flag], al
    mov al, 0
    cmp al, 0
    je L_0
    push word ptr [x]
    call _print_integer
    jmp L_1
L_0:
    push word ptr [y]
    call _print_integer
L_1:
L_2:
    mov al, byte ptr [flag]
    cmp al, 0
    je L_3
    mov al, 0
    mov byte ptr [flag], al
    push word ptr [y]
    call _print_integer
    jmp L_2
L_3:
    ; fim do programa
