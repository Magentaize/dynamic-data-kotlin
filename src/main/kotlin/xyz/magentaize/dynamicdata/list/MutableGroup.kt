package xyz.magentaize.dynamicdata.list

interface MutableGroup<T, out K> {
    val key: K
    val list: ObservableList<T>
}
