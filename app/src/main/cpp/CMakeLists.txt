cmake_minimum_required(VERSION 3.18)
project("audioprocessor")

# 创建共享库
add_library(
        audio_processor
        SHARED
        audio_processor.cpp
)

# 查找 NDK 日志库
find_library(
        log-lib
        log
)

# 链接库
target_link_libraries(
        audio_processor
        ${log-lib}
)