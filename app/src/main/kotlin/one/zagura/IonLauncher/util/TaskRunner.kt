package one.zagura.IonLauncher.util

import java.lang.Thread.MAX_PRIORITY
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object TaskRunner {

    fun submit(task: () -> Unit) {
        lock.withLock {
            queue.addLast(task)
            if (!isRunning) {
                isRunning = true
                Thread(::run).apply {
                    priority = MAX_PRIORITY
                    isDaemon = false
                }.start()
            }
        }
    }

    fun submitUnique(task: () -> Unit) {
        lock.withLock {
            if (!queue.contains(task)) {
                queue.addLast(task)
                if (!isRunning) {
                    isRunning = true
                    Thread(::run).apply {
                        priority = MAX_PRIORITY
                        isDaemon = false
                    }.start()
                }
            }
        }
    }

    private val queue = ArrayDeque<() -> Unit>()
    private var isRunning = false
    private val lock = ReentrantLock()

    private fun run() {
        while (true) {
            lock.lock()
            if (queue.isEmpty()) {
                isRunning = false
                lock.unlock()
                return
            }
            val task = queue.removeFirst()
            lock.unlock()
            task()
        }
    }
}