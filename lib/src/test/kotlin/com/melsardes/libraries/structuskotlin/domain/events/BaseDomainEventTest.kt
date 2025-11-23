/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BaseDomainEventTest {

    private data class TestEvent(
        override val aggregateId: String,
        val data: String,
        val testCausationId: String? = null,
        val testCorrelationId: String? = null
    ) : BaseDomainEvent(
        aggregateId = aggregateId,
        aggregateType = "TestAggregate",
        eventVersion = 1,
        causationId = testCausationId,
        correlationId = testCorrelationId
    )

    @Test
    fun `should generate unique event ID`() {
        val event1 = TestEvent("agg-1", "data1")
        val event2 = TestEvent("agg-1", "data2")

        assertNotEquals(event1.eventId, event2.eventId)
    }

    @Test
    fun `should set occurred at timestamp`() {
        val before = kotlin.time.Clock.System.now()
        val event = TestEvent("agg-1", "data")
        val after = kotlin.time.Clock.System.now()

        assertTrue(event.occurredAt >= before)
        assertTrue(event.occurredAt <= after)
    }

    @Test
    fun `should store aggregate ID`() {
        val event = TestEvent("agg-123", "data")

        assertEquals("agg-123", event.aggregateId)
    }

    @Test
    fun `should store aggregate type`() {
        val event = TestEvent("agg-1", "data")

        assertEquals("TestAggregate", event.aggregateType)
    }

    @Test
    fun `should store event version`() {
        val event = TestEvent("agg-1", "data")

        assertEquals(1, event.eventVersion)
    }

    @Test
    fun `should store causation ID when provided`() {
        val event = TestEvent("agg-1", "data", testCausationId = "cmd-123")

        assertEquals("cmd-123", event.causationId)
    }

    @Test
    fun `should store correlation ID when provided`() {
        val event = TestEvent("agg-1", "data", testCorrelationId = "corr-123")

        assertEquals("corr-123", event.correlationId)
    }

    @Test
    fun `should have null causation ID when not provided`() {
        val event = TestEvent("agg-1", "data")

        assertEquals(null, event.causationId)
    }

    @Test
    fun `should have null correlation ID when not provided`() {
        val event = TestEvent("agg-1", "data")

        assertEquals(null, event.correlationId)
    }

    @Test
    fun `eventType should return class simple name`() {
        val event = TestEvent("agg-1", "data")

        assertEquals("TestEvent", event.eventType())
    }

    @Test
    fun `events with same data should be equal`() {
        val event1 = TestEvent("agg-1", "data")
        val event2 = event1.copy()

        assertEquals(event1, event2)
    }

    @Test
    fun `events with different data should not be equal`() {
        val event1 = TestEvent("agg-1", "data1")
        val event2 = TestEvent("agg-1", "data2")

        assertNotEquals(event1, event2)
    }

    @Test
    fun `events with same data should have same hashCode`() {
        val event1 = TestEvent("agg-1", "data")
        val event2 = event1.copy()

        assertEquals(event1.hashCode(), event2.hashCode())
    }

    @Test
    fun `toString should include event type and key fields`() {
        val event = TestEvent("agg-123", "data")
        val string = event.toString()

        assertTrue(string.contains("TestEvent"))
        assertTrue(string.contains("agg-123"))
        assertNotNull(event.eventId)
    }

    @Test
    fun `should support event versioning`() {
        val eventV1 = object : BaseDomainEvent(
            aggregateId = "agg-1",
            aggregateType = "TestAggregate",
            eventVersion = 1
        ) {}

        val eventV2 = object : BaseDomainEvent(
            aggregateId = "agg-1",
            aggregateType = "TestAggregate",
            eventVersion = 2
        ) {}

        assertEquals(1, eventV1.eventVersion)
        assertEquals(2, eventV2.eventVersion)
    }

    @Test
    fun `should support event correlation chain`() {
        val commandId = "cmd-123"
        val correlationId = "corr-456"

        val event1 = TestEvent(
            "agg-1",
            "data1",
            testCausationId = commandId,
            testCorrelationId = correlationId
        )

        val event2 = TestEvent(
            "agg-2",
            "data2",
            testCausationId = event1.eventId,
            testCorrelationId = correlationId
        )

        assertEquals(commandId, event1.causationId)
        assertEquals(correlationId, event1.correlationId)
        assertEquals(event1.eventId, event2.causationId)
        assertEquals(correlationId, event2.correlationId)
    }
}
