//Example for arm64-v8a
//Qemu 9.2.2 & 7.2.22
//Fix when building with ARM CPUs
//No need to fix if you don't get errors or don't use Mesa.
//qemu-3dfx/qemu-1/hw/mesa/mglmapbo.c
//...
#include <arm_acle.h>
//...
//#define _mm_crc32_u64 __builtin_arm_crc32cd
//Edit to
#define _mm_crc32_u64 __crc32cd
//...