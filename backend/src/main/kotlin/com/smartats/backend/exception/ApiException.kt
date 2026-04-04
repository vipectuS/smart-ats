package com.smartats.backend.exception

open class ApiException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(message)

class DuplicateResourceException(message: String) : ApiException(message)

class InvalidCredentialsException(message: String) : ApiException(message)

class ResourceNotFoundException(message: String) : ApiException(message)