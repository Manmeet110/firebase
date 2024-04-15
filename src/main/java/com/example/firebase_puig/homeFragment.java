package com.example.firebase_puig;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;


public class homeFragment extends Fragment {
    NavController navController;
    public AppViewModel appViewModel;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.NewPostFragment);
            }
        });

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        Query query = FirebaseFirestore.getInstance().collection("posts").limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options));
    }

    private void deletePost(String postKey) {
        FirebaseFirestore.getInstance().collection("posts")
                .document(postKey)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Post deleted successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error deleting post", e);
                    }
                });
    }

    private void addComment(String postKey, String commentContent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Comment comment = new Comment(user.getUid(), user.getDisplayName(), commentContent, System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("posts")
                .document(postKey)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Comment added successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error adding comment", e);
                    }
                });
    }

    private void retrieveComments(String postKey, ImageView commentsLayout) {
        FirebaseFirestore.getInstance().collection("posts")
                .document(postKey)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Error getting comments", error);
                            return;
                        }
                        // Iterate through the comments and display them in your UI
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            // Add logic to display comments in your UI
                        }
                    }
                });
    }


    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {
        NavController navController;
        AppViewModel appViewModel;
        Context context;

        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {
            super(options);
            this.navController = navController;
            this.appViewModel = appViewModel;
            this.context = context;

        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @SuppressLint("SuspiciousIndentation")
        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {

            Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);

            // Gestion de likes
            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (post.likes.containsKey(uid))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes." + uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });
            // clicklistener para eliminar post
            holder.deletePostImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deletePost(postKey);
                }
            });
            // Handle adding comments
            holder.commentImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addComment(postKey, "Sample Comment");
                }
            });

            // Retrieve and display comments
            retrieveComments(postKey, holder.commentsLayout);


            // Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }
        }


        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, mediaImageView, deletePostImageView, commentImageView, commentsLayout;
            TextView authorTextView, contentTextView, numLikesTextView;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                mediaImageView = itemView.findViewById(R.id.mediaImageView);
                deletePostImageView = itemView.findViewById(R.id.deletePostImageView);
                commentImageView = itemView.findViewById(R.id.commentImageView); // Initialize commentImageView
                commentsLayout = itemView.findViewById(R.id.commentImageView);

                // Set click listener for the comment button
                commentImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle click event for adding comments
                        // For example, you can show a dialog or navigate to a new activity/fragment for adding comments
                        Toast.makeText(itemView.getContext(), "Comment button clicked!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // Method to show the comment dialog
            public void showCommentDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                builder.setTitle("Add Comment");

                // Set up the input
                final EditText input = new EditText(itemView.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String commentText = input.getText().toString();
                        // Handle the comment text (e.g., save it or send it to a server)
                        // For now, just display a toast message
                        Toast.makeText(itemView.getContext(), "Comment added: " + commentText, Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // Show the dialog
                builder.show();
            }
        }
    }
}
