package dynamicdata.list.internal

import dynamicdata.kernel.Optional

internal data class ItemWithGroupKey<T, K>(
    val item: T,
    var group: K,
    val previousGroup: Optional<K>
) {
    override fun toString(): String =
        "$item ($group)"
}
