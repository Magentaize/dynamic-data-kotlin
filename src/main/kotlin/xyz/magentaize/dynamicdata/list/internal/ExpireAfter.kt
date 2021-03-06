package xyz.magentaize.dynamicdata.list.internal

import xyz.magentaize.dynamicdata.kernel.scheduleRecurringAction
import xyz.magentaize.dynamicdata.list.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import xyz.magentaize.dynamicdata.kernel.ObservableEx
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration

internal class ExpireAfter<T>(
    private val _source: EditableObservableList<T>,
    private val _expireAfter: (T) -> Duration?,
    private val _pollingInterval: Duration?,
    private val _scheduler: Scheduler
) {
    fun run(): Observable<Iterable<T>> =
        ObservableEx.create { emitter ->
            var dateTime = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS))
            val orderItemWasAdded = AtomicLong(-1)
            val autoRemover = _source.connect()
                .doOnEach { dateTime = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS)) }
                .cast {
                    val removeAt = _expireAfter(it)
                    val expireAt =
                        if (removeAt != null) dateTime.plusMillis(removeAt.toLongMilliseconds()) else Instant.MAX
                    ExpirableItem(it, expireAt, orderItemWasAdded.getAndIncrement())
                }
                .asObservableList()

            fun removal() {
                try {
                    val now = Instant.ofEpochMilli(_scheduler.now(TimeUnit.MILLISECONDS))
                    val toRemove = autoRemover.items
                        .filter { it.expireAt.isBefore(now) || it.expireAt == now }
                        .map { it.item }
                        .toList()

                    emitter.onNext(toRemove)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }

            val removalSubscription =
                if (_pollingInterval != null)
                    _scheduler.scheduleRecurringAction(::removal, _pollingInterval)
                else
                    autoRemover.connect()
                        .distinctValues { it.expireAt }
                        .subscribeMany { time ->
                            val now = _scheduler.now(TimeUnit.MILLISECONDS)
                            val expireAt =
                                if (time != Instant.MAX) time.minusMillis(now).toEpochMilli() else Long.MAX_VALUE
                            Observable.timer(expireAt, TimeUnit.MILLISECONDS, _scheduler)
                                .take(1)
                                .subscribe { removal() }
                        }
                        .subscribe()

            return@create Disposable.fromAction {
                removalSubscription.dispose()
                autoRemover.dispose()
            }
        }
}
