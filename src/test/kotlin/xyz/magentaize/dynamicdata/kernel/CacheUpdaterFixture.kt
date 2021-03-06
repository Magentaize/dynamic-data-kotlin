package xyz.magentaize.dynamicdata.kernel

import xyz.magentaize.dynamicdata.cache.ChangeAwareCache
import xyz.magentaize.dynamicdata.cache.ChangeReason
import xyz.magentaize.dynamicdata.cache.internal.CacheUpdater
import xyz.magentaize.dynamicdata.domain.Person
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import kotlin.test.Test

internal class CacheUpdaterFixture {
    private val cache = ChangeAwareCache<String, Person>()
    private val updater = CacheUpdater(cache)
    private val key = "Adult1"

    @Test
    fun add() {
        val person = Person("Adult1", 50)
        updater.addOrUpdate(person, "Adult1")
        val updates = cache.captureChanges()

        cache.lookup("Adult1").value shouldBeEqualTo person
        cache.size shouldBe 1
        1 shouldBe updates.count()
        // TODO
        //updates.first() shouldBe Change(ChangeReason.Add, person.name, person)
    }

    @Test
    fun attemptedRemovalOfANonExistentKeyWillBeIgnored() {
        updater.remove(key)
        val updates = cache.captureChanges()

        cache.size shouldBe 0
        updates.size shouldBe 0
    }

    @Test
    fun remove() {
        val person = Person(key, 50)
        updater.addOrUpdate(person, key)
        updater.remove(key)
        val updates = cache.captureChanges()

        cache.size shouldBe 0
        1 shouldBe updates.count { it.reason == ChangeReason.Add }
        1 shouldBe updates.count { it.reason == ChangeReason.Remove }
        2 shouldBe updates.size
    }

    @Test
    fun update() {
        val newPerson = Person(key, 50)
        val updated = Person(key, 51)
        updater.addOrUpdate(newPerson, key)
        updater.addOrUpdate(updated, key)
        val updates = cache.captureChanges()

        cache.lookup(key).value shouldBe updated
        cache.size shouldBe 1
        1 shouldBe updates.count { it.reason == ChangeReason.Add }
        1 shouldBe updates.count { it.reason == ChangeReason.Update }
        2 shouldBe updates.size
    }
}
