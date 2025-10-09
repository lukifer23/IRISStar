package com.nervesparks.iris.llm

import android.llama.cpp.LLamaAndroid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class EmbeddingService @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    @Named("embedding_dispatcher") private val dispatcher: CoroutineDispatcher
) {
    suspend fun embed(text: String): List<Float> = withContext(dispatcher) {
        llamaAndroid.getEmbeddings(text).toList()
    }
}
