package com.coldmint.rust.pro

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.os.Bundle
import com.coldmint.rust.pro.databinding.ActivityUserHomePageBinding
import com.coldmint.rust.pro.base.BaseActivity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.coldmint.rust.core.dataBean.ApiResponse
import com.coldmint.rust.core.dataBean.follow.FollowUserListData
import com.coldmint.rust.core.dataBean.user.SpaceInfoData
import com.coldmint.rust.core.interfaces.ApiCallBack
import com.coldmint.rust.core.web.*
import com.coldmint.rust.pro.adapters.UserHomeStateAdapter
import com.coldmint.rust.pro.tool.AppSettings
import com.coldmint.rust.pro.tool.GlobalMethod
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.gyf.immersionbar.ktx.immersionBar


class UserHomePageActivity : BaseActivity<ActivityUserHomePageBinding>() {

    lateinit var userId: String
    lateinit var account: String
    var userName: String? = null
    var fans: Int = 0
    val userHomeStateAdapter by lazy {
        UserHomeStateAdapter(this, userId)
    }


    val appID by lazy {
        appSettings.getValue(AppSettings.Setting.AppID, "")
    }

    //旧的备份数据
    var oldSpaceInfoData: SpaceInfoData? = null

    @SuppressLint("CheckResult")
    private fun initView() {
        immersionBar {
            transparentStatusBar().statusBarDarkFont(true)
                .navigationBarColor(R.color.white_200).navigationBarDarkIcon(true)
        }

        val thisIntent = intent
        val temUserId = thisIntent.getStringExtra("userId")
        if (temUserId == null) {
            showError("请传入用户id")
            return
        } else {
            userId = temUserId
        }

        val temAccount = appSettings.getValue(AppSettings.Setting.Account, "")
        if (temAccount.isBlank()) {
            showError("请先登录")
            return
        } else {
            account = temAccount
        }
        viewBinding.toolbar.title = ""
        setSupportActionBar(viewBinding.toolbar)
        setReturnButton()
        val params = viewBinding.toolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
        params.setMargins(0, statusBarHeight(this), 0, 0)
        viewBinding.toolbar.layoutParams = params
        viewBinding.followLayout.setOnClickListener {
            openUserList(userId, true)

        }
        viewBinding.fansLayout.setOnClickListener {
            openUserList(userId, false)
        }
        initButton()


    }

    /**
     * 打开用户列表
     * @param account String 账号
     * @param isFollowMode Boolean 是否加载偶像
     */
    fun openUserList(account: String, isFollowMode: Boolean) {
        val bundle = Bundle()
        bundle.putString("account", account)
        bundle.putBoolean("isFollowMode", isFollowMode)
        val intent = Intent(this, UserListActivity::class.java)
        intent.putExtra("data", bundle)
        startActivity(intent)
    }

    /**
     * 初始化按钮
     */
    private fun initButton() {
        if (account == userId) {
            viewBinding.button.text = getString(R.string.editData)
        } else {
            Community.getFollowState(account, userId, object : ApiCallBack<ApiResponse> {
                override fun onResponse(t: ApiResponse) {
                    if (t.code == ServerConfiguration.Success_Code) {
                        val data = t.data
                        if (data != null && ServerConfiguration.isEvent(data)) {
                            when (data) {
                                "@event:已互粉" -> {
                                    viewBinding.button.text = getString(R.string.each_other_follow)
                                }
                                "@event:已关注" -> {
                                    viewBinding.button.text = getString(R.string.followed)
                                }
                                "@event:关注" -> {
                                    viewBinding.button.text = getString(R.string.follow)
                                }
                                "@event:拒绝关注" -> {
                                    viewBinding.button.text = getString(R.string.reject_follow)
                                    viewBinding.button.isEnabled = false
                                }
                            }
                        }
                    } else {

                    }
                }

                override fun onFailure(e: Exception) {

                }

            })
        }

    }

