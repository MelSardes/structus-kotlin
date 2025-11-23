/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.events

import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DomainEventHandlerTest {

    private data class TestEvent(
        override val eventId: String,
        override val occurredAt: Instant,
        override val aggregateId: String,
        val data: String
    ) : DomainEvent

    private data class OtherEvent(
        override val eventId: String,
        override val occurredAt: Instant,
        override val aggregateId: String
    ) : DomainEvent

    private class TestEventHandler : DomainEventHandler<TestEvent> {
        val handledEvents = mutableListOf<TestEvent>()

        override suspend fun handle(event: TestEvent) {
            handledEvents.add(event)
        }
    }

    private class OtherEventHandler : DomainEventHandler<OtherEvent> {
        val handledEvents = mutableListOf<OtherEvent>()

        override suspend fun handle(event: OtherEvent) {
            handledEvents.add(event)
        }
    }

    private class TestEventDispatcher {
        private val handlers = mutableMapOf<KClass<out DomainEvent>, MutableList<DomainEventHandler<*>>>()

        fun <E : DomainEvent> register(eventClass: KClass<E>, handler: DomainEventHandler<E>) {
            handlers.getOrPut(eventClass) { mutableListOf() }.add(handler)
        }

        suspend fun dispatch(event: DomainEvent) {
            handlers[event::class]?.forEach { handler ->
                @Suppress("UNCHECKED_CAST")
                (handler as DomainEventHandler<DomainEvent>).handle(event)
            }
        }
    }

    @Test
    fun `dispatcher should route event to correct handler`() = runTest {
        val testEventHandler = TestEventHandler()
        val otherEventHandler = OtherEventHandler()
        val dispatcher = TestEventDispatcher()

        dispatcher.register(TestEvent::class, testEventHandler)
        dispatcher.register(OtherEvent::class, otherEventHandler)

        val testEvent = TestEvent("evt-1", kotlin.time.Clock.System.now(), "agg-1", "test")
        dispatcher.dispatch(testEvent)

        assertEquals(1, testEventHandler.handledEvents.size)
        assertEquals(testEvent, testEventHandler.handledEvents[0])
        assertEquals(0, otherEventHandler.handledEvents.size)
    }

    @Test
    fun `dispatcher should not call handler for different event`() = runTest {
        val testEventHandler = TestEventHandler()
        val dispatcher = TestEventDispatcher()

        dispatcher.register(TestEvent::class, testEventHandler)

        val otherEvent = OtherEvent("evt-2", kotlin.time.Clock.System.now(), "agg-2")
        dispatcher.dispatch(otherEvent)

        assertEquals(0, testEventHandler.handledEvents.size)
    }

    @Test
    fun `dispatcher should call multiple handlers for the same event`() = runTest {
        val handler1 = TestEventHandler()
        val handler2 = TestEventHandler()
        val dispatcher = TestEventDispatcher()

        dispatcher.register(TestEvent::class, handler1)
        dispatcher.register(TestEvent::class, handler2)

        val testEvent = TestEvent("evt-1", kotlin.time.Clock.System.now(), "agg-1", "test")
        dispatcher.dispatch(testEvent)

        assertEquals(1, handler1.handledEvents.size)
        assertEquals(1, handler2.handledEvents.size)
    }
}
