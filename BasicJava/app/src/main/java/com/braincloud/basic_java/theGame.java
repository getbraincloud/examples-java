package com.braincloud.basic_java;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONException;
import org.json.JSONObject;

public class theGame extends AppCompatActivity implements IServerCallback
{
    IServerCallback theCallback;

    Button card1, card2, card3, card4, card5, card6, card7, card8, card9, card10, card11, card12;
    Button[] cardChoices;
    int chances = 25;
    Map<String, Integer> cardValues;
    IServerCallback theGameCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_the_game);

        //theGameCallback = this;

        //Login._bc.GetWrapper().getPlayerStatisticsService().readAllUserStats(theGameCallback);

        TextView chancestext = findViewById(R.id.chancesTextView);
        chancestext.setText("Chances Left: " + chances);

        //setup cards tha will flip
        card1 = (Button) findViewById(R.id.card1);
        card2 = (Button) findViewById(R.id.card2);
        card3 = (Button) findViewById(R.id.card3);
        card4 = (Button) findViewById(R.id.card4);
        card5 = (Button) findViewById(R.id.card5);
        card6 = (Button) findViewById(R.id.card6);
        card7 = (Button) findViewById(R.id.card7);
        card8 = (Button) findViewById(R.id.card8);
        card9 = (Button) findViewById(R.id.card9);
        card10 = (Button) findViewById(R.id.card10);
        card11= (Button) findViewById(R.id.card11);
        card12 = (Button) findViewById(R.id.card12);

        //set up tags for easy access later
        card1.setTag("card1");
        card2.setTag("card2");
        card3.setTag("card3");
        card4.setTag("card4");
        card5.setTag("card5");
        card6.setTag("card6");
        card7.setTag("card7");
        card8.setTag("card8");
        card9.setTag("card9");
        card10.setTag("card10");
        card11.setTag("card11");
        card12.setTag("card12");

        //set up an array to store the first and second choice
        cardChoices = new Button[2];
        cardChoices[0] = null;
        cardChoices[1] = null;

        //created a map to keep track of cards and their values.
        cardValues = new HashMap<>();

        //make another array of points and matches of the same length
        int[] points = {5, 5, 15, 15, 25, 25, 50, 50, 75, 75, 100, 100};

        //shuffle the array, then add add the array to the map.
        Shuffle(points, 2);

        //add the shuffled array to the map
        for(int i = 0; i < points.length; i++)
        {
            String temp = "card" + (i+1);
            cardValues.put(temp, points[i]);
        }

        //for the buttons the player presses
        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice((card1));
            }
        });

        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card2);
            }
        });

        card3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card3);
            }
        });

        card4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card4);
            }
        });

        card5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card5);
            }
        });

        card6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card6);
            }
        });

        card7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card7);
            }
        });

        card8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card8);
            }
        });

        card9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card9);
            }
        });

        card10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card10);
            }
        });

        card11.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card11);
            }
        });

        card12.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandleCardChoice(card12);
            }
        });
    }

    void Shuffle(int[] arr, int numShuffles)
    {
        for(int k = 0; k < numShuffles; k++)
        {
            int index;
            int temp;
            Random rand = new Random();
            for (int i = 0; i < arr.length; i++) {
                //generate the index I'm swapping
                index = rand.nextInt(arr.length);
                temp = arr[index];
                arr[index] = arr[i];
                arr[i] = temp;
            }
        }
    }

    void HandleCardChoice(Button btn)
    {
        //Are they onto their second choice?
        if(cardChoices[0] != null && btn != cardChoices[0])
        {
            //make the choice equal the chosen button then show its value.
            cardChoices[1] = btn;
            String buttonTag = btn.getTag().toString();
            String value = cardValues.get(buttonTag).toString();
            btn.setText(value);

            //now lets check to see if card 1 and 2 are the same
            if(cardValues.get(cardChoices[0].getTag().toString()) == cardValues.get(cardChoices[1].getTag().toString()))
            {
                //disable the button of choice 1
                cardChoices[0].setEnabled(false);
                cardChoices[0].setClickable(false);
                //disable the button of choice 2
                cardChoices[1].setEnabled(false);
                cardChoices[1].setClickable(false);
            }
            else
            {
                //otherwise they were not equal and I need to set the buttons back to null and text to nothing.
                cardChoices[0].setText("");
                cardChoices[1].setText("");
                cardChoices[0] = null;
                cardChoices[1] = null;
                chances--;
                TextView chancestext = findViewById(R.id.chancesTextView);
                chancestext.setText("Chances Left: " + chances);
            }
        }
        //they're making their first choice
        if(cardChoices[0] == null)
        {
            cardChoices[0] = btn;
            String buttonTag = btn.getTag().toString();
            String value = cardValues.get(buttonTag).toString();
            btn.setText(value);
        }

        if(chances <= 0)
        {
            //increment losses on braincloud.
            try {
                JSONObject obj = new JSONObject();
                obj.put("Lose", 1);
                Login._bc.GetWrapper().getPlayerStatisticsService().incrementUserStats(obj.toString(), theCallback);
            }
            catch(JSONException e) {
                e.printStackTrace();
            }
            //reset the game
            ResetGame();
        }

        //check game state
        if(
                !card1.isEnabled()
                && !card2.isEnabled()
                && !card3.isEnabled()
                && !card4.isEnabled()
                && !card5.isEnabled()
                && !card6.isEnabled()
                && !card7.isEnabled()
                && !card8.isEnabled()
                && !card9.isEnabled()
                && !card10.isEnabled()
                && !card11.isEnabled()
                && !card12.isEnabled())
        {
            //increment wins
            try {
                JSONObject obj = new JSONObject();
                obj.put("Win", 1);
                Login._bc.GetWrapper().getPlayerStatisticsService().incrementUserStats(obj.toString(), theCallback);
            }
            catch(JSONException e) {
                e.printStackTrace();
            }
            ResetGame();
        }
    }
    void ResetGame()
    {
        chances = 25;

        TextView chancestext = findViewById(R.id.chancesTextView);
        chancestext.setText("Chances Left: " + chances);

        //Reset the game
        card1.setEnabled(true);
        card1.setClickable(true);
        card1.setText("");
        card2.setEnabled(true);
        card2.setClickable(true);
        card2.setText("");
        card3.setEnabled(true);
        card3.setClickable(true);
        card3.setText("");
        card4.setEnabled(true);
        card4.setClickable(true);
        card4.setText("");
        card5.setEnabled(true);
        card5.setClickable(true);
        card5.setText("");
        card6.setEnabled(true);
        card6.setClickable(true);
        card6.setText("");
        card7.setEnabled(true);
        card7.setClickable(true);
        card7.setText("");
        card8.setEnabled(true);
        card8.setClickable(true);
        card8.setText("");
        card9.setEnabled(true);
        card9.setClickable(true);
        card9.setText("");
        card10.setEnabled(true);
        card10.setClickable(true);
        card10.setText("");
        card11.setEnabled(true);
        card11.setClickable(true);
        card11.setText("");
        card12.setEnabled(true);
        card12.setClickable(true);
        card12.setText("");

        //set up an array to store the first and second choice
        cardChoices = new Button[2];
        cardChoices[0] = null;
        cardChoices[1] = null;

        //created a map to keep track of cards and their values.
        cardValues = new HashMap<>();

        //make another array of points and matches of the same length
        int[] points = {5, 5, 15, 15, 25, 25, 50, 50, 75, 75, 100, 100};

        //shuffle the array, then add add the array to the map.
        Shuffle(points, 2);

        //add the shuffled array to the map
        for(int i = 0; i < points.length; i++)
        {
            String temp = "card" + (i+1);
            cardValues.put(temp, points[i]);
        }
    }

    //callback functions
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData)
    {
       // if(serviceOperation.equals(ServiceOperation.READ))
       // {
        //    try {
                //JSONObject entries = new JsonParser()

        //    }
            //catch(JSONException e) {
               // e.printStackTrace();
            //}
        //}
    }

    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
    {

    }
}
