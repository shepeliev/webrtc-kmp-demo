package com.shepeliev.webrtckmp.sample.shared

actual object Log {
    actual fun i(tag: String, msg: String, throwable: Throwable?) {
        log("I", tag, msg, throwable)
    }

    actual fun d(tag: String, msg: String, throwable: Throwable?) {
        log("D", tag, msg, throwable)
    }

    actual fun w(tag: String, msg: String, throwable: Throwable?) {
        log("W", tag, msg, throwable)
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        log("E", tag, msg, throwable)
    }

    private fun log(level: String, tag: String, msg: String, throwable: Throwable?) {
        println("$level: $tag $msg${exceptionText(throwable)}")
    }

    private fun exceptionText(throwable: Throwable?): String {
        if (throwable == null) return ""

        val cause = throwable.cause?.let { "\n\tCause: $it" } ?: ""
        val stacktrace = throwable.stackTraceToString()
        return "\n\tException: $throwable$cause\n\tStacktrace:\n\t\t$stacktrace"
    }
}
