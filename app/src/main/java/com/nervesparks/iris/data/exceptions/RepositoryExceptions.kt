package com.nervesparks.iris.data.exceptions

/**
 * Base exception for repository operations
 */
sealed class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when a model file is not found
 */
class ModelNotFoundException(modelName: String, cause: Throwable? = null) : 
    RepositoryException("Model not found: $modelName", cause)

/**
 * Exception thrown when a model file is corrupted or invalid
 */
class InvalidModelException(modelName: String, cause: Throwable? = null) : 
    RepositoryException("Invalid model: $modelName", cause)

/**
 * Exception thrown when a chat is not found
 */
class ChatNotFoundException(chatId: Long, cause: Throwable? = null) : 
    RepositoryException("Chat not found: $chatId", cause)

/**
 * Exception thrown when a message is not found
 */
class MessageNotFoundException(messageId: Long, cause: Throwable? = null) : 
    RepositoryException("Message not found: $messageId", cause)

/**
 * Exception thrown when validation fails
 */
class ValidationException(message: String, cause: Throwable? = null) : 
    RepositoryException("Validation failed: $message", cause)

/**
 * Exception thrown when network operations fail
 */
class NetworkException(message: String, cause: Throwable? = null) : 
    RepositoryException("Network error: $message", cause)

/**
 * Exception thrown when storage operations fail
 */
class StorageException(message: String, cause: Throwable? = null) : 
    RepositoryException("Storage error: $message", cause)

/**
 * Exception thrown when configuration operations fail
 */
class ConfigurationException(message: String, cause: Throwable? = null) : 
    RepositoryException("Configuration error: $message", cause) 