/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EntityTest {

    private data class TestEntityId(val value: String)

    private class TestEntity(override val id: TestEntityId, val name: String = "test") : Entity<TestEntityId>()

    @Test
    fun `entities with same ID should be equal`() {
        val id = TestEntityId("123")
        val entity1 = TestEntity(id, "Entity 1")
        val entity2 = TestEntity(id, "Entity 2")

        assertEquals(entity1, entity2)
    }

    @Test
    fun `entities with different IDs should not be equal`() {
        val entity1 = TestEntity(TestEntityId("123"))
        val entity2 = TestEntity(TestEntityId("456"))

        assertNotEquals(entity1, entity2)
    }

    @Test
    fun `entities with same ID should have same hashCode`() {
        val id = TestEntityId("123")
        val entity1 = TestEntity(id, "Entity 1")
        val entity2 = TestEntity(id, "Entity 2")

        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun `entities with different IDs should have different hashCode`() {
        val entity1 = TestEntity(TestEntityId("123"))
        val entity2 = TestEntity(TestEntityId("456"))

        assertNotEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun `entity should equal itself`() {
        val entity = TestEntity(TestEntityId("123"))

        assertEquals(entity, entity)
    }

    @Test
    fun `entity should not equal null`() {
        val entity = TestEntity(TestEntityId("123"))

        assertNotEquals<Any?>(entity, null)
    }

    @Test
    fun `entity should not equal object of different type`() {
        val entity = TestEntity(TestEntityId("123"))
        val other = "not an entity"

        assertNotEquals<Any>(entity, other)
    }

    @Test
    fun `toString should include class name and ID`() {
        val entity = TestEntity(TestEntityId("123"))
        val string = entity.toString()

        assertTrue(string.contains("TestEntity"))
        assertTrue(string.contains("123"))
    }

    @Test
    fun `entities can be used in sets`() {
        val id = TestEntityId("123")
        val entity1 = TestEntity(id, "Entity 1")
        val entity2 = TestEntity(id, "Entity 2")

        val set = setOf(entity1, entity2)

        assertEquals(1, set.size)
    }

    @Test
    fun `entities can be used as map keys`() {
        val id = TestEntityId("123")
        val entity1 = TestEntity(id, "Entity 1")
        val entity2 = TestEntity(id, "Entity 2")

        val map = mutableMapOf<TestEntity, String>()
        map[entity1] = "value1"
        map[entity2] = "value2"

        assertEquals(1, map.size)
        assertEquals("value2", map[entity1])
    }
}
