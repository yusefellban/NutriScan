package iti.grad.nutriscan.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        // 400 and 409 are intentionally NOT intercepted here.
        // They pass through to Retrofit so the repository layer can parse
        // the JSON error body (e.g., registration: "email already taken").
        when (response.code) {
            401 -> throw UnauthorizedException()
            403 -> throw ForbiddenException()
            404 -> throw NotFoundException()
            422 -> throw OcrLowConfidenceException()
            in 500..599 -> throw ServerException(response.code)
        }
        
        return response
    }
}
