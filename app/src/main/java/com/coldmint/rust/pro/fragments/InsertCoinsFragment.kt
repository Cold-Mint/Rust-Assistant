package com.coldmint.rust.pro.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.coldmint.dialog.CoreDialog
import com.coldmint.dialog.InputDialog
import com.coldmint.rust.core.dataBean.mod.CoinStatusData
import com.coldmint.rust.core.dataBean.mod.InsertCoinHistoryData
import com.coldmint.rust.core.interfaces.ApiCallBack
import com.coldmint.rust.core.web.WebMod
import com.coldmint.rust.pro.R
import com.coldmint.rust.pro.UserHomePageActivity
import com.coldmint.rust.pro.adapters.InsertCoinsAdapter
import com.coldmint.rust.pro.base.BaseFragment
import com.coldmint.rust.pro.databinding.FragmentInsertCoinsBinding
import com.coldmint.rust.pro.tool.AppSettings
import com.coldmint.rust.pro.ui.StableLinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration

/**
 * 投币碎片
 */
class InsertCoinsFragment(val modId: String) : BaseFragment<FragmentInsertCoinsBinding>() {
    private val token by lazy {
        AppSettings.getValue(AppSettings.Setting.Token, "")
    }

    override fun whenViewCreated(inflater: LayoutInflater, savedInstanceState: Bundle?) {
        viewBinding.recyclerView.layoutManager = StableLinearLayoutManager(requireContext())
        val divider = MaterialDividerItemDecoration(
            requireContext(),
            MaterialDividerItemDecoration.VERTICAL
        )

        viewBinding.recyclerView.addItemDecoration(
            divider
        )
        viewBinding.swipeRefreshLayout.setOnRefreshListener {
            loadList(false)
            viewBinding.swipeRefreshLayout.isRefreshing = false
        }
        viewBinding.button.setOnClickListener {
            InsertCoinsDialog(requireContext(), modId).setCallBackListener {
                if (it) {
                    viewBinding.button.isEnabled = false
                    viewBinding.tipView.text = getString(R.string.insert_coins_ok)
                    loadList()
                }
            }.show()
        }
        loadButton()
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    override fun getViewBindingObject(layoutInflater: LayoutInflater): FragmentInsertCoinsBinding {
        return FragmentInsertCoinsBinding.inflate(layoutInflater)
    }


    fun loadList(useLinearProgressIndicator: Boolean = true) {
        if (useLinearProgressIndicator) {
            viewBinding.linearProgressIndicator.isVisible = true
        }
        WebMod.instance.getInsertCoinHistory(modId, object : ApiCallBack<InsertCoinHistoryData> {
            override fun onResponse(t: InsertCoinHistoryData) {
                if (isAdded) {
                    val dataList = t.data
                    if (dataList.isNullOrEmpty()) {
                        viewBinding.recyclerView.isVisible = false
                        if (useLinearProgressIndicator) {
                            viewBinding.linearProgressIndicator.isVisible = false
                        }
                        viewBinding.loadLayout.isVisible = true
                        viewBinding.coinRecordsView.text = getString(R.string.coin_records)
                    } else {
                        val adapter = InsertCoinsAdapter(requireContext(), dataList)
                        adapter.setItemEvent { i, itemInsertCoinsBinding, viewHolder, data ->
                            itemInsertCoinsBinding.imageView.setOnClickListener {
                                gotoUserPage(data.account)
                            }
                        }
                        viewBinding.recyclerView.adapter =
                            adapter
                        val data = getString(R.string.coin_records) + "(" + dataList.size + ")"
                        viewBinding.coinRecordsView.text = data
                        viewBinding.recyclerView.isVisible = true
                        if (useLinearProgressIndicator) {
                            viewBinding.linearProgressIndicator.isVisible = false
                        }
                        viewBinding.loadLayout.isVisible = false
                    }
                }
            }

            override fun onFailure(e: Exception) {
                e.printStackTrace()
                viewBinding.recyclerView.isVisible = false
                viewBinding.coinRecordsView.text = getString(R.string.coin_records)
                if (useLinearProgressIndicator) {
                    viewBinding.linearProgressIndicator.isVisible = false
                }
                viewBinding.loadLayout.isVisible = true
            }

        })
    }

    /**
     * 打开用户主页
     * @param userId String
     */
    fun gotoUserPage(userId: String) {
        val intent = Intent(
            requireContext(),
            UserHomePageActivity::class.java
        )
        intent.putExtra("userId", userId)
        startActivity(
            intent
        )
    }

    fun loadButton() {
        if (token.isBlank()) {
            viewBinding.tipView.text = getString(R.string.please_login_first)
            viewBinding.button.isEnabled = false
            return
        }
        WebMod.instance.getCoinStatus(token, modId, object : ApiCallBack<CoinStatusData> {
            override fun onResponse(t: CoinStatusData) {
                viewBinding.button.isEnabled = !t.data
                if (t.data) {
                    viewBinding.tipView.text = getString(R.string.insert_coins_ok)
                } else {
                    viewBinding.tipView.text = getString(R.string.insert_coins_no)
                }
            }

            override fun onFailure(e: Exception) {
                e.printStackTrace()
                viewBinding.button.isEnabled = false
                viewBinding.tipView.text = getString(R.string.insert_coins_no)
            }

        })
    }


}