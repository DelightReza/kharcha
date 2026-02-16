package com.delightreza.kharcha.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

interface GitHubApi {
    
    // 1. Fetch Config dynamically using full URL
    @GET
    suspend fun fetchConfig(@Url url: String): AppConfig

    // 2. Fetch Data dynamically using constructed URL
    @GET
    suspend fun getPublicData(@Url url: String): KharchaData

    // 3. Get SHA/Content for Write Operations (Dynamic Path)
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileDetails(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): GitHubFileResponse

    // 4. Update File (Dynamic Path)
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFile(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: UpdateFileRequest
    )
}
