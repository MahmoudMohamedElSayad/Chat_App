package com.example.moo.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class help extends AppCompatActivity {


   private Toolbar htoolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        htoolbar = (Toolbar) findViewById(R.id.tool_help);

            htoolbar.setTitle(R.string.Help);
            setSupportActionBar(htoolbar);





        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        htoolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        htoolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                overridePendingTransition(R.anim.animation_enter, R.anim.animation_leave);
                startActivity(intent);
            }
        });


        }






    public void click (View v) {

        // Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);
        String g = "http://www.google.com";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(g));
        Intent choose = Intent.createChooser(intent, "choose");
        startActivity(choose);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(choose);
        } else {
            Log.d("vv", "Couldn't call " + g.toString()
                    + ", no receiving apps installed!");
        }


    }}
