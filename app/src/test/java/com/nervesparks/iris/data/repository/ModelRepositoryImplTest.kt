package com.nervesparks.iris.data.repository

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.repository.impl.ModelRepositoryImpl
import io.mockk.coEvery
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRepositoryImplTest {
    @Test
    fun loadModel_success() = runBlocking {
        val llama = mockk<LLamaAndroid>()
        coEvery { llama.load(any(), any(), any(), any(), any()) } justRun
        val repo = ModelRepositoryImpl(llama)
        val result = repo.loadModel("/tmp/model.gguf")
        assertTrue(result.isSuccess)
    }

    @Test
    fun loadModel_failure() = runBlocking {
        val llama = mockk<LLamaAndroid>()
        coEvery { llama.load(any(), any(), any(), any(), any()) } throws IllegalStateException("fail")
        val repo = ModelRepositoryImpl(llama)
        val result = repo.loadModel("/tmp/model.gguf")
        assertTrue(result.isFailure)
    }
}
