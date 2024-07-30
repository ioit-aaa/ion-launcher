package one.zagura.IonLauncher.util

import java.util.concurrent.atomic.AtomicBoolean

class Cancellable {
    private val value = AtomicBoolean(false)
    val isCancelled get() = value.get()
    fun cancel() = value.set(true)
}

inline val Cancellable.isNotCancelled get() = !isCancelled