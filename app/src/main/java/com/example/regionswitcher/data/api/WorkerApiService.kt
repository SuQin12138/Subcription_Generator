package com.example.regionswitcher.data.api

import com.example.regionswitcher.data.api.model.WorkerConfigPayload
import com.example.regionswitcher.data.api.model.WorkerUpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface WorkerApiService {

    @GET
    suspend fun fetchConfig(@Url fullUrl: String): Response<WorkerConfigPayload>

    @Headers("Content-Type: application/json")
    @POST
    suspend fun updateConfig(
        @Url fullUrl: String,
        @Body payload: WorkerConfigPayload
    ): Response<WorkerUpdateResponse>
}