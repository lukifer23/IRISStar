package com.nervesparks.iris.performance

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartMemoryManagerTest {

    @Test
    fun onTrimMemoryClearsCache() {
        val manager = SmartMemoryManager<String, String>(maxSize = 2)
        manager.put("a", "1")
        manager.put("b", "2")
        assertEquals(2, manager.size())

        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

        assertEquals(0, manager.size())
    }
}

