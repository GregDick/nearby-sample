package com.example.mercury.nearbysample

import android.content.res.Resources
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import java.util.ArrayList

import butterknife.BindView
import butterknife.ButterKnife
import com.example.mercury.nearbysample.R.id.message_text
import com.example.mercury.nearbysample.R.id.message_who
import com.example.mercury.nearbysample.R.layout.message_item
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.message_item.*
import kotlinx.android.synthetic.main.message_item.message_text



open class MessageAdapter constructor(private var messageList: ArrayList<MessageModel>,
                                      private val activityCallback: MainActivityCallback,
                                      override val containerView: View?)
    : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(), LayoutContainer {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(v)
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        //TODO: color message based on sender
        val message = messageList[position]
        if (message.who == activityCallback.username) {
            message_text.background = activityCallback.resources.getDrawable(R.drawable.self_message_background)
            holder.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
            //these calls are necessary on older OS for some reason
            message_who.text = null
        } else {
            message_who.text = String.format("%s : ", message.who)
            //these calls are necessary on older OS for some reason
            message_who.setTextColor(activityCallback.resources.getColor(R.color.colorPrimary))
            message_text.background = null
            holder.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        message_text.text = message.text
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun setMessageList(newMessages: ArrayList<MessageModel>) {
        messageList = newMessages
        notifyDataSetChanged()
        Log.d(TAG, "setMessageList: updating adapter's knowledge of message list. messageList size: " + messageList!!.size.toString())
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var messageContainer: LinearLayout

        init {
            ButterKnife.bind(this, itemView)
            messageContainer = itemView as LinearLayout
        }

    }

    interface MainActivityCallback {
        val username: String
        val resources: Resources
    }

    companion object {

        private val TAG = MessageAdapter::class.java.simpleName
    }
}
