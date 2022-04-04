package com.coldmint.rust.pro.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.coldmint.rust.pro.R
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.afollestad.materialdialogs.MaterialDialog
import android.widget.TextView
import androidx.core.view.isVisible
import com.coldmint.rust.pro.base.BaseAdapter
import com.coldmint.rust.pro.databean.LibInfo
import com.coldmint.rust.pro.databinding.LibItemBinding
import java.util.ArrayList

class LibAdapter(val context: Context, dataList: ArrayList<LibInfo>) :
    BaseAdapter<LibItemBinding, LibInfo>(context, dataList) {

    override fun getViewBindingObject(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): LibItemBinding {
        return LibItemBinding.inflate(layoutInflater, parent, false)
    }

    override fun onBingView(
        data: LibInfo,
        viewBinding: LibItemBinding,
        viewHolder: BaseAdapter.ViewHolder<LibItemBinding>,
        position: Int
    ) {
        viewBinding.titleView.text = data.title
        viewBinding.descriptionView.text = data.description
        viewBinding.linkView.setOnClickListener {
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            val content_url = Uri.parse(data.link)
            intent.data = content_url
            context.startActivity(intent)
        }
        val agreement = data.agreement
        if (agreement.isEmpty()) {
            viewBinding.agreementView.isVisible = false
        } else {
            viewBinding.agreementView.text = agreement
        }
        val tip = data.tip
        if (tip != null) {
            viewBinding.tipView.isVisible = true
            viewBinding.tipView.setOnClickListener {
                MaterialDialog(context).show {
                    title(R.string.about).message(text = tip).negativeButton(R.string.close)
                }
            }
        }
    }
}