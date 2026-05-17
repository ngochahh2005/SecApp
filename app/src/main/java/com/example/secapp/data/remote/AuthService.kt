package com.example.secapp.data.remote

import com.example.secapp.data.model.dto.AuthResponse
import com.example.secapp.data.model.dto.LoginRequest
import com.example.secapp.data.model.dto.SignupRequest
import com.example.secapp.data.model.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): UserResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
