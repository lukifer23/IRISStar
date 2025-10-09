package com.nervesparks.iris.llm

import android.llama.cpp.LLamaAndroid
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmbeddingServiceTest {

    @Test
    fun embedReturnsEmbeddingFromNativeLayer() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val llamaAndroid = mockk<LLamaAndroid>()
        every { llamaAndroid.getEmbeddings("hello world") } returns floatArrayOf(1.0f, 2.0f, 3.0f)

        val service = EmbeddingService(llamaAndroid, dispatcher)

        val result = service.embed("hello world")

        assertEquals(listOf(1.0f, 2.0f, 3.0f), result)
    }
}
