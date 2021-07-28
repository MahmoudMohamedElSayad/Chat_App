package com.example.moo.chatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {
    AdView adView;

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    public static final int SIGN_IN = 1;
    private static final int PHOTO_PICKER = 2;


    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoButton;
    private EditText mMessageEditText;
    private Button mSendButton;


    private String mUsername;
    private FirebaseDatabase mFirebaseDatabase;
    // object to refernece a specific part of database object
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private ImageView iv;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, "ca-app-pub-3940256099942544/6300978111");

        adView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//
//
//        adView.loadAd(adRequest);


        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
//////////////

        mUsername = ANONYMOUS;
        iv = (ImageView) findViewById(R.id.iv_photo);
        new Download().execute("http://findicons.com/files/icons/2101/ciceronian/59/photos.png");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");


        mProgressBar = (ProgressBar) findViewById(R.id.pb_loading);
        mMessageListView = (ListView) findViewById(R.id.lv_message);
        mPhotoButton = (ImageButton) findViewById(R.id.ib_photo);
        mMessageEditText = (EditText) findViewById(R.id.et_message);
        mSendButton = (Button) findViewById(R.id.b_send_button);

        final List<Message> messageList = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.message_layout, messageList);
        mMessageListView.setAdapter(mMessageAdapter);

        mProgressBar.setVisibility(ProgressBar.INVISIBLE);


        // show an image picker to upload a image for message
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                setResult(Activity.RESULT_OK,intent);

                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO_PICKER);
            }
        });


        ////////

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message(mMessageEditText.getText().toString(), mUsername, null);

                mMessagesDatabaseReference.push().setValue(message);
                mMessageEditText.setText("");
            }
        });


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    // signed in
                    onSignedIn(firebaseUser.getDisplayName());
                } else {
                    // signed out
                    onSignedOut();

                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                    );


                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(providers)
                                    .build(), SIGN_IN);
                }
            }
        };


    }


    public class Download extends AsyncTask<String, String, Bitmap> {

        //private RemoteViews views;
        String url = "http://findicons.com/files/icons/2101/ciceronian/59/photos.png";


        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                InputStream inputStream = new java.net.URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Log.d(TAG, "doInBackground: bitmap");
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            iv.setVisibility(VISIBLE);
            Log.d(TAG, "onPostExecute: visible");
            Glide.with(iv.getContext())
                    .load(url)
                    .into(iv);
            iv.setVisibility(VISIBLE);

        }

        @Override
        protected void onCancelled(Bitmap bitmap) {
            super.onCancelled(bitmap);
            Log.d(TAG, "onCancelled: errorr");
        }


    }


    private void onSignedIn(String username) {
        mUsername = username;
        attachDatabaseListener();
    }

    private void onSignedOut() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseListener();
    }


    private void attachDatabaseListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Message message = dataSnapshot.getValue(Message.class);
                    mMessageAdapter.add(message);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "database error ", Toast.LENGTH_SHORT).show();

                }
            };

            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }


    @Override
    //@NonNull
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);




        Log.d(TAG, "onActivityResult: " + "DDDDDDDDDDDDDDDDDDDDDDDDDD");


            Log.d(TAG, "onActivityResult: signed");
            // handling photos result
            if (requestCode==2) {
                Uri selectedImage = data.getData();
                Log.d("testo", "onActivityResult: d" + data.getData());
                StorageReference photoReference =
                        mPhotosStorageReference.child(selectedImage.getLastPathSegment());

                // upload file to firebase
                photoReference.putFile(selectedImage).addOnSuccessListener(
                        this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                Log.d("asssa", "onSuccess: " + downloadUrl);
                                Message message = new Message(null, mUsername, downloadUrl.toString());
                                mMessagesDatabaseReference.push().setValue(message);
                                Log.d("testo", "onActivityResult: d" + "hhhhhhhh");


                            }
                        }
                ).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.d("TAG", "onFailure: " + e.getMessage());
                    }
                });

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "onActivityResult: canceled");
            }
        }



    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }


        super.onPause();

        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseListener();
        mMessageAdapter.clear();
    }

    /**
     * Called when returning to the activity
     */
    @Override
    public void onResume() {

        super.onResume();

        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        if (adView != null) {
            adView.resume();
        }
    }

    /**
     * Called before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.contact_activity:
                Intent intent = new Intent(MainActivity.this, ContactActivity.class);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
                startActivity(intent);

                break;

            case R.id.help:
                Intent intentss = new Intent(MainActivity.this, help.class);
               overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_right);
                startActivity(intentss);
                break;
            default:
                return true;

               // return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);

    }

    public void click(View v) {

        // Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);
        String g = "http://www.google.com";

        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(g));
        Intent choose=Intent.createChooser(intent,"choose");
        startActivity(choose);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("vv", "Couldn't call " + g.toString()
                    + ", no receiving apps installed!");
        }

    }
}