package com.smartats.backend.exception

open class ApiException(
	message: String,
	val errorCode: ApiErrorCode,
	val retryable: Boolean = false,
	val userHint: String? = null,
) : RuntimeException(message)

class BadRequestException(
	message: String,
	errorCode: ApiErrorCode = ApiErrorCode.BAD_REQUEST,
	retryable: Boolean = true,
	userHint: String? = "请检查输入内容后重试。",
) : ApiException(message, errorCode, retryable, userHint)

class DuplicateResourceException(
	message: String,
	errorCode: ApiErrorCode = ApiErrorCode.RESOURCE_ALREADY_EXISTS,
	retryable: Boolean = true,
	userHint: String? = "请确认是否已存在相同数据，必要时更换后再试。",
) : ApiException(message, errorCode, retryable, userHint)

class InvalidCredentialsException(
	message: String,
	errorCode: ApiErrorCode = ApiErrorCode.AUTH_INVALID_CREDENTIALS,
	retryable: Boolean = true,
	userHint: String? = "请确认认证信息后重试。",
) : ApiException(message, errorCode, retryable, userHint)

class ResourceNotFoundException(
	message: String,
	errorCode: ApiErrorCode = ApiErrorCode.RESOURCE_NOT_FOUND,
	retryable: Boolean = false,
	userHint: String? = "请刷新页面并确认目标资源仍然存在。",
) : ApiException(message, errorCode, retryable, userHint)