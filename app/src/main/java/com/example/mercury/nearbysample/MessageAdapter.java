package com.example.mercury.nearbysample;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final String TAG = MessageAdapter.class.getSimpleName();
    private ArrayList<MessageModel> messageList;

    MessageAdapter(ArrayList<MessageModel> messages) {
        messageList = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
        return new MessageViewHolder(v);
    }


    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        holder.messageWho.setText(message.getWho());
        holder.messageWho.setText(String.format("%s : ", message.getWho()));
        holder.messageText.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessageList(ArrayList<MessageModel> newMessages) {
        messageList = newMessages;
        notifyDataSetChanged();
        Log.d(TAG, "setMessageList: updating adapter's knowledge of message list. messageList size: " + String.valueOf(messageList.size()));
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.message_who)
        TextView messageWho;

        @BindView(R.id.message_text)
        TextView messageText;

        LinearLayout messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            messageContainer = (LinearLayout) itemView;
        }

    }

}
