package com.example.digitalpass

data class OnboardingItem(
    val title: String,
    val description: String,
    val layoutResId: Int
)

object OnboardingData {
    fun getOnboardingDataForRole(role: String?): List<OnboardingItem> {
        val userRole = role?.lowercase() ?: ""
        
        if (userRole == "student") {
            return listOf(
                OnboardingItem(
                    "Welcome to Digital Pass!",
                    "On your dashboard, you can see all your applied gate passes. Tap your profile picture at the top to add or update it.",
                    R.layout.activity_student
                ),
                OnboardingItem(
                    "Apply for Gate Pass",
                    "Tap 'Apply', choose Regular or Inter-Institutional Gate Pass. Enter your reason in the popup, submit, and your pass is requested!",
                    R.layout.activity_student
                ),
                OnboardingItem(
                    "Check Notifications",
                    "Tap the bell icon on the dashboard to check your latest notifications and pass approvals.",
                    R.layout.activity_notifications
                )
            )
        }
        
        if (userRole == "principal" || userRole == "hod" || userRole == "admin") {
            return listOf(
                OnboardingItem(
                    "Welcome to Digital Pass!",
                    "This is your central dashboard. From here, you can view recent gate passes, visitors, and inter-institutional passes.",
                    R.layout.activity_management_member
                ),
                OnboardingItem(
                    "Add New Users",
                    "Add a new user manually or add users in bulk via Excel upload.",
                    R.layout.activity_add_user
                ),
                OnboardingItem(
                    "Manage Users",
                    "Manage existing users easily.",
                    R.layout.activity_user_management
                ),
                OnboardingItem(
                    "View Batches",
                    "View and manage different batches across the campus.",
                    R.layout.activity_batch
                ),
                OnboardingItem(
                    "Check Comprehensive History",
                    "Check history logs for gate passes, visitors, and inter-institutional gate passes.",
                    R.layout.activity_user_history
                ),
                OnboardingItem(
                    "Self Gate Pass & Profile",
                    "Apply for your own gate pass and check your own gate pass history. Don't forget to update your profile picture from the dashboard!",
                    R.layout.activity_applied_gate_pass_by_self_user
                )
            )
        }
        
        if (userRole == "faculty") {
            return listOf(
                OnboardingItem(
                    "Welcome to Digital Pass!",
                    "This is your central dashboard. Tap your profile picture at the top right to access your options.",
                    R.layout.activity_management_member
                ),
                OnboardingItem(
                    "Student Management",
                    "Manage your students efficiently from the user management screen.",
                    R.layout.activity_user_management
                ),
                OnboardingItem(
                    "Check History",
                    "View complete history logs for gate passes and visitors.",
                    R.layout.activity_user_history
                ),
                OnboardingItem(
                    "Self Gate Pass & Profile",
                    "Apply for your own gate pass and track its history. Update your profile picture anytime from the dashboard.",
                    R.layout.activity_applied_gate_pass_by_self_user
                )
            )
        }
        
        if (userRole == "security guard") {
            return listOf(
                OnboardingItem(
                    "Welcome to Digital Pass!",
                    "This is your dashboard. Monitor campus exits, recent gate passes, visitors, and inter-institutional passes directly from here.",
                    R.layout.activity_security_guard
                ),
                OnboardingItem(
                    "Register New Visitors",
                    "Register a new visitor entry quickly by tapping the enter visitor button on your screen.",
                    R.layout.activity_enter_visitor
                ),
                OnboardingItem(
                    "Self Gate Pass & Profile",
                    "Need to leave? Apply for your own gate pass and check its history. You can also update your profile picture.",
                    R.layout.activity_applied_gate_pass_by_self_user
                )
            )
        }
        
        if (userRole == "reception") {
            return listOf(
                OnboardingItem(
                    "Welcome to Digital Pass!",
                    "This is your main dashboard. Easily view and manage the active visitor list right from this screen.",
                    R.layout.activity_reception
                ),
                OnboardingItem(
                    "Check History",
                    "Check the complete gate pass history and visitor history logs.",
                    R.layout.activity_user_history
                ),
                OnboardingItem(
                    "Self Gate Pass & Profile",
                    "Apply for your own gate pass and track its history. Manage your profile picture from the dashboard.",
                    R.layout.activity_applied_gate_pass_by_self_user
                )
            )
        }

        // Default fallback
        return listOf(
            OnboardingItem(
                "Welcome to Digital Pass!",
                "Your personal gateway to campus services.",
                R.layout.activity_student
            )
        )
    }
}
