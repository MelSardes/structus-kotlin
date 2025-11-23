/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.queries

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueryHandlerTest {

    private data class TestQuery(val id: String) : Query

    private data class TestDto(val id: String, val value: String)

    private class TestQueryHandler : QueryHandler<TestQuery, TestDto?> {
        private val data = mapOf(
            "1" to TestDto("1", "value1"),
            "2" to TestDto("2", "value2")
        )

        override suspend fun invoke(query: TestQuery): TestDto? {
            return data[query.id]
        }
    }

    @Test
    fun `handler should return data when found`() = runTest {
        val handler = TestQueryHandler()
        val query = TestQuery("1")

        val result = handler(query)

        assertEquals(TestDto("1", "value1"), result)
    }

    @Test
    fun `handler should return null when not found`() = runTest {
        val handler = TestQueryHandler()
        val query = TestQuery("999")

        val result = handler(query)

        assertNull(result)
    }

    @Test
    fun `handler should be suspendable`() = runTest {
        val handler = TestQueryHandler()
        val query = TestQuery("2")

        // This test verifies that the handler can be called in a coroutine context
        val result = handler(query)

        assertEquals(TestDto("2", "value2"), result)
    }
}
