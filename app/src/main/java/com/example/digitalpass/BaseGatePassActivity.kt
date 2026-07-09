package com.example.digitalpass

import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class BaseGatePassActivity : BaseActivity() {

    protected lateinit var adapter: RecentPassAdapter
    protected var swipeRefreshLayout: SwipeRefreshLayout? = null
    protected var progressBar: CustomProgressBar? = null
    protected var applyButton: Button? = null
    protected var gatePassList = arrayListOf<HashMap<String, String>>()

    abstract val recyclerViewId: Int

    protected var getCommonData = { gatePass: HashMap<String, String> ->
        gatePass["img"] = LoginUserDataHolder.loginUserData?.get("img") ?: ""
        gatePass["name"] = LoginUserDataHolder.loginUserData?.get("name") ?: ""
        gatePass["applyEmail"] = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        gatePass["department"] = LoginUserDataHolder.loginUserData?.get("department") ?: ""
        gatePass["campus"] = LoginUserDataHolder.loginUserData?.get("campus") ?: ""
        gatePass["role"] = LoginUserDataHolder.loginUserData?.get("role") ?: ""
        gatePass["phone"] = LoginUserDataHolder.loginUserData?.get("phone") ?: ""
        gatePass
    }

    protected fun setupGatePassUI() {
        val recyclerView = findViewById<RecyclerView>(recyclerViewId)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter = RecentPassAdapter("selfGatePass", gatePassList)
        recyclerView.adapter = adapter

        passSyncViewModel.selfGatePasses.observe(this) { list ->
            progressBar?.stopAnimation()
            swipeRefreshLayout?.isRefreshing = false
            gatePassList.clear()
            for (gp in list) {
                gatePassList.add(HashMap(gp.passData))
            }
            adapter.updateList(gatePassList)
        }

        passSyncViewModel.gatePassSyncState.observe(this) { result ->
            result.onSuccess {
                val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
                passSyncViewModel.loadSelfGatePasses(email)
            }.onFailure {
                progressBar?.stopAnimation()
                swipeRefreshLayout?.isRefreshing = false
                Toast.makeText(this, it.message ?: "Failed to sync passes", Toast.LENGTH_SHORT).show()
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
            val token = LoginUserDataHolder.token
            passSyncViewModel.triggerGatePassSync(token)
        }

        val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        if (swipeRefreshLayout?.isRefreshing != true) {
            progressBar?.startProgressBar()
        }
        passSyncViewModel.loadSelfGatePasses(email)

        val token = LoginUserDataHolder.token
        passSyncViewModel.triggerGatePassSync(token)

        applyButton?.setOnClickListener {
            if (LoginUserDataHolder.loginUserData?.get("img")?.trim() == "") {
                Toast.makeText(this, "upload profile picture first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            showDialogueToGetType()
        }
    }

    protected fun showDialogueToGetType() {
        val types = arrayOf("Regular", "Inter Institution")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Gate Pass Type")
            .setItems(types) { _, which ->
                if (which == 0) {
                    showReasonDialog(null)
                } else {
                    fetchCampusesAndShowSelection()
                }
            }
            .show()
    }

    private fun fetchCampusesAndShowSelection() {
        fetchAndShowCampusSelection(progressBar) { destinationCampus ->
            showReasonDialog(destinationCampus)
        }
    }

    private fun showReasonDialog(destinationCampus: String?) {
        val dialogView = layoutInflater.inflate(R.layout.show_dialog_to_give_aproval_visitor, null)
        val dialogApplyButton = dialogView.findViewById<Button>(R.id.remarkDoneButton)
        val reason = dialogView.findViewById<EditText>(R.id.remark)

        //setup text of button hint for applying gate pass
        dialogApplyButton.text = "Apply"

        dialogView.findViewById<TextInputLayout>(R.id.nameInputLayout).hint = "Reason for gate pass"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        dialogApplyButton.setOnClickListener {
            if (reason.text.toString().trim() == "") {
                Toast.makeText(this, "Please enter reason", Toast.LENGTH_SHORT).show()
            } else {
                dialogApplyButton.isEnabled = false
                checkLocationAndApply(reason.text.toString().trim(), destinationCampus, dialogApplyButton)
                dialog.dismiss()
            }
        }
    }

    private fun checkLocationAndApply(reason: String, destinationCampus: String?, dialogApplyButton: Button) {
        progressBar?.startProgressBar()
        applyButton?.isEnabled = false

        requestUserLocation { location ->
            if (location != null) {
                applyForGatePass(reason, location.latitude.toString(), location.longitude.toString(), destinationCampus, dialogApplyButton)
            } else {
                progressBar?.stopAnimation()
                applyButton?.isEnabled = true
                dialogApplyButton.isEnabled = true
                Toast.makeText(this, "Unable to get location. Please ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyForGatePass(reason: String, latitude: String, longitude: String, destinationCampus: String?, dialogApplyButton: Button) {
        progressBar?.startProgressBar()
        applyButton?.isEnabled = false
        val map = hashMapOf(
            "reason" to reason,
            "token" to LoginUserDataHolder.token,
            "latitude" to latitude,
            "longitude" to longitude
        )
        if (destinationCampus != null) {
            map["destinationCampus"] = destinationCampus
        }
        val callToApplyGatePass = RetrofitClient.instance.applyForGatePass(map)
        callToApplyGatePass.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                progressBar?.stopAnimation()
                applyButton?.isEnabled = true
                dialogApplyButton.isEnabled = true
                if (response.isSuccessful) {
                    triggerSuccessAnimation(response.body()!!)
                } else {
                    Toast.makeText(this@BaseGatePassActivity, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {
                progressBar?.stopAnimation()
                applyButton?.isEnabled = true
                dialogApplyButton.isEnabled = true
                Toast.makeText(this@BaseGatePassActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun triggerSuccessAnimation(gatePassData: HashMap<String, String>) {
        val successOverlay = findViewById<android.view.View>(R.id.successOverlay)
        val successCircle = findViewById<android.view.View>(R.id.successCircle)
        val successCheckmark = findViewById<android.view.View>(R.id.successCheckmark)
        val glowRing = findViewById<android.view.View>(R.id.glowRing)
        val successTitle = findViewById<android.view.View>(R.id.successTitle)
        val successSubtitle = findViewById<android.view.View>(R.id.successSubtitle)

        val confettiViews = arrayOf(
            findViewById<android.view.View>(R.id.confetti1),
            findViewById<android.view.View>(R.id.confetti2),
            findViewById<android.view.View>(R.id.confetti3),
            findViewById<android.view.View>(R.id.confetti4),
            findViewById<android.view.View>(R.id.confetti5),
            findViewById<android.view.View>(R.id.confetti6)
        )

        // Reset elements
        successOverlay.visibility = android.view.View.VISIBLE
        successOverlay.alpha = 0f
        successCircle.scaleX = 0f
        successCircle.scaleY = 0f
        successCheckmark.scaleX = 0f
        successCheckmark.scaleY = 0f
        glowRing.scaleX = 0f
        glowRing.scaleY = 0f
        glowRing.alpha = 0f
        successTitle.alpha = 0f
        successTitle.translationY = 40f
        successSubtitle.alpha = 0f
        successSubtitle.translationY = 40f

        for (confetti in confettiViews) {
            confetti.visibility = android.view.View.INVISIBLE
            confetti.translationX = 0f
            confetti.translationY = 0f
            confetti.scaleX = 1f
            confetti.scaleY = 1f
            confetti.alpha = 1f
        }

        // 1. Fade in the background overlay
        successOverlay.animate().alpha(1f).setDuration(400).withEndAction {
            
            // 2. Spring-bounce success circle
            successCircle.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                .withEndAction {
                    
                    // 3. Spring-bounce white checkmark & reveal text
                    successCheckmark.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.8f))
                        .start()

                    successTitle.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .start()

                    successSubtitle.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .start()

                    // Expand glow ring
                    glowRing.alpha = 1f
                    glowRing.animate()
                        .scaleX(1.4f)
                        .scaleY(1.4f)
                        .alpha(0f)
                        .setDuration(800)
                        .start()

                    // 4. Confetti Explosion
                    val angles = arrayOf(30.0, 75.0, 120.0, 210.0, 285.0, 330.0)
                    val distance = 300f
                    for (i in confettiViews.indices) {
                        val confetti = confettiViews[i]
                        confetti.visibility = android.view.View.VISIBLE
                        val angleRad = Math.toRadians(angles[i])
                        val targetX = (Math.cos(angleRad) * distance).toFloat()
                        val targetY = (Math.sin(angleRad) * distance).toFloat()

                        confetti.animate()
                            .translationX(targetX)
                            .translationY(targetY)
                            .rotation(360f)
                            .alpha(0f)
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .setDuration(1200)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }

                    // 5. Dismiss after 2.8 seconds
                    successOverlay.postDelayed({
                        successOverlay.animate()
                            .alpha(0f)
                            .setDuration(600)
                            .withEndAction {
                                successOverlay.visibility = android.view.View.GONE
                                gatePassList.add(0, getCommonData(gatePassData))
                                adapter.updateList(gatePassList)
                                findViewById<RecyclerView>(recyclerViewId).smoothScrollToPosition(0)
                            }
                            .start()
                    }, 2800)
                }
                .start()
        }.start()
    }

    protected fun getGatePass() {
        val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        passSyncViewModel.loadSelfGatePasses(email)
        val token = LoginUserDataHolder.token
        passSyncViewModel.triggerGatePassSync(token)
    }
}
