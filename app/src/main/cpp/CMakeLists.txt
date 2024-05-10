cmake_minimum_required(VERSION 3.22.1)

Project(VectrasVM)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

add_subdirectory(proot)