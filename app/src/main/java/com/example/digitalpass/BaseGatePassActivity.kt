package com.example.digitalpass

import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.digitalpass.CommonOperation.logout
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

        val interInstitutionalSwitch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.interInstitutionalSwitch)
        
        passSyncViewModel.selfInterInstitutional.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked == true) {
                progressBar?.stopAnimation()
                swipeRefreshLayout?.isRefreshing = false
                gatePassList.clear()
                gatePassList.addAll(list.map { HashMap(it.passData) })
                adapter.updateList(gatePassList)
            }
        }

        passSyncViewModel.interInstitutionalSyncState.observe(this) { result ->
            result.onSuccess {
                val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
                passSyncViewModel.loadSelfInterInstitutional(email)
                swipeRefreshLayout?.isRefreshing = false
            }.onFailure {
                if (interInstitutionalSwitch?.isChecked == true) {
                    progressBar?.stopAnimation()
                    swipeRefreshLayout?.isRefreshing = false
                    Toast.makeText(this, it.message ?: "Failed to sync passes", Toast.LENGTH_SHORT).show()
                }
            }
        }

        passSyncViewModel.selfGatePasses.observe(this) { list ->
            if (interInstitutionalSwitch?.isChecked != true) {
                progressBar?.stopAnimation()
                swipeRefreshLayout?.isRefreshing = false
                gatePassList.clear()
                gatePassList.addAll(list.map { HashMap(it.passData) })
                adapter.updateList(gatePassList)
            }
        }

        passSyncViewModel.gatePassSyncState.observe(this) { result ->
            result.onSuccess {
                val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
                passSyncViewModel.loadSelfGatePasses(email)
                swipeRefreshLayout?.isRefreshing = false
            }.onFailure {
                if (interInstitutionalSwitch?.isChecked != true) {
                    progressBar?.stopAnimation()
                    swipeRefreshLayout?.isRefreshing = false
                    Toast.makeText(this, it.message ?: "Failed to sync passes", Toast.LENGTH_SHORT).show()
                }
            }
        }

        swipeRefreshLayout?.setOnRefreshListener {
                passSyncViewModel.triggerInterInstitutionalSync(LoginUserDataHolder.token)
            passSyncViewModel.triggerGatePassSync(LoginUserDataHolder.token)
        }

        val email = LoginUserDataHolder.loginUserData?.get("email") ?: ""
        if (swipeRefreshLayout?.isRefreshing != true) {
            progressBar?.startProgressBar()
        }
        
        val token = LoginUserDataHolder.token
        passSyncViewModel.loadSelfInterInstitutional(email)
        passSyncViewModel.triggerInterInstitutionalSync(token)
        passSyncViewModel.loadSelfGatePasses(email)
        passSyncViewModel.triggerGatePassSync(token)

        interInstitutionalSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val currentList = passSyncViewModel.selfInterInstitutional.value ?: emptyList()
                gatePassList.clear()
                gatePassList.addAll(currentList.map { HashMap(it.passData) })
                adapter.updateList(gatePassList)
            } else {
                val currentList = passSyncViewModel.selfGatePasses.value ?: emptyList()
                gatePassList.clear()
                gatePassList.addAll(currentList.map { HashMap(it.passData) })
                adapter.updateList(gatePassList)
            }
        }

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_apply_gate_pass, null)
        val dialogApplyButton = dialogView.findViewById<Button>(R.id.applyDialogBtn)
        val reason = dialogView.findViewById<EditText>(R.id.reasonEditText)
        val locationProgressBar = dialogView.findViewById<ProgressBar>(R.id.locationProgressBar)
        val locationIcon = dialogView.findViewById<ImageView>(R.id.locationIcon)
        val locationStatusText = dialogView.findViewById<TextView>(R.id.locationStatusText)
        val locationVerificationCard = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationVerificationCard)

        var fetchedLocation: android.location.Location? = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        dialog.show()

        val chipIds = listOf(R.id.chip1, R.id.chip2, R.id.chip3, R.id.chip4)
        for (id in chipIds) {
            dialogView.findViewById<com.google.android.material.chip.Chip>(id)?.setOnClickListener { view ->
                val chip = view as com.google.android.material.chip.Chip
                reason.setText(chip.text)
                reason.setSelection(reason.text.length)
            }
        }

        requestUserLocation { location ->
            if (location != null) {
                fetchedLocation = location
                locationProgressBar.visibility = android.view.View.GONE
                locationIcon.visibility = android.view.View.VISIBLE
                locationIcon.setImageResource(android.R.drawable.checkbox_on_background)
                locationStatusText.text = "Location Verified"
                locationStatusText.setTextColor(android.graphics.Color.parseColor("#059669"))
                locationVerificationCard.setCardBackgroundColor(android.graphics.Color.parseColor("#D1FAE5"))
                locationVerificationCard.strokeColor = android.graphics.Color.parseColor("#A7F3D0")
                dialogApplyButton.isEnabled = true
            } else {
                locationProgressBar.visibility = android.view.View.GONE
                locationIcon.visibility = android.view.View.VISIBLE
                locationIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                locationStatusText.text = "Location Verification Failed"
                locationStatusText.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                locationVerificationCard.setCardBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                locationVerificationCard.strokeColor = android.graphics.Color.parseColor("#FECACA")

                MaterialAlertDialogBuilder(this)
                    .setTitle("Alert")
                    .setMessage("Location verification failed. Please check GPS permissions.")
                    .setNegativeButton("Ok", null)
                    .show()
            }
        }

        dialogApplyButton.setOnClickListener {
            if (reason.text.toString().trim() == "") {
                Toast.makeText(this, "Please enter reason", Toast.LENGTH_SHORT).show()
            } else {
                dialogApplyButton.isEnabled = false
                if (fetchedLocation != null) {
                    progressBar?.startProgressBar()
                    applyButton?.isEnabled = false
                    applyForGatePass(reason.text.toString().trim(), fetchedLocation!!.latitude.toString(), fetchedLocation!!.longitude.toString(), destinationCampus, dialogApplyButton)
                    dialog.dismiss()
                }
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
                    MaterialAlertDialogBuilder(this@BaseGatePassActivity)
                        .setTitle("Alert")
                        .setMessage(LoginUserDataHolder.getErrorMessage(response))
                        .setNegativeButton("Ok", null)
                        .show()
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

}
