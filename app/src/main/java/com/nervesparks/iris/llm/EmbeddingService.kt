package com.nervesparks.iris.llm

import android.llama.cpp.LLamaAndroid
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides embedding generation capabilities backed by the native LLama runtime.
 */
interface EmbeddingService {
    /** Generate an embedding vector for the supplied text. */
    suspend fun embed(text: String): List<Float>
}

class LlamaEmbeddingService @Inject constructor(
    private val llamaAndroid: LLamaAndroid
) : EmbeddingService {
    override suspend fun embed(text: String): List<Float> = withContext(Dispatchers.Default) {
        llamaAndroid.getEmbeddings(text).toList()
    }
}
