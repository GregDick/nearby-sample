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

open class MessageAdapter constructor(private var messageList: ArrayList<MessageModel>?, private val activityCallback: MainActivityCallback) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(v)
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        //TODO: color message based on sender
        val message = messageList!![position]
        if (message.who == activityCallback.username) {
            holder.messageText!!.background = activityCallback.resources.getDrawable(R.drawable.self_message_background)
            holder.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_RTL
            //these calls are necessary on older OS for some reason
            holder.messageWho!!.text = null
        } else {
            holder.messageWho!!.text = String.format("%s : ", message.who)
            //these calls are necessary on older OS for some reason
            holder.messageWho!!.setTextColor(activityCallback.resources.getColor(R.color.colorPrimary))
            holder.messageText!!.background = null
            holder.messageContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        holder.messageText!!.text = message.text
    }

    override fun getItemCount(): Int {
        return messageList!!.size
    }

    fun setMessageList(newMessages: ArrayList<MessageModel>) {
        messageList = newMessages
        notifyDataSetChanged()
        Log.d(TAG, "setMessageList: updating adapter's knowledge of message list. messageList size: " + messageList!!.size.toString())
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.message_who)
        var messageWho: TextView? = null

        @BindView(R.id.message_text)
        var messageText: TextView? = null

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
