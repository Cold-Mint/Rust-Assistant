package com.coldmint.rust.pro.base

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.coldmint.rust.pro.R
import com.coldmint.rust.pro.interfaces.ItemChangeEvent
import com.coldmint.rust.pro.interfaces.ItemEvent
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.coldmint.dialog.CoreDialog
import com.github.promeg.pinyinhelper.Pinyin


abstract class BaseAdapter<ViewBindingType : ViewBinding, DataType>(
    protected val context: Context,
    protected var dataList: MutableList<DataType>
) :
    RecyclerView.Adapter<BaseAdapter.ViewHolder<ViewBindingType>>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var itemEvent: ItemEvent<ViewBindingType, ViewHolder<ViewBindingType>, DataType>? = null
    private var itemChangeEvent: ItemChangeEvent<DataType>? = null
    private val handler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val spannableStringBuilder: SpannableStringBuilder = SpannableStringBuilder()
    private val colorSpan: ForegroundColorSpan = ForegroundColorSpan(Color.parseColor("#e91e63"))
    private val bold = StyleSpan(Typeface.BOLD)


    /**
     * 获取列表项目的首字母
     */
    fun getInitial(string: String): Char {
        return if (string.isBlank()) {
            '#'
        } else {
            val char = string[0]
            if (Pinyin.isChinese(char)) {
                val pinyin = Pinyin.toPinyin(char)
                pinyin[0].uppercaseChar()
            } else {
                char.uppercaseChar()
            }
        }
    }

    /**
     * 建立搜索标题，注意当[BaseAdapter.keyWord]为空时永远返回null
     * @param title String 标题
     * @return 彩色标题
     */
    fun createSpannableString(title: String, keyWord: String): SpannableStringBuilder? {
        val startIndex = title.indexOf(keyWord)
        if (startIndex > -1) {
            spannableStringBuilder.clear()
            spannableStringBuilder.append(title)
            spannableStringBuilder.setSpan(
                colorSpan,
                startIndex,
                startIndex + keyWord.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            spannableStringBuilder.setSpan(
                bold,
                startIndex,
                startIndex + keyWord.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            return spannableStringBuilder
        } else {
            return spannableStringBuilder.append(title)
        }
    }

    /**
     * 项目改变类型
     */
    enum class ChangeType {
        Add, Remove, Replace, Disable, Enable
    }

    /**
     * 设置数据列表(会刷新列表代价很大)
     * @param dataList MutableList<DataType>
     */
    open fun setNewDataList(dataList: MutableList<DataType>) {
        this.dataList = dataList
        handler.post {
            notifyDataSetChanged()
        }
    }

    /**
     * 获取列表项目数据
     * @return MutableList<DataType>
     */
    fun getItemData(index: Int): DataType {
        return dataList[index]
    }

    /**
     * 设置项目事件加载器
     * @param itemEvent ItemEvent<ViewBindingType, DataType>
     */
    fun setItemEvent(itemEvent: ((Int, ViewBindingType, ViewHolder<ViewBindingType>, DataType) -> Unit)?) {
        if (itemEvent == null) {
            this.itemEvent = null
        } else {
            this.itemEvent =
                object : ItemEvent<ViewBindingType, ViewHolder<ViewBindingType>, DataType> {
                    override fun loadEvent(
                        index: Int,
                        viewBinding: ViewBindingType,
                        viewHolder: ViewHolder<ViewBindingType>,
                        data: DataType
                    ) {
                        itemEvent.invoke(index, viewBinding, viewHolder, data)
                    }

                }
        }
    }

    /**
     * 设置项目改变事件(当调用[BaseAdapter.addItem]，[BaseAdapter.removeItem]，[BaseAdapter.replaceItem]这3个方法任意一个，触发监听事件)注意调用[BaseAdapter.replaceItem]方法时，data为旧的项目数据
     * @param changeEvent Function3<Int, DataType, Int, Unit>
     */
    fun setItemChangeEvent(changeEvent: ((ChangeType, Int, DataType, Int) -> Unit)?) {
        if (changeEvent == null) {
            this.itemChangeEvent = null
        } else {
            this.itemChangeEvent = object : ItemChangeEvent<DataType> {
                override fun onChanged(type: ChangeType, index: Int, data: DataType, size: Int) {
                    changeEvent.invoke(type, index, data, size)
                }

            }
        }
    }

    /**
     * 移除菜单项目
     * @param index Int
     */
    fun removeItem(index: Int) {
        if (index > -1 && index < dataList.size) {
            val data = dataList[index]
            dataList.removeAt(index)
            handler.post {
                notifyItemRemoved(index)
                itemChangeEvent?.onChanged(ChangeType.Remove, index, data, dataList.size)
            }
        }
    }


    /**
     * 显示删除项目对话框
     * @param name String 名称
     * @param index Int 位置
     * @param onClickPositiveButton Function0<Boolean>? 当点击删除按钮时，返回true则删除
     * @param cancelable Boolean 是否可取消
     * @param checkBoxPrompt 选择框显示的文本
     */
    fun showDeleteItemDialog(
        name: String,
        index: Int,
        onClickPositiveButton: ((Int, Boolean) -> Boolean)? = null,
        cancelable: Boolean = false,
        checkBoxPrompt: String? = null
    ) {
        val coreDialog = CoreDialog(context).setTitle(R.string.delete_title).setMessage(
            String.format(
                context.getString(R.string.delete_prompt),
                name
            )
        ).setNegativeButton(R.string.dialog_cancel) {}
        coreDialog.setCancelable(cancelable)
        coreDialog.setCheckboxBox(checkBoxPrompt)
        coreDialog.setPositiveButton(R.string.dialog_ok) {
            if (onClickPositiveButton == null) {
                removeItem(index)
            } else {
                if (onClickPositiveButton.invoke(index, coreDialog.isChecked())) {
                    removeItem(index)
                }
            }
        }.show()
    }

    /**
     * 添加项目
     * @param data DataType
     * @param index Int
     */
    fun addItem(data: DataType, index: Int = dataList.size) {
        dataList.add(index, data)
        handler.post {
            notifyItemChanged(index)
            itemChangeEvent?.onChanged(ChangeType.Add, index, data, dataList.size)
        }
    }


    /**
     * 替换项目
     * @param data DataType
     * @param index Int
     */
    fun replaceItem(data: DataType, index: Int) {
        val oldData = dataList[index]
        dataList.removeAt(index)
        dataList.add(index, data)
        handler.post {
            notifyItemChanged(index)
            itemChangeEvent?.onChanged(ChangeType.Replace, index, oldData, dataList.size)
        }
    }

    abstract fun getViewBindingObject(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ViewBindingType

    abstract fun onBingView(
        data: DataType,
        viewBinding: ViewBindingType,
        viewHolder: ViewHolder<ViewBindingType>,
        position: Int
    )

    class ViewHolder<T : ViewBinding>(val viewBinding: T) :
        RecyclerView.ViewHolder(viewBinding.root) {
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder<ViewBindingType> {
        return ViewHolder(getViewBindingObject(layoutInflater, parent, viewType))
    }

    override fun onBindViewHolder(holder: ViewHolder<ViewBindingType>, position: Int) {
        val data: DataType = dataList[position]
        val viewBinding: ViewBindingType = holder.viewBinding
        itemEvent?.loadEvent(position, viewBinding, holder, data)
        onBingView(data, viewBinding, holder, position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}