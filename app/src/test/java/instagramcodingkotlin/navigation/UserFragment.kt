package com.company.howl.howlstagram.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.provider.Settings.Global.getString
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.company.howl.howlstagram.LoginActivity
import com.company.howl.howlstagram.MainActivity
import com.company.howl.howlstagram.R
import com.company.howl.howlstagram.R.id.account_tv_post_count
import com.company.howl.howlstagram.model.AlarmDTO

import com.company.howl.howlstagram.model.ContentDTO
import com.company.howl.howlstagram.model.FollowDTO
import com.company.howl.howlstagram.util.FcmPush
import com.example.instagramcodingkotlin.MainActivity
import com.example.instagramcodingkotlin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import java.util.ArrayList

class UserFragment : Fragment() {

    // Firebase
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    //private String destinationUid;
    var uid: String? = null
    var currentUserUid: String? = null
    var fragmentView: View? = null
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) {

        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)
        uid = arguments!!.getString("destinationUid")
        // Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fcmPush = FcmPush()

        currentUserUid = auth?.currentUser?.uid

        fragmentView?.account_reyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_reyclerview?.layoutManger = GridLayoutManager(activity!!,3)

        if (arguments != null) {

            uid = arguments!!.getString("destinationUid")

            // 본인 계정인 경우 -> 로그아웃, Toolbar 기본으로 설정
            if (uid != null && uid == currentUserUid) {

                fragmentView!!.account_btn_follow_signout.text = getString(R.string.signout)
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    startActivity(Intent(activity, LoginActivity::class.java))
                    activity?.finish()
                    auth?.signOut()
                }
            } else {
                fragmentView!!.account_btn_follow_signout.text = getString(R.string.follow)
                //view.account_btn_follow_signout.setOnClickListener{ requestFollow() }
                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE

                mainActivity.toolbar_username.text = arguments!!.getString("userId")

                mainActivity.toolbar_btn_back.setOnClickListener { mainActivity.bottom_navigation.selectedItemId = R.id.action_home }

                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    requestFollow()
                }
            }

        }
        // Profile Image Click Listener
        fragmentView?.account_iv_profile?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                //앨범 오픈
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                activity!!.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
            }
        }
        getFollowing()
        getFollower()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()

        return fragmentView
    }



    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val contentDTOs: ArrayList<ContentDTO>

        init {

            contentDTOs = ArrayList()

            // 나의 사진만 찾기
            recyclerListenerRegistration = firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                if (querySnapshot == null) return@addSnapshotListener
                for (snapshot in querySnapshot?.documents!!) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                getProfileImage()
                getFollowerAndFollowing()
                fragmentView?.account_tv_post_count.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }

        }

        fun getFollowerAndFollowing{
            firestore?.collection("users")?.document(uid!!).addSnapshotListener{documentSnapshot,firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
                if(FollowDTO?.followingCount !=null){
                    fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
                }
                if(FollowDTO?.followerCount !=null){
                    fragmentView?.account_tv_following_count?.text = followDTO?.followerCount?.toString()
                    if(followDTO?.followers?.containsKey(currentUserUid!!)){
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                        fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompag.getcolor(activity!!,R.color.coloerLightGray),porterDuff.Mode.MULTIPLY)

                    }else{
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    }
                }
            }
        }

         fun requstFollow() {
            var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
            firestore?.runTransaction { transaction ->

                var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
                if (followDTO == null) {

                    followDTO = FollowDTO()
                    followDTO.followingCount = 1
                    followDTO.followings[uid!!] = true

                    transaction.set(tsDocFollowing, followDTO)
                    return@runTransaction

                }
                // Unstar the post and remove self from stars
                if (followDTO?.followings?.containsKey(uid)!!) {

                    followDTO?.followingCount = followDTO?.followingCount - 1
                    followDTO?.followings.remove(uid)
                } else {

                    followDTO?.followingCount = followDTO?.followingCount + 1
                    followDTO?.followings[uid!!] = true
                    followerAlarm(uid!!)
                }
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            var tsDocFollower = firestore!!.collection("users").document(uid!!)
            firestore?.runTransaction { transaction ->

                var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
                if (followDTO == null) {

                    followDTO = FollowDTO()
                    followDTO!!.followerCount = 1
                    followDTO!!.followers[currentUserUid!!] = true
                    followerAlarm(uid!!)


                    transaction.set(tsDocFollower, followDTO!!)
                    return@runTransaction
                }

                if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {


                    followDTO!!.followerCount = followDTO!!.followerCount - 1
                    followDTO!!.followers.remove(currentUserUid!!)
                } else {

                    followDTO!!.followerCount = followDTO!!.followerCount + 1
                    followDTO!!.followers[currentUserUid!!] = true
                    followerAlarm(uid!!)

                }// Star the post and add self to stars

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

        }

        fun followerAlarm(destinationUid: String) {

            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = auth?.currentUser!!.email
            alarmDTO.uid = auth?.currentUser!!.uid
            alarmDTO.kind = 2
            alarmDTO.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            var message = auth?.currentUser!!.email + getString(R.string.alarm_follow)
            fcmPush?.sendMessage(destinationUid, "알림 메세지 입니다.", message)
        }


        fun getProfileImage() {
            imageprofileListenerRegistration = firestore?.collection("profileImages")?.document(uid!!)
                ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                    if (documentSnapshot?.data != null) {
                        val url = documentSnapshot?.data!!["image"]
                        Glide.with(activity)
                            .load(url)
                            .apply(RequestOptions().circleCrop()).into(fragmentView!!.account_iv_profile)
                    }
                }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageview)
        }

        override fun getItemCount(): Int {

            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
            super.onBindViewHolder(holder, position, payloads
                var imageview = (p0 as CustomViewHolder).imageview
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

        // RecyclerView Adapter - View Holder
        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followingListenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }

}