/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.commands

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandHandlerTest {

    private data class TestCommand(val value: Int) : Command

    private class TestCommandHandler : CommandHandler<TestCommand, Int> {
        override suspend fun invoke(command: TestCommand): Int {
            return command.value * 2
        }
    }

    @Test
    fun `handler should process command and return result`() = runTest {
        val handler = TestCommandHandler()
        val command = TestCommand(21)

        val result = handler(command)

        assertEquals(42, result)
    }

    @Test
    fun `handler should be suspendable`() = runTest {
        val handler = TestCommandHandler()
        val command = TestCommand(10)

        // This test verifies that the handler can be called in a coroutine context
        val result = handler(command)

        assertEquals(20, result)
    }
}
