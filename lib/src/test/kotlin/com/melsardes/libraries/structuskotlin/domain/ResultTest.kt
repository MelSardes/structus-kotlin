/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun `success should create Success result`() {
        val result = Result.success(42)

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `failure should create Failure result`() {
        val exception = IllegalStateException("Test error")
        val result = Result.failure<Int>(exception)

        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getOrThrow should return value for Success`() {
        val result = Result.success(42)

        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun `getOrThrow should throw exception for Failure`() {
        val exception = IllegalStateException("Test error")
        val result = Result.failure<Int>(exception)

        assertFailsWith<IllegalStateException> {
            result.getOrThrow()
        }
    }

    @Test
    fun `getOrElse should return value for Success`() {
        val result = Result.success(42)

        val value = result.getOrElse { 0 }

        assertEquals(42, value)
    }

    @Test
    fun `getOrElse should return default for Failure`() {
        val exception = IllegalStateException("Test error")
        val result = Result.failure<Int>(exception)

        val value = result.getOrElse { 0 }

        assertEquals(0, value)
    }

    @Test
    fun `map should transform Success value`() {
        val result = Result.success(42)

        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(84, mapped.getOrNull())
    }

    @Test
    fun `map should preserve Failure`() {
        val exception = IllegalStateException("Test error")
        val result = Result.failure<Int>(exception)

        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertEquals(exception, mapped.exceptionOrNull())
    }

    @Test
    fun `runCatching should create Success for successful block`() {
        val result = runCatching { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching should create Failure for throwing block`() {
        val result = runCatching<Int> {
            throw RuntimeException("Test exception")
        }

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `chaining operations should work correctly`() {
        val result = runCatching { 10 }
            .map { it * 2 }
            .map { it + 5 }
            .map { it.toString() }

        assertTrue(result.isSuccess)
        assertEquals("25", result.getOrNull())
    }

    @Test
    fun `chaining should stop at first failure`() {
        val exception = IllegalStateException("Test error")
        val result = runCatching { 10 }
            .map { it * 2 }
            .mapCatching { throw exception }
            .map { "This should not be reached" }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
