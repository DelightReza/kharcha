package com.delightreza.kharcha.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query

interface GitHubApi {
    @GET("https://delightreza.github.io/kharcha/data.json")
    suspend fun getPublicData(@Query("t") timestamp: Long): KharchaData

    // Used for Verification AND getting SHA
    @GET("repos/DelightReza/kharcha/contents/data.json")
    suspend fun getFileDetails(
        @Header("Authorization") token: String
    ): GitHubFileResponse

    @PUT("repos/DelightReza/kharcha/contents/data.json")
    suspend fun updateFile(
        @Header("Authorization") token: String,
        @Body body: UpdateFileRequest
    )
}
