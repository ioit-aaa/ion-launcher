package one.zagura.IonLauncher.provider

abstract class UpdatingResource<T> {
    private var callback: (T) -> Unit = {}
    fun track(doNow: Boolean = true, callback: (T) -> Unit) {
        this.callback = callback
        if (doNow)
            callback(getResource())
    }
    fun release() {
        this.callback = {}
    }
    fun update(res: T) = callback(res)
    abstract fun getResource(): T
}