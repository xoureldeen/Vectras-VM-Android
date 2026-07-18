cmake_minimum_required(VERSION 3.22.1)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
project("termux-terminal")

add_library(termux SHARED termux.c)

find_library(log-lib log)
target_link_libraries(termux ${log-lib})