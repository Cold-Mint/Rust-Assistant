package com.coldmint.rust.pro

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.coldmint.rust.core.debug.LogCat
import com.coldmint.rust.pro.adapters.GuideAdapter
import com.coldmint.rust.pro.base.BaseActivity
import com.coldmint.rust.pro.databean.GuideData
import com.coldmint.rust.pro.databinding.ActivityCreationWizardBinding
import com.coldmint.rust.pro.tool.AppSettings
import com.coldmint.rust.pro.ui.StableLinearLayoutManager

class CreationWizardActivity : BaseActivity<ActivityCreationWizardBinding>() {

    lateinit var createMod: ActivityResultLauncher<Intent>

    //创建向导类型（模组，模板包）
    lateinit var type: String
    override fun whenCreateActivity(savedInstanceState: Bundle?, canUseView: Boolean) {
        setReturnButton()
        title = getString(R.string.creation_assistant)
        createMod = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                finish()
                LogCat.d("创建单位", "收到了数据，关闭界面。")
            } else {
                LogCat.d("创建单位", "没有收到数据。")
            }
        }
        val temType = intent.getStringExtra("type")
        if (temType.isNullOrBlank()) {
            showToast("请传入类型")
            finish()
            return
        }
        type = temType
        viewBinding.recyclerView.layoutManager = StableLinearLayoutManager(this)
        when (temType) {
            "mod" -> {
                loadMod()
            }
            "template" -> {
                loadTemplate()
            }
        }
    }

    /**
     * 加载模板活动
     */
    fun loadTemplate() {
        val dataList = ArrayList<GuideData>()
        dataList.add(
            GuideData(
                R.string.create_template,
                R.string.create_template_describe,
                R.drawable.ic_outline_create_24
            )
        )
        dataList.add(
            GuideData(
                R.string.import_template,
                R.string.import_template_describe,
                R.drawable.folder
            )
        )
        val adapter = GuideAdapter(this, dataList)
        adapter.setItemEvent { i, itemGuideBinding, viewHolder, guideData ->
            itemGuideBinding.root.setOnClickListener {
                finish()
                when (guideData.titleRes) {
                    R.string.create_template -> {
                        startActivity(
                            Intent(
                                this,
                                CreateTemplateActivity::class.java
                            )
                        )
                    }
                    R.string.import_template -> {
                        val startIntent =
                            Intent(this, FileManagerActivity::class.java)
                        val fileBundle = Bundle()
                        fileBundle.putString("type", "selectFile")
                        startIntent.putExtra("data", fileBundle)
                        startActivity(startIntent)
                    }
                }
            }
        }
        viewBinding.recyclerView.adapter = adapter
    }

    /**
     * 加载模组活动
     */
    fun loadMod() {
        val dataList = ArrayList<GuideData>()
        dataList.add(
            GuideData(
                R.string.create_mod_lable,
                R.string.create_mod_describe,
                R.drawable.ic_outline_create_24
            )
        )
        dataList.add(
            GuideData(
                R.string.import_mod,
                R.string.import_mod_from_file_manager_describe,
                R.drawable.folder
            )
        )
        dataList.add(
            GuideData(
                R.string.import_mod_from_package_directory,
                R.string.import_mod_from_package_directory_describe,
                R.drawable.zip
            )
        )
        dataList.add(
            GuideData(
                R.string.import_mod_from_recycle_bin,
                R.string.import_mod_from_recycle_bin_describe,
                R.drawable.auto_delete
            )
        )
        val adapter = GuideAdapter(this, dataList)
        adapter.setItemEvent { i, itemGuideBinding, viewHolder, guideData ->
            itemGuideBinding.root.setOnClickListener {
                when (guideData.titleRes) {
                    R.string.create_mod_lable -> {
                        val startActivity =
                            Intent(
                                this,
                                CreateModActivity::class.java
                            )
                        createMod.launch(startActivity)
                    }
                    R.string.import_mod -> {
                        val startIntent =
                            Intent(this, FileManagerActivity::class.java)
                        val fileBundle = Bundle()
                        fileBundle.putString("type", "selectFile")
                        startIntent.putExtra("data", fileBundle)
                        startActivity(startIntent)
                        finish()
                    }
                    R.string.import_mod_from_package_directory -> {
                        val startIntent =
                            Intent(this, FileManagerActivity::class.java)
                        val fileBundle = Bundle()
                        fileBundle.putString("type", "selectFile")
                        val packDirectory = AppSettings.getValue(
                            AppSettings.Setting.PackDirectory,
                            AppSettings.dataRootDirectory + "/bin/"
                        )
                        fileBundle.putString("path", packDirectory)
                        fileBundle.putString("rootpath", packDirectory)
                        startIntent.putExtra("data", fileBundle)
                        startActivity(startIntent)
                        finish()
                    }
                    R.string.import_mod_from_recycle_bin -> {
                        startActivity(Intent(this, RecyclingStationActivity::class.java))
                        finish()
                    }
                }
            }
        }
        viewBinding.recyclerView.adapter = adapter
    }

    override fun getViewBindingObject(layoutInflater: LayoutInflater): ActivityCreationWizardBinding {
        return ActivityCreationWizardBinding.inflate(layoutInflater)
    }

}