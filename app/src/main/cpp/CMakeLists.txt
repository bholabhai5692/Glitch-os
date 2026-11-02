cmake_minimum_required(VERSION 3.22.1)
project(photorec_jni C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

include(CheckIncludeFile)
set(PHOTOREC_PLATFORM_DEFINITIONS)
foreach(header IN ITEMS
    dirent.h
    errno.h
    fcntl.h
    inttypes.h
    limits.h
    locale.h
    signal.h
    stdint.h
    stdlib.h
    string.h
    strings.h
    sys/ioctl.h
    sys/mman.h
    sys/mount.h
    sys/param.h
    sys/stat.h
    sys/time.h
    sys/types.h
    sys/utsname.h
    sys/wait.h
    time.h
    unistd.h
    utime.h)
  string(TOUPPER "${header}" header_macro)
  string(REPLACE "/" "_" header_macro "${header_macro}")
  string(REPLACE "." "_" header_macro "${header_macro}")
  set(definition_name "HAVE_${header_macro}")
  check_include_file("${header}" "${definition_name}")
  if(${definition_name})
    list(APPEND PHOTOREC_PLATFORM_DEFINITIONS "${definition_name}")
  endif()
endforeach()

set(TESTDISK_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/testdisk/src")
file(STRINGS "${TESTDISK_SOURCE_DIR}/Makefile.am" TESTDISK_AUTOMAKE_LINES)
file(READ "${CMAKE_CURRENT_SOURCE_DIR}/third_party/testdisk/configure.ac" TESTDISK_CONFIGURE_SOURCE)
string(REGEX MATCH "AC_INIT\\(\\[testdisk\\],\\[([^]]+)\\]" VERSION_MATCH "${TESTDISK_CONFIGURE_SOURCE}")
set(TESTDISK_VERSION "${CMAKE_MATCH_1}")
string(REGEX MATCH "TESTDISKDATE=\"([^\"]+)\"" RELEASE_DATE_MATCH "${TESTDISK_CONFIGURE_SOURCE}")
set(TESTDISK_RELEASE_DATE "${CMAKE_MATCH_1}")

function(read_automake_sources variable_name output_name)
  set(value "")
  set(continuing FALSE)

  foreach(line IN LISTS TESTDISK_AUTOMAKE_LINES)
    if(NOT continuing)
      if(line MATCHES "^${variable_name}[ \t]*=[ \t]*(.*)$")
        set(value "${CMAKE_MATCH_1}")
        string(FIND "${value}" "\\" slash_position)
        if(slash_position GREATER_EQUAL 0)
          set(continuing TRUE)
        else()
          break()
        endif()
      endif()
    else()
      string(APPEND value " ${line}")
      string(FIND "${line}" "\\" slash_position)
      if(slash_position LESS 0)
        break()
      endif()
    endif()
  endforeach()

  string(REPLACE "\\" " " value "${value}")
  string(REGEX REPLACE "[ \t]+" ";" value "${value}")
  set(${output_name} "${value}" PARENT_SCOPE)
endfunction()

read_automake_sources(smallbase_C smallbase_sources)
read_automake_sources(base_C base_sources)
string(REPLACE "$(smallbase_C)" "${smallbase_sources}" base_sources "${base_sources}")
read_automake_sources(fs_C filesystem_sources)
read_automake_sources(file_C file_carver_sources)
read_automake_sources(photorec_C photorec_sources)
read_automake_sources(photorec_ncurses_C photorec_cli_sources)

set(PHOTOREC_SOURCE_FILES)
foreach(source_list IN ITEMS
    photorec_sources
    file_carver_sources
    base_sources
    filesystem_sources
    photorec_cli_sources)
  foreach(source_file IN LISTS ${source_list})
    list(APPEND PHOTOREC_SOURCE_FILES "${TESTDISK_SOURCE_DIR}/${source_file}")
  endforeach()
endforeach()
list(APPEND PHOTOREC_SOURCE_FILES "${TESTDISK_SOURCE_DIR}/suspend_no.c")

set_source_files_properties(
    "${TESTDISK_SOURCE_DIR}/phmain.c"
    PROPERTIES COMPILE_DEFINITIONS "main=photorec_cli_main"
)

add_library(photorec_jni SHARED ${PHOTOREC_SOURCE_FILES} photorec_jni.c)
target_include_directories(photorec_jni PRIVATE "${TESTDISK_SOURCE_DIR}")
target_compile_definitions(photorec_jni PRIVATE
  ${PHOTOREC_PLATFORM_DEFINITIONS}
  "VERSION=\"${TESTDISK_VERSION}\""
  "TESTDISKDATE=\"${TESTDISK_RELEASE_DATE}\"")
if(NOT ANDROID)
  find_package(JNI REQUIRED)
  target_include_directories(photorec_jni PRIVATE ${JNI_INCLUDE_DIRS})
endif()