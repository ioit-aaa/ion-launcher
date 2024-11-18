package one.zagura.IonLauncher.util

import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object TaskRunner : Thread() {

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

    private val queue = LinkedList<() -> Unit>()
    private var isRunning = false
    private val lock = ReentrantLock()

    override fun run() {
        while (true) {
            lock.lock()
            if (queue.peek() == null) {
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