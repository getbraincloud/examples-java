package com.bitheads.bcdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView enterBrainCloud = findViewById(R.id.enter_brainCloud_tv);

        // Click to switch activities
        enterBrainCloud.setOnClickListener(view -> enterAuthMenu());
    }

    /**
     * Go to the AuthenticateMenu Activity to initialize brainCloud and select authentication type
     */
    public void enterAuthMenu(){
        Intent intent = new Intent(this, AuthenticateMenu.class);
        startActivity(intent);

        finish();
    }
}