    /**
     * 展示用户数据到视图
     * @param spaceInfoData SpaceInfoData
     */
    fun showUserdataIfNeed(spaceInfoData: SpaceInfoData) {
        var isFirst = false
        if (oldSpaceInfoData == null) {
            isFirst = true
            oldSpaceInfoData = spaceInfoData
        } else {
            if (oldSpaceInfoData == spaceInfoData) {
                return
            }
        }
        userName = spaceInfoData.data.userName

        viewBinding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            viewBinding.toolbar.title =
                if ((Math.abs(verticalOffset) >= appBarLayout.totalScrollRange)) {
                    spaceInfoData.data.userName
                } else {
                    ""
                }
        })
        viewBinding.nameView.text = spaceInfoData.data.userName
        viewBinding.describeView.text =
            spaceInfoData.data.introduce ?: getString(R.string.defaultIntroduced)
        viewBinding.loginTimeView.text = String.format(
            getString(R.string.user_info),
            spaceInfoData.data.loginTime,
            spaceInfoData.data.account
        )

        viewBinding.fab.isVisible = true

        val gender = spaceInfoData.data.gender
        if (gender > 0) {
            Glide.with(this).load(R.drawable.boy).apply(GlobalMethod.getRequestOptions()).into(viewBinding.genderView)
        } else {
            Glide.with(this).load(R.drawable.girl).apply(GlobalMethod.getRequestOptions()).into(viewBinding.genderView)
        }

        val permission = spaceInfoData.data.permission
        when (permission) {
            1 -> {
                viewBinding.cardView.isVisible = true
                viewBinding.cardView.setCardBackgroundColor(Color.parseColor("#f47920"))
                viewBinding.positionView.setText(R.string.super_admin)
            }
            2 -> {
                viewBinding.cardView.isVisible = true
                viewBinding.cardView.setCardBackgroundColor(Color.parseColor("#ffd400"))
                viewBinding.positionView.setText(R.string.admin)
            }
            else -> {
                if (spaceInfoData.data.expirationTime == ServerConfiguration.ForeverTime) {
                    viewBinding.cardView.isVisible = true
                    viewBinding.cardView.setCardBackgroundColor(Color.parseColor("#33a3dc"))
                    viewBinding.positionView.setText(R.string.forever_time)
                }
            }
        }

        val icon = spaceInfoData.data.headIcon
        if (icon != null) {
            val iconLink = ServerConfiguration.getRealLink(icon)
            Glide.with(this@UserHomePageActivity).load(iconLink)
                .apply(GlobalMethod.getRequestOptions(true))
                .into(viewBinding.headIconView)
        }

        val cover = spaceInfoData.data.cover
        if (cover != null) {
            Glide.with(this).load(ServerConfiguration.getRealLink(cover)).apply(GlobalMethod.getRequestOptions())
                .into(viewBinding.coverView)
        }

        viewBinding.viewPager.adapter = userHomeStateAdapter


        viewBinding.coverView.setOnClickListener {
            if (cover != null) {
                val intent = Intent(this@UserHomePageActivity, FullScreenCoverActivity::class.java)
                val name = getString(R.string.transition_cover)
                val options =
                    ActivityOptions.makeSceneTransitionAnimation(this, viewBinding.coverView, name)
                intent.putExtra("iconLink", cover)
                this@UserHomePageActivity.startActivity(intent, options.toBundle())
            }
        }

        fans = spaceInfoData.data.fans
        viewBinding.fansNumView.text = ServerConfiguration.numberToString(spaceInfoData.data.fans)
        viewBinding.followNumView.text =
            ServerConfiguration.numberToString(spaceInfoData.data.follower)
        viewBinding.praiseNumView.text =
            ServerConfiguration.numberToString(spaceInfoData.data.praise)

        if (isFirst) {
            TabLayoutMediator(viewBinding.tabLayout, viewBinding.viewPager) { tab, position ->
                when (position) {
                    0 ->
                        tab.text = getString(R.string.homepage)
                    else -> {
                        tab.text = getString(R.string.dynamic)
                    }
                }
            }.attach()
            viewBinding.fab.hide()
            viewBinding.tabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val finalTab = tab
                    if (finalTab != null) {
                        val tiltle = finalTab.text
                        val dynamic = getString(R.string.dynamic)
                        if (dynamic == tiltle && userId == account) {
                            viewBinding.fab.show()
                        } else {
                            viewBinding.fab.hide()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }

            })
        }
    }

    override fun onResume() {
        super.onResume()
        User.getSpaceInfo(userId, object : ApiCallBack<SpaceInfoData> {
            override fun onResponse(t: SpaceInfoData) {

                if (t.code == ServerConfiguration.Success_Code) {
                    showUserdataIfNeed(t)
                } else {
                    showError(t.message)
                }
            }

            override fun onFailure(e: Exception) {
                showInternetError(null, e)
            }

        })
    }

    /**
     * 获取状态栏高度
     * @param context Context
     * @return Int
     */
    fun statusBarHeight(context: Context): Int {
        var height = 0
        val res = context.resources
        val resId = res.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            height = res.getDimensionPixelSize(resId)
        }
        return height
    }

    private fun initAction() {
        viewBinding.fab.setOnClickListener {
            MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                input(maxLength = 255).title(R.string.send_dynamic)
                    .positiveButton(R.string.dialog_ok)
                    .positiveButton {
                        val inputField: EditText = it.getInputField()
                        val text = inputField.text.toString()
                        if (!text.isBlank()) {
                            Dynamic.instance.send(
                                account,
                                appID,
                                text,
                                object : ApiCallBack<ApiResponse> {
                                    override fun onResponse(t: ApiResponse) {
                                        if (t.code == ServerConfiguration.Success_Code) {
                                            Snackbar.make(
                                                viewBinding.button,
                                                R.string.release_ok,
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                            userHomeStateAdapter.updataDynamicList()
                                        } else {
                                            Snackbar.make(
                                                viewBinding.button,
                                                t.message,
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onFailure(e: Exception) {
                                        showInternetError(viewBinding.button, e)
                                    }

                                })
                        }
                    }.negativeButton(R.string.dialog_cancel)

                val editText = this.getInputField()
                editText.inputType =
                    EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                editText.minLines = 3
                editText.gravity = Gravity.TOP
                editText.isSingleLine = false
            }
        }
        viewBinding.button.setOnClickListener {
            when (val type = viewBinding.button.text.toString()) {
                getString(R.string.follow) -> {
                    viewBinding.button.setBackgroundColor(
                        GlobalMethod.getThemeColor(
                            this,
                            R.attr.colorPrimaryVariant
                        )
                    )
                    viewBinding.button.setText(R.string.request_data)
                    Community.follow(account, userId, object : ApiCallBack<ApiResponse> {
                        override fun onResponse(t: ApiResponse) {
                            viewBinding.button.setBackgroundColor(
                                GlobalMethod.getColorPrimary(
                                    this@UserHomePageActivity
                                )
                            )
                            if (t.code == ServerConfiguration.Success_Code) {
                                fans++
                                viewBinding.fansNumView.text =
                                    ServerConfiguration.numberToString(fans)
                                viewBinding.button.text = getString(R.string.followed)
                            } else {
                                viewBinding.button.text = type
                                Snackbar.make(viewBinding.button, t.message, Snackbar.LENGTH_SHORT)
                                    .show()

                            }
                        }

                        override fun onFailure(e: Exception) {
                            showInternetError(viewBinding.button, e)
                            viewBinding.button.setBackgroundColor(
                                GlobalMethod.getColorPrimary(
                                    this@UserHomePageActivity
                                )
                            )
                            viewBinding.button.text = type
                        }

                    })
                }
                getString(R.string.followed), getString(R.string.each_other_follow) -> {
                    val de = String.format(getString(R.string.defollow_tip), userName ?: userId)
                    MaterialDialog(this).show {
                        title(R.string.defollow).message(text = de)
                            .positiveButton(R.string.dialog_ok).positiveButton {
                                viewBinding.button.setBackgroundColor(
                                    GlobalMethod.getThemeColor(
                                        this@UserHomePageActivity,
                                        R.attr.colorPrimaryVariant
                                    )
                                )
                                viewBinding.button.setText(R.string.request_data)
                                Community.deFollow(
                                    account,
                                    userId,
                                    object : ApiCallBack<ApiResponse> {
                                        override fun onResponse(t: ApiResponse) {
                                            viewBinding.button.setBackgroundColor(
                                                GlobalMethod.getColorPrimary(
                                                    this@UserHomePageActivity
                                                )
                                            )
                                            if (t.code == ServerConfiguration.Success_Code) {
                                                fans--
                                                viewBinding.fansNumView.text =
                                                    ServerConfiguration.numberToString(fans)
                                                viewBinding.button.text =
                                                    getString(R.string.follow)
                                            } else {
                                                Snackbar.make(
                                                    viewBinding.button,
                                                    t.message,
                                                    Snackbar.LENGTH_SHORT
                                                ).show()
                                                viewBinding.button.text = type
                                            }
                                        }

                                        override fun onFailure(e: Exception) {
                                            showInternetError(viewBinding.button, e)
                                        }

                                    })
                            }

                    }.negativeButton(R.string.dialog_cancel)
                }
                getString(R.string.editData) -> {
                    val intent = Intent(this, EditUserInfoActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                }
            }
        }
    }

    override fun getViewBindingObject(): ActivityUserHomePageBinding {
        return ActivityUserHomePageBinding.inflate(layoutInflater)
    }

    override fun whenCreateActivity(savedInstanceState: Bundle?, canUseView: Boolean) {
        if (canUseView) {
            initView()
            initAction()
        }
    }
}