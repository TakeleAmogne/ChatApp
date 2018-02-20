package se.newton.chatapp.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.newton.chatapp.databinding.TextMessageBinding;
import se.newton.chatapp.model.Message;
import se.newton.chatapp.viewmodel.MessageViewModel;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private static final String TAG = "MessageAdapter";
    private List<Message> messages = new ArrayList<>();

    public MessageAdapter() {
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case Message.TYPE_TEXT:
                v = TextMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                        parent, false).getRoot();
                break;
            default:
                v = null;
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.binding.setViewModel(new MessageViewModel(message));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getMessageType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextMessageBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
