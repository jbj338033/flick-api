package com.flick.support.logging

import io.github.oshai.kotlinlogging.KotlinLogging

inline fun <reified T> T.logger() = KotlinLogging.logger {}
