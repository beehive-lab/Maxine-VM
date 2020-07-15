#include <windows.h>

#include <stddef.h>    // size_t
#include <stdint.h>    // uint8_t, uint32_t
#include <stdio.h>     // printf
#include <tchar.h>     // Must be included before strsafe.h
#include <strsafe.h>
#include <fcntl.h>
// Display the error message corresponding to GetLastError() in a message box.

  uint8_t machine_code[] = {'k', 0xB8, 0x78, 0x56, 0x34, 0x12, 0xC3 };

int _tmain(int argc, _TCHAR **argv) {
    // Allocate a new page of memory, setting its protections to read+write
    LPVOID mem = VirtualAlloc(NULL, sizeof(machine_code),
        MEM_COMMIT, PAGE_READWRITE);
    if (mem == NULL) {
        return 1;
    }

	int fd = open("output.txt" , _O_RDWR	);
	write(
   fd,
  machine_code,
   sizeof(machine_code));
   _close(fd);
   HANDLE open_img_result = CreateFileA(
  "output.txt" ,
  GENERIC_READ | GENERIC_WRITE|GENERIC_EXECUTE ,
  FILE_SHARE_WRITE | FILE_SHARE_READ,
  NULL,
  OPEN_EXISTING,
  FILE_ATTRIBUTE_NORMAL,
  NULL
);
if (open_img_result == INVALID_HANDLE_VALUE || !open_img_result)
	        printf("could not open image file:  %d" , GetLastError());
		
		 
  HANDLE fmapping = CreateFileMappingA( open_img_result  , NULL , PAGE_EXECUTE_READWRITE| SEC_COMMIT  ,0u ,0,   NULL);
	if(!fmapping)
		printf("ss %d\n", GetLastError());
	mem =  MapViewOfFileEx (fmapping,   FILE_MAP_READ | FILE_MAP_WRITE | FILE_MAP_EXECUTE |FILE_MAP_COPY,   0, 0,0,   0);
	if(!mem)
		printf("dd %d\n", GetLastError());


    // Point a function pointer at the newly allocated page, then call it
	printf("fff %d \n", sizeof(machine_code));
	mem =mem +1;
    uint32_t(*fn)() = (uint32_t(*)()) mem;
    uint32_t result = fn();
    _tprintf(TEXT("result = 0x%x\n"), result);
	return 0;
   

}

