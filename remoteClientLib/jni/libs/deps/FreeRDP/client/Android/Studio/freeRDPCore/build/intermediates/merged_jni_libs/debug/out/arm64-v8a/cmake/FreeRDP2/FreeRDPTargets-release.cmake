#----------------------------------------------------------------
# Generated CMake target import file for configuration "Release".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "freerdp" for configuration "Release"
set_property(TARGET freerdp APPEND PROPERTY IMPORTED_CONFIGURATIONS RELEASE)
set_target_properties(freerdp PROPERTIES
  IMPORTED_LINK_DEPENDENT_LIBRARIES_RELEASE "winpr"
  IMPORTED_LINK_INTERFACE_LIBRARIES_RELEASE ""
  IMPORTED_LOCATION_RELEASE "${_IMPORT_PREFIX}/./libfreerdp2.so"
  IMPORTED_SONAME_RELEASE "libfreerdp2.so"
  )

list(APPEND _IMPORT_CHECK_TARGETS freerdp )
list(APPEND _IMPORT_CHECK_FILES_FOR_freerdp "${_IMPORT_PREFIX}/./libfreerdp2.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
