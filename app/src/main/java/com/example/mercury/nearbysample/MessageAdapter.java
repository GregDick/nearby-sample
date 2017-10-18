package com.example.mercury.nearbysample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

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
        holder.messageText.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.message_who)
        TextView messageWho;

        @BindView(R.id.message_text)
        TextView messageText;

        LinearLayout messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(itemView);
            messageContainer = (LinearLayout) itemView;
        }

    }

}
