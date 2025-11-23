/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AggregateRootTest {

    private data class TestAggregateId(val value: String)

    private data class TestEvent(
        override val eventId: String,
        override val occurredAt: Instant,
        override val aggregateId: String,
        val data: String
    ) : DomainEvent

    private class TestAggregate(override val id: TestAggregateId) : AggregateRoot<TestAggregateId>() {
        fun performAction(data: String) {
            recordEvent(TestEvent(
                eventId = "evt-1",
                occurredAt = kotlin.time.Clock.System.now(),
                aggregateId = id.value,
                data = data
            ))
        }

        fun performMultipleActions() {
            recordEvent(TestEvent("evt-1", kotlin.time.Clock.System.now(), id.value, "action1"))
            recordEvent(TestEvent("evt-2", kotlin.time.Clock.System.now(), id.value, "action2"))
            recordEvent(TestEvent("evt-3", kotlin.time.Clock.System.now(), id.value, "action3"))
        }
    }

    @Test
    fun `should record events when domain operation is performed`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        aggregate.performAction("test data")

        assertEquals(1, aggregate.eventCount())
        assertTrue(aggregate.hasEvents())
        assertEquals(1, aggregate.domainEvents.size)
    }

    @Test
    fun `should clear events after publishing`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test data")

        assertEquals(1, aggregate.eventCount())

        aggregate.clearEvents()

        assertEquals(0, aggregate.eventCount())
        assertFalse(aggregate.hasEvents())
        assertTrue(aggregate.domainEvents.isEmpty())
    }

    @Test
    fun `should maintain event order`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        aggregate.performMultipleActions()

        assertEquals(3, aggregate.eventCount())
        val events = aggregate.domainEvents
        assertEquals("evt-1", events[0].eventId)
        assertEquals("evt-2", events[1].eventId)
        assertEquals("evt-3", events[2].eventId)
    }

    @Test
    fun `should start with no events`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertEquals(0, aggregate.eventCount())
        assertFalse(aggregate.hasEvents())
        assertTrue(aggregate.domainEvents.isEmpty())
    }

    @Test
    fun `should allow multiple event recordings`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        aggregate.performAction("action1")
        aggregate.performAction("action2")
        aggregate.performAction("action3")

        assertEquals(3, aggregate.eventCount())
        assertTrue(aggregate.hasEvents())
    }

    @Test
    fun `should return immutable event list`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test")

        val events1 = aggregate.domainEvents
        val events2 = aggregate.domainEvents

        // Should return different list instances
        assertTrue(events1 !== events2)
        // But with same content
        assertEquals(events1, events2)
    }

    @Test
    fun `should preserve events after retrieval`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test")

        val events = aggregate.domainEvents
        assertEquals(1, events.size)

        // Events should still be there
        assertEquals(1, aggregate.eventCount())
        assertTrue(aggregate.hasEvents())
    }

    @Test
    fun `should allow clearing events multiple times`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test")

        aggregate.clearEvents()
        assertEquals(0, aggregate.eventCount())

        aggregate.clearEvents()
        assertEquals(0, aggregate.eventCount())
    }

    @Test
    fun `should record events after clearing`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test1")
        aggregate.clearEvents()

        aggregate.performAction("test2")

        assertEquals(1, aggregate.eventCount())
        assertEquals("evt-1", aggregate.domainEvents[0].eventId)
    }

    @Test
    fun `hasEvents should return false when no events`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertFalse(aggregate.hasEvents())
    }

    @Test
    fun `hasEvents should return true when events exist`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.performAction("test")

        assertTrue(aggregate.hasEvents())
    }

    // Lifecycle Management Tests

    @Test
    fun `markAsCreated should set creation audit fields`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        val before = kotlin.time.Clock.System.now()

        aggregate.markAsCreated(by = "user-123")

        assertTrue(aggregate.createdAt!! >= before)
        assertEquals("user-123", aggregate.createdBy)
        assertTrue(aggregate.updatedAt!! >= before)
        assertEquals("user-123", aggregate.updatedBy)
    }

    @Test
    fun `markAsCreated should accept custom timestamp`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        val customTime = kotlin.time.Clock.System.now()

        aggregate.markAsCreated(by = "user-123", at = customTime)

        assertEquals(customTime, aggregate.createdAt)
        assertEquals(customTime, aggregate.updatedAt)
    }

    @Test
    fun `markAsUpdated should set update audit fields`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.markAsCreated(by = "user-123")
        val before = kotlin.time.Clock.System.now()

        aggregate.markAsUpdated(by = "user-456")

        assertTrue(aggregate.updatedAt!! >= before)
        assertEquals("user-456", aggregate.updatedBy)
        assertEquals("user-123", aggregate.createdBy) // Should not change
    }

    @Test
    fun `softDelete should mark aggregate as deleted`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        val before = kotlin.time.Clock.System.now()

        aggregate.softDelete(by = "user-123")

        assertTrue(aggregate.isDeleted())
        assertFalse(aggregate.isActive())
        assertTrue(aggregate.deletedAt!! >= before)
        assertEquals("user-123", aggregate.deletedBy)
    }

    @Test
    fun `softDelete should fail if already deleted`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.softDelete(by = "user-123")

        try {
            aggregate.softDelete(by = "user-456")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Aggregate is already deleted", e.message)
        }
    }

    @Test
    fun `restore should restore deleted aggregate`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.softDelete(by = "user-123")
        assertTrue(aggregate.isDeleted())

        aggregate.restore(by = "user-456")

        assertFalse(aggregate.isDeleted())
        assertTrue(aggregate.isActive())
        assertEquals(null, aggregate.deletedAt)
        assertEquals(null, aggregate.deletedBy)
        assertEquals("user-456", aggregate.updatedBy)
    }

    @Test
    fun `restore should fail if not deleted`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        try {
            aggregate.restore(by = "user-123")
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Aggregate is not deleted", e.message)
        }
    }

    @Test
    fun `isDeleted should return false for new aggregate`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertFalse(aggregate.isDeleted())
    }

    @Test
    fun `isActive should return true for new aggregate`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertTrue(aggregate.isActive())
    }

    @Test
    fun `incrementVersion should increase version number`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertEquals(0, aggregate.version)

        aggregate.incrementVersion()
        assertEquals(1, aggregate.version)

        aggregate.incrementVersion()
        assertEquals(2, aggregate.version)

        aggregate.incrementVersion()
        assertEquals(3, aggregate.version)
    }

    @Test
    fun `version should start at zero`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertEquals(0, aggregate.version)
    }

    @Test
    fun `audit fields should be null initially`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))

        assertEquals(null, aggregate.createdAt)
        assertEquals(null, aggregate.createdBy)
        assertEquals(null, aggregate.updatedAt)
        assertEquals(null, aggregate.updatedBy)
        assertEquals(null, aggregate.deletedAt)
        assertEquals(null, aggregate.deletedBy)
    }

    @Test
    fun `softDelete should update updatedAt and updatedBy`() {
        val aggregate = TestAggregate(TestAggregateId("agg-1"))
        aggregate.markAsCreated(by = "user-123")
        val before = kotlin.time.Clock.System.now()

        aggregate.softDelete(by = "user-456")

        assertTrue(aggregate.updatedAt!! >= before)
        assertEquals("user-456", aggregate.updatedBy)
    }
}
