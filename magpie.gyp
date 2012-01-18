# This is used by GYP to generate platform-specific project files for building
# Magpie. (I.e. on a Mac it will create an XCode project, on Linux a makefile.)
# See README for more.

{
  'xcode_settings': {
    'GCC_ENABLE_CPP_EXCEPTIONS': 'NO', # -fno-exceptions
    'GCC_ENABLE_CPP_RTTI': 'NO', # -fno-rtti
    'GCC_TREAT_WARNINGS_AS_ERRORS': 'YES',    # -Werror
    'GCC_WARN_CHECK_SWITCH_STATEMENTS': 'YES', # -Wswitch
    'WARNING_CFLAGS': [
      '-Wall',
      '-W',
      '-Wno-unused-parameter',
      '-Wnon-virtual-dtor',
    ],
  },
  'configurations': {
    'Debug': {
      'cflags': [ '-g', '-O0' ],
      'defines': [ 'DEBUG' ],
      'xcode_settings': {
        'GCC_OPTIMIZATION_LEVEL': '0',
      },
    },
    'Release': {
      'cflags': [ '-O3' ],
      'xcode_settings': {
        'GCC_OPTIMIZATION_LEVEL': '3',
      },
    },
  },
  'target_defaults': {
    'default_configuration': 'Debug',
    'configurations': {
      'Debug': {
      },
      'Release': {
      },
    },
    'include_dirs': [
      'src',
    ],
    'sources': [
      'src/Array.h',
      'src/Bytecode.h',
      'src/CallFrame.cpp',
      'src/CallFrame.h',
      'src/Chunk.cpp',
      'src/Chunk.h',
      'src/ChunkTable.cpp',
      'src/ChunkTable.h',
      'src/Fiber.cpp',
      'src/Fiber.h',
      'src/ForwardingAddress.h',
      'src/GC.h',
      'src/Heap.cpp',
      'src/Heap.h',
      'src/Macros.h',
      'src/magpie.1',
      'src/MagpieString.cpp',
      'src/MagpieString.h',
      'src/Managed.cpp',
      'src/Managed.h',
      'src/Memory.cpp',
      'src/Memory.h',
      'src/Multimethod.cpp',
      'src/Multimethod.h',
      'src/NumberObject.cpp',
      'src/NumberObject.h',
      'src/Object.cpp',
      'src/Object.h',
      'src/RootSource.h',
      'src/Stack.h',
      'src/Token.cpp',
      'src/Token.h',
      'src/VM.cpp',
      'src/VM.h',
    ],
  },
  'targets': [
    {
      'target_name': 'magpie',
      'type': 'executable',
      'sources': [
        'src/main.cpp',
      ],
    },
    {
      'target_name': 'unit_tests',
      'type': 'executable',
      'defines': [ 'UNIT_TEST' ],
      'include_dirs': [
        'src/Test',
      ],
      'sources': [
      'src/Test/StringTests.cpp',
      'src/Test/StringTests.h',
      'src/Test/Test.cpp',
      'src/Test/Test.h',
      'src/Test/TestMain.cpp',
      ],
    },
  ],
}
