package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class MemberViewForVisitor : BaseActivity() {
    private lateinit var member: HashMap<String, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_member_view_for_visitor)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
         member=intent.getSerializableExtra("member") as HashMap<String,String>
        var image=findViewById<ImageView>(R.id.imageView)
        var name=findViewById<TextView>(R.id.name)
        var department=findViewById<TextView>(R.id.department)
        var phone=findViewById<TextView>(R.id.phone)
        var callIcon=findViewById<ImageView>(R.id.callIcon)
        var email=findViewById<TextView>(R.id.email)

        if(member["img"]?.trim()!="") Glide.with(this).load(LoginUserDataHolder.getURL(member["img"])).into(image)
        name.text=member["name"]
        department.text=member["department"]
        phone.text=member["phone"]
        email.text=member["email"]

        callIcon.setOnClickListener {
            //check the permission to make phone call
            if(checkPermission()) {

                //make a call to the phone number
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = android.net.Uri.parse("tel:${member["phone"]}")
                startActivity(intent)
            }
        }

        var selectionButton=findViewById<TextView>(R.id.selectionButton)
        selectionButton.setOnClickListener {
            val intent = Intent()
            //return member to previous activity with result
            intent.putExtra("member", member)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun checkPermission():Boolean {
        if(checkSelfPermission(android.Manifest.permission.CALL_PHONE)==android.content.pm.PackageManager.PERMISSION_GRANTED){
            return true
        }
        requestPermissions(arrayOf(android.Manifest.permission.CALL_PHONE),100)
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==100){
            if(grantResults.isNotEmpty()&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){
                //make a call to the phone number
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = android.net.Uri.parse("tel:${member["phone"]}")
                }
                startActivity(intent)
                }
            else Toast.makeText(this,"Permission required to make call",Toast.LENGTH_SHORT).show()
            }

    }

}