package se.newton.chatapp.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import se.newton.chatapp.R;
import se.newton.chatapp.adapter.MessageAdapter;
import se.newton.chatapp.model.Message;
import se.newton.chatapp.pushnotify.Messaging;
import se.newton.chatapp.service.AdapterManager;
import se.newton.chatapp.service.Database;
import se.newton.chatapp.service.Storage;
import se.newton.chatapp.viewmodel.ChannelViewModel;

import static android.app.Activity.RESULT_OK;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private static final int REQUEST_IMAGE_OPEN_AND_SEND = 1;
    private String cid;
    private FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
    private ChannelViewModel viewModel;

    public ChatFragment() {
        // Required empty public constructor
    }

    // Use this method to create a new ChatFragment instead of calling the constructor. Here we can
    //  pass arguments if needed.
    // cid - ID of the channel that is to be displayed in the fragment.
    public static ChatFragment newInstance(RequestManager glideManager, String cid) {
        Log.d(TAG, "Creating a new fragment");
        ChatFragment fragment = new ChatFragment();
        fragment.cid = cid;
        fragment.viewModel = ChannelViewModel.getViewModel(glideManager, cid);
        Messaging.subscribeToTopic(cid);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Inflating fragment");
        getActivity().setTitle(cid);
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        // Connect the adapter to the RecyclerView
        ((RecyclerView) activity.findViewById(R.id.messageList)).setAdapter(viewModel.getAdapter());

        // Attach a listener to the Send button.
        activity.findViewById(R.id.buttonSend).setOnClickListener(view -> {
            if(fUser == null){
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        // Select login methods, save as a List used in the next method call
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent. This is done through startActivityForResult, which
                // launches an activity from an Intent and calls onActivityResult when this Intent exits.
                startActivity(
                        // Create the Intent via FirebaseUI
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build());
                return;
            }

            TextView messageText = activity.findViewById(R.id.messageText);
            if(messageText.getText().length() > 0)
                // If the text field is not empty, send the message and set the text field to blank.
                // Consider waiting for server response before wiping the text field, showing an
                //  animation in the mean time.
                Database.createMessage(Message.TYPE_TEXT, messageText.getText().toString(), cid, m -> {});
                messageText.setText("");
        });

        // Attach a listener to the Attachment button, which currently just sends the user profile
        //  image as an image message.
        // TODO: Implement a context menu where you can choose between different items to send.

        activity.findViewById(R.id.buttonAttach).setOnClickListener(view -> {
            if(fUser == null){
                // Select login methods, save as a List used in the next method call
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent. This is done through startActivityForResult, which
                // launches an activity from an Intent and calls onActivityResult when this Intent exits.
                startActivity(
                        // Create the Intent via FirebaseUI
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build());
                return;
            }

            startImagePicker();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    // -- Launch image picker to upload to Firebase and send as a message --
    private void startImagePicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_IMAGE_OPEN_AND_SEND);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_OPEN_AND_SEND && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            Storage.uploadImage(fullPhotoUri, UUID.randomUUID().toString(), task -> {
                if(task.isSuccessful())
                    Database.createMessage(Message.TYPE_IMAGE,
                            task.getResult().getDownloadUrl().toString(), cid, m -> {});
            });
        }
    }
}
