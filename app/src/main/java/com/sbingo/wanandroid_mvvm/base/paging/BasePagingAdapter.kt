package com.sbingo.wanandroid_mvvm.base.paging

import android.net.wifi.p2p.WifiP2pDevice.FAILED
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sbingo.wanandroid_mvvm.R
import com.sbingo.wanandroid_mvvm.base.RequestState

/**
 * Author: Sbingo666
 * Date:   2019/4/12
 */
abstract class BasePagingAdapter<T>(diffCallback: DiffUtil.ItemCallback<T>, private val retryCallback: () -> Unit) :
    PagedListAdapter<T, BasePagingAdapter.ViewHolder>(diffCallback) {

    private val TYPE_ITEM = 0
    private val TYPE_FOOTER = 1
    private var requestState: RequestState<Any>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> ViewHolder(LayoutInflater.from(parent.context).inflate(getItemLayout(), parent, false))
            TYPE_FOOTER -> FooterViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("未知 view type = $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bind(holder, getItem(position)!!, position)
    }

    open class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val map = mutableMapOf<Int, View>()

        fun getView(id: Int): View? {
            var viewId = map[id]
            if (viewId == null) {
                viewId = view.findViewById(id)
                map[id] = viewId
            }
            return viewId
        }

        fun setText(id: Int, string: String?) {
            val textView = getView(id)
            if (textView is TextView) {
                textView.text = string
            }
        }
    }

    class FooterViewHolder(view: View, private val retryCallback: () -> Unit) : ViewHolder(view) {
        private val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        private val retry = view.findViewById<Button>(R.id.retry_button)
        private val errorMsg = view.findViewById<TextView>(R.id.error_msg)

        init {
            retry.setOnClickListener {
                retryCallback()
            }
        }

        fun bindTo(requestState: RequestState<Any>?) {
            setText(R.id.msg, requestState?.status"加载中...")
            progressBar.visibility = toVisibility(networkState?.status == RUNNING)
            retry.visibility = toVisibility(networkState?.status == FAILED)
            errorMsg.visibility = toVisibility(networkState?.msg != null)
            errorMsg.text = networkState?.msg
        }

        companion object {
            fun create(parent: ViewGroup, retryCallback: () -> Unit): FooterViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.footer_item, parent, false)
                return FooterViewHolder(view, retryCallback)
            }

            fun toVisibility(constraint: Boolean): Int {
                return if (constraint) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    private fun hasExtraRow() = !!requestState?.isSuccess()

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            TYPE_FOOTER
        } else {
            TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    fun setRequestState(newRequestState: RequestState<Any>) {
        val previousState = this.requestState
        val hadExtraRow = hasExtraRow()
        this.requestState = newRequestState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newRequestState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    abstract fun bind(holder: ViewHolder, item: T, position: Int)

    abstract fun getItemLayout(): Int
}