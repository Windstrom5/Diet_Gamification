    package com.example.diet_gamification.utils

    import com.example.diet_gamification.model.AccountModel
    import retrofit2.Response
    import retrofit2.http.Body
    import retrofit2.http.DELETE
    import retrofit2.http.GET
    import retrofit2.http.POST
    import retrofit2.http.PUT
    import retrofit2.http.Path
    import retrofit2.http.Query

    interface ApiService {
        // ----------- ACCOUNTS -----------
        @GET("/api/accounts")
        suspend fun getAccounts(): Response<List<Map<String, Any>>>

        @POST("/api/accounts")
        suspend fun register(@Body request: AccountModel): Response<Map<String, Any>>

        @POST("/api/accounts/login")
        suspend fun login(@Body request: Map<String, String>): Response<Map<String, Any>>

        @PUT("/api/accounts/{id}")
        suspend fun updateAccount(
            @Path("id") id: Int,
            @Body update: Map<String, Any>
        ): Response<Map<String, Any>>

        @DELETE("/api/accounts/{id}")
        suspend fun deleteAccount(@Path("id") id: Int): Response<Map<String, Any>>

        @GET("/api/accounts/verification")
        suspend fun verifyAccount(@Query("user") userJson: String): Response<String>
        @GET("/api/captcha")
        suspend fun getCaptcha(): Response<Map<String, String>>

        // ----------- CALORIES -----------
        @GET("/api/calories")
        suspend fun getCalories(): Response<List<Map<String, Any>>>

        @POST("/api/calories")
        suspend fun createCalorie(@Body request: Map<String, Any>): Response<Map<String, Any>>

        @GET("/api/calories/{id}")
        suspend fun getCalorieById(@Path("id") id: Int): Response<Map<String, Any>>

        @PUT("/api/calories/{id}")
        suspend fun updateCalorie(
            @Path("id") id: Int,
            @Body update: Map<String, Any>
        ): Response<Map<String, Any>>

        @DELETE("/api/calories/{id}")
        suspend fun deleteCalorie(@Path("id") id: Int): Response<Map<String, Any>>

        @POST("/api/calories/check")
        suspend fun checkCalories(@Body input: Map<String, Any>): Response<Map<String, Any>>

        // ----------- XP HISTORY (Laravel CRUD) -----------
        @GET("/api/xp-history")
        suspend fun getXpHistory(): Response<List<Map<String, Any>>>

        @POST("/api/xp-history")
        suspend fun createxpEntry(@Body request: Map<String, Any>): Response<Map<String, Any>>

        @GET("/api/xp-history/{id}")
        suspend fun getXpEntryById(@Path("id") id: Int): Response<Map<String, Any>>

        @PUT("/api/xp-history/{id}")
        suspend fun updatexpEntry(
            @Path("id") id: Int,
            @Body update: Map<String, Any>
        ): Response<Map<String, Any>>

        @DELETE("/api/xp-history/{id}")
        suspend fun deletexpEntry(@Path("id") id: Int): Response<Map<String, Any>>


        // ----------- WORKOUTS -----------
        @GET("/api/workouts")
        suspend fun getWorkouts(): Response<List<Map<String, Any>>>

        @POST("/api/workouts")
        suspend fun createWorkout(@Body request: Map<String, Any>): Response<Map<String, Any>>

        @GET("/api/workouts/{id}")
        suspend fun getWorkoutById(@Path("id") id: Int): Response<Map<String, Any>>

        @PUT("/api/workouts/{id}")
        suspend fun updateWorkout(
            @Path("id") id: Int,
            @Body update: Map<String, Any>
        ): Response<Map<String, Any>>

        @DELETE("/api/workouts/{id}")
        suspend fun deleteWorkout(@Path("id") id: Int): Response<Map<String, Any>>
    }
