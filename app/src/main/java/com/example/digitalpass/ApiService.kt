package com.example.digitalpass


import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("/login-user")
    fun loginUser(@Body loginData: LoginData): Call<HashMap<String, String>>
    @POST("/get-campus-and-department")
    fun getCampusAndDepartment(@Body token: String): Call<HashMap<String, ArrayList<String>>>
    @POST("/get-role-based-on-department")
    fun getRoleBasedOnDepartment(@Body department: HashMap<String, String>): Call<ArrayList<String>>
    @POST("/get-batches-based-on-department")
    fun getBatchesBasedOnDepartment(@Body cdr: HashMap<String, String>): Call<ArrayList<String>>

    @POST("/add-new-user")
    fun addNewUser(@Body usersData: HashMap<String, String>): Call<ResponseBody>

    @Multipart
    @POST("/upload-excel-users")
    fun uploadExcelUsers(
        @Part file: MultipartBody.Part,
        @Part("token") token: RequestBody
    ): Call<ResponseBody>

    @POST("/get-all-batches")
    fun getAllBatches(@Body hashToGetAllBatches: HashMap<String,String>): Call<HashMap<String,ArrayList<String>>>
    @POST("/remove-batch")
    fun removeBatch(@Body hashToRemoveBatch: HashMap<String, String>): Call<ResponseBody>

    @POST("/edit-batch")
    fun editBatch(@Body batchData: BatchData): Call<ResponseBody>
    @POST("/get-all-members-for-level")
    fun getAllMemberForLevel(@Body loginData: HashMap<String, String>): Call<ArrayList<HashMap<String, String>>>

    @POST("/get-data-for-batch")
    fun getDataForBatch(@Body loginData: String): Call<HashMap<String, ArrayList<String>>>

    @POST("/add-new-batch")
    fun addNewBatch(@Body batchData: BatchData): Call<ResponseBody>

    @POST("/get-members-for-user-management")
    fun getMembersForUserManagement(@Body loginData: String): Call<ArrayList<HashMap<String,String>>>

    @POST("/remove-user")
    fun removeUser(@Body hashToRemoveUser: HashMap<String, String>): Call<ResponseBody>

    @POST("/edit-user")
    fun editUser(@Body editedUser: HashMap<String, String>): Call<ResponseBody>

    @POST("/get-leveled-member")
    fun getLeveledMember(@Body hashToGetLeveledMember: HashMap<String, String>): Call<HashMap<String, ArrayList<HashMap<String, String>>>>

    @Multipart
    @POST("/upload-profile-image")
    fun uploadProfileImage(
        @Part file: MultipartBody.Part,
        @Part("token") token: RequestBody
    ): Call<ResponseBody>

    @POST("/get-campus-for-allotment")
    fun getCampusForAllotment(@Body token: String): Call<ArrayList<String>>

    @POST("/get-allotted-security-guard")
    fun getAllottedSecurityGuard(@Body hashToGetAllottedSecurityGuard: HashMap<String, String>): Call<HashMap<String, ArrayList<HashMap<String, String>>>>

    @POST("/save-allotted-security-guard")
    fun saveAllottedSecurityGuard(@Body hashToSaveAllottedSecurityGuard: HashMap<String, Any>): Call<ResponseBody>

    @POST("/check-permission-of-security-guard")
    fun checkPermissionOfSecurityGuard(@Body token: String): Call<ResponseBody>

    @POST("/get-all-member-for-visitor")
    fun getAllMemberForVisitor(@Body token: String): Call<ArrayList<HashMap<String, String>>>


    @Multipart
    @POST("/enter-visitor")
    fun enterVisitor(
        @Part("visitor") visitor: RequestBody,
        @Part("token") token: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @POST("/get-recent-visitor-list")
    fun getRecentVisitorList(@Body token: String): Call<ArrayList<HashMap<String, String>>>

    @POST("/meet-visitor")
    fun meetVisitor(@Body meetData: HashMap<String, String>): Call<ResponseBody>

    @Multipart
    @POST("/edit-visitor")
    fun editVisitor(
        @Part("visitor") visitor: RequestBody,
        @Part file: MultipartBody.Part?
    ): Call<ResponseBody>


    @POST("/get-recent-updated-visitor")
    fun getRecentUpdatedVisitor(@Body hashToGetRecentUpdatedVisitor: HashMap<String, String>): Call<HashMap<String, String>>

    @POST("/store-fcm-token")
    fun storeFCMToken(@Body hashToStoreFCMToken: HashMap<String, String>): Call<ResponseBody>

    @POST("/apply-for-gate-pass")
    fun applyForGatePass(@Body hashToApplyForGatePass: HashMap<String, String>): Call<HashMap<String,String>>

    @POST("/remove-gate-pass-by-self-user")
    fun removeGatePassBySelfUser(@Body hashToRemoveGatePassBySelfUser: HashMap<String, String>): Call<ResponseBody>

    @POST("/edit-gate-pass-by-self-user")
    fun editGatePassBySelfUser(@Body hashToEditGatePassBySelfUser: HashMap<String, String>): Call<ResponseBody>

    @POST("/get-self-user-gate-pass")
    fun getSelfUserGatePass(@Body token: String): Call<ArrayList<HashMap<String,String>>>

    @POST("/get-recent-gate-pass-list")
    fun getRecentGatePassList(@Body token: String):Call<ArrayList<HashMap<String,String>>>

    @POST("/reject-gate-pass")
    fun rejectGatePass(@Body hashToRejectGatePass: HashMap<String, String>): Call<ResponseBody>

    @POST("/approve-gate-pass")
    fun approveGatePass(@Body hashToApproveGatePass: HashMap<String, String>): Call<ResponseBody>

    @POST("/edit-gate-pass")
    fun editGatePass(@Body hashToEditGatePass: HashMap<String, String>): Call<ResponseBody>

    @POST("/get-recent-updated-gate-pass")
    fun getRecentUpdatedGatePass(@Body hashToGetRecentUpdatedGatePass: HashMap<String, String>): Call<HashMap<String, String>>

    @POST("/logout")
    fun logout(@Body token: String): Call<ResponseBody>

    @POST("/get-visitor-list-history")
    fun getVisitorListHistory(@Body hashToGetVisitorListHistory: HashMap<String, String>): Call<ArrayList<HashMap<String, String>>>
    @POST("/get-gate-pass-list-history")
    fun getGatePassListHistory(@Body hashToGetGatePassListHistory: HashMap<String, String>): Call<ArrayList<HashMap<String, String>>>

    @POST("/send-verification-code")
    fun sendVerificationCode(@Body email: String): Call<ResponseBody>
    @POST("/verify-verification-code")
    fun verifyVerificationCode(@Body hashToVerifyVerificationCode: HashMap<String, String>): Call<ResponseBody>

    @POST("/update-password")
    fun updatePassword(@Body hashToUpdatePassword: HashMap<String, String>): Call<String>
    //


}