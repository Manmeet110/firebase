package com.example.firebase_puig;

import androidx.appcompat.app.AppCompatActivity;

public class YourParentActivity extends AppCompatActivity {
    private homeFragment.PostsAdapter postsAdapter;

    // Other activity lifecycle methods...

    public homeFragment.PostsAdapter getPostsAdapter() {
        return postsAdapter;
    }
}
