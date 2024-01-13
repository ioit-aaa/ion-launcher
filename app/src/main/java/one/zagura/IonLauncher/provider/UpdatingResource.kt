package one.zagura.IonLauncher.provider

abstract class UpdatingResource<T> {
    private var callback: (T) -> Unit = {}
    fun track(callback: (T) -> Unit) {
        this.callback = callback
        callback(getResource())
    }
    fun release() {
        this.callback = {}
    }
    fun update(res: T) = callback(res)
    abstract fun getResource(): T
}