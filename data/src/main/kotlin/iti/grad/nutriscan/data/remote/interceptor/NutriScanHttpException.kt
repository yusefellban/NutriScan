package iti.grad.nutriscan.data.remote.interceptor

import java.io.IOException

sealed class NutriScanHttpException(message: String) : IOException(message)

class UnauthorizedException : NutriScanHttpException("401 — token invalid or expired")
class ForbiddenException : NutriScanHttpException("403 — insufficient permissions")
class NotFoundException : NutriScanHttpException("404 — resource not found")
class OcrLowConfidenceException : NutriScanHttpException("422 — OCR confidence too low; retake required")
class ServerException(code: Int) : NutriScanHttpException("5xx Server Error: $code")
