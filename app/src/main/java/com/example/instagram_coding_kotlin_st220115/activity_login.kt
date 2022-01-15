 package com.example.instagram_coding_kotlin_st220115

 import android.annotation.SuppressLint
 import android.content.Intent
 import android.os.Bundle
 import android.widget.Button
 import android.widget.Toast
 import com.google.firebase.auth.*
 import java.util.*
import androidx.appcompat.app.AppCompatActivity
 import com.google.android.gms.auth.api.Auth
 import com.google.android.gms.auth.api.signin.GoogleSignIn
 import com.google.android.gms.auth.api.signin.GoogleSignInAccount
 import com.google.android.gms.auth.api.signin.GoogleSignInClient
 import com.google.android.gms.auth.api.signin.GoogleSignInOptions
 import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
 import com.google.firebase.auth.ktx.auth
 import com.google.firebase.ktx.Firebase
import android.content.pm.PackageManager

import android.content.pm.PackageInfo
 import android.media.tv.TvContract.Programs.Genres.encode
 import android.net.Uri.encode
 import android.util.Base64.encode
 import android.util.Log
 import com.facebook.AccessToken
 import com.facebook.CallbackManager
 import com.facebook.FacebookCallback
 import com.facebook.FacebookException
 import com.facebook.login.LoginManager
 import com.facebook.login.LoginResult
 import com.google.zxing.aztec.encoder.Encoder.encode
 import com.google.zxing.qrcode.encoder.Encoder.encode
 import java.lang.Exception
 import java.security.MessageDigest


 class activity_login : AppCompatActivity() {
     var auth : FirebaseAuth? = null
     var googleSignInClient : GoogleSignInClient? = null
     var GOOGLE_LOGIN_CODE = 9001
     var callBackManager : CallbackManager? = null
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_login)
         auth = Firebase.auth

         findViewById<Button>(R.id.email_login_button).setOnClickListener {
             signinAndSignup()
         }
         findViewById<Button>(R.id.google_sign_in_button).setOnClickListener {
                googleLogin()

         }
         findViewById<Button>(R.id.facebook_login_button).setOnClickListener{
              facebookLogin()
         }
         var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
             .requestIdToken(getString(R.string.default_web_client_id))
             .requestEmail()
             .build()
         googleSignInClient = GoogleSignIn.getClient(this,gso)
         callBackManager = CallbackManager.Factory.create()
     }

    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }
     fun facebookLogin() {
         LoginManager.getInstance()
         LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
         LoginManager.getInstance().registerCallback(callBackManager, object : FacebookCallback<LoginResult>{
             override fun onSuccess(loginResult: LoginResult) {
                 handleFacebookAccessToken(loginResult.accessToken)
             }

             override fun onCancel() {
             }

             override fun onError(error: FacebookException?) {
             }
         })
     }

     fun handleFacebookAccessToken(token: AccessToken?) {
         val credential = FacebookAuthProvider.getCredential(token!!.token)
         auth?.signInWithCredential(credential)
             ?.addOnCompleteListener {
                     task ->
                 //다음 페이지 이동
                 if (task.isSuccessful) {
                     moveMainPage(auth?.currentUser)
                 }
             }
     }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)
         callBackManager?.onActivityResult(resultCode,resultCode,data)
         if (requestCode == GOOGLE_LOGIN_CODE){
             var  result = data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(it) }
             if (result?.isSuccess == true){
                 var account = result?.signInAccount
                 firebaseAuthWithGoogle(account)
             }
         }
     }


     fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
         var credential = GoogleAuthProvider.getCredential(account?.idToken,null)
         auth?.signInWithCredential(credential)
             ?.addOnCompleteListener { task ->

                 if (task.isSuccessful) {
                     //로그인 성공 및 다음페이지 호출
                     moveMainPage(auth?.currentUser)
                 } else {
                     //로그인 실패
                     Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                 }
             }
     }

     fun signinAndSignup() {
         auth?.createUserWithEmailAndPassword(R.id.email_edittext.toString(), R.id.password_edittext.toString())
             ?.addOnCompleteListener {
                     task ->
                 if (task.isSuccessful) {
                     //아이디 생성이 성공했을 경우
                     Toast.makeText(
                         this,
                         getString(R.string.signup_complete), Toast.LENGTH_SHORT
                     ).show()

                     //다음페이지 호출
                     moveMainPage(auth?.currentUser)
                 } else if (task.exception?.message.isNullOrEmpty()) {
                     //회원가입 에러가 발생했을 경우
                     Toast.makeText(
                         this,
                         task.exception!!.message, Toast.LENGTH_SHORT
                     ).show()
                 } else {
                     //아이디 생성도 안되고 에러도 발생되지 않았을 경우 로그인
                     signinEmail()
                 }
             }

     }

     fun signinEmail() {
         auth?.signInWithEmailAndPassword(
             R.id.email_edittext.toString(),
             R.id.password_edittext.toString()
         )
             ?.addOnCompleteListener { task ->

                 if (task.isSuccessful) {
                     //로그인 성공 및 다음페이지 호출
                     moveMainPage(auth?.currentUser)
                 } else {
                     //로그인 실패
                     Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                 }
             }
     }

     fun moveMainPage(user: FirebaseUser?) {

         // User is signed in
         if (user != null) {
             Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_SHORT).show()
             startActivity(Intent(this, MainActivity::class.java))
             finish()
         }
     }
 }
