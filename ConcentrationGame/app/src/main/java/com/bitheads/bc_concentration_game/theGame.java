package com.bitheads.bc_concentration_game;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bitheads.braincloud.client.IServerCallback;
import com.bitheads.braincloud.client.ServiceName;
import com.bitheads.braincloud.client.ServiceOperation;

import org.json.JSONException;
import org.json.JSONObject;

public class theGame extends AppCompatActivity implements IServerCallback
{
    BrainCloudManager brainCloudManager;

    static final int MAX_TURNS = 20;
    static final int FLIP_HALF_MS = 150;
    static final int UNFLIP_DELAY_MS = 1250;

    static final int[] CARD_IDS = {
            R.id.card1, R.id.card2, R.id.card3, R.id.card4, R.id.card5, R.id.card6,
            R.id.card7, R.id.card8, R.id.card9, R.id.card10, R.id.card11, R.id.card12
    };

    //brainCloud feature icons from getbraincloud.com, one per pair
    static final int[] TILE_ICONS = {
            R.drawable.tile_authentication, R.drawable.tile_leaderboards, R.drawable.tile_multiplayer,
            R.drawable.tile_cloud_code, R.drawable.tile_gamification, R.drawable.tile_chat
    };
    static final String[] TILE_LABELS = {
            "Authentication", "Leaderboards", "Multiplayer", "Cloud Code", "Gamification", "Chat"
    };

    Button[] cards;
    View[] pips;
    //index into TILE_ICONS for each card
    int[] cardValues;
    boolean[] matched;
    int matchedCount;
    int turn;

    //index of the first face-up card of the current turn, or -1
    int firstChoice = -1;
    //true while a turn is resolving, so taps are ignored
    boolean busy = false;
    boolean gameOver = false;

    final Handler handler = new Handler(Looper.getMainLooper());

    TextView statusText;
    TextView playAgainText;
    int directionsColor;

    int winsForAchievement = 0;

    //the callback that this class will work with
    IServerCallback theGameCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_the_game);

        brainCloudManager = BrainCloudManager.getInstance(theGame.this);

        //set the callback
        theGameCallback = this;

        statusText = findViewById(R.id.statusTextView);
        playAgainText = findViewById(R.id.playAgainTextView);
        directionsColor = statusText.getCurrentTextColor();

        TextView versionText = findViewById(R.id.versionTextView);
        versionText.setText("v" + GetAppVersion() + " · brainCloud " + brainCloudManager.getBrainCloudWrapper().getClient().getBrainCloudVersion());

        if(getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
        }

        CheckAchievements();

        //read the player's name for the banner, and their stats for the record line (the callback success will update the ui)
        brainCloudManager.getBrainCloudWrapper().getPlayerStateService().readUserState(theGameCallback);
        brainCloudManager.getBrainCloudWrapper().getPlayerStatisticsService().readAllUserStats(theGameCallback);

        cards = new Button[CARD_IDS.length];
        for(int i = 0; i < CARD_IDS.length; i++)
        {
            final int index = i;
            cards[i] = findViewById(CARD_IDS[i]);
            cards[i].setOnClickListener(v -> HandleCardChoice(index));
        }

        int pipSize = Dp(12);
        LinearLayout pipRow = findViewById(R.id.turnPips);
        pips = new View[MAX_TURNS];
        for(int i = 0; i < MAX_TURNS; i++)
        {
            pips[i] = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(pipSize, pipSize);
            lp.setMargins(Dp(2), 0, Dp(2), 0);
            pipRow.addView(pips[i], lp);
        }

        //once the game ends, tapping anywhere starts a new one
        findViewById(R.id.gameRoot).setOnClickListener(v -> {
            if(gameOver)
            {
                NewGame();
            }
        });

        //tiles need their laid-out size to scale the icons
        cards[0].post(this::NewGame);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_logout)
        {
            Logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void Logout()
    {
        brainCloudManager.getBrainCloudWrapper().logout(true, new IServerCallback() {

            @Override
            public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData) {
                ReturnToLogin();
            }

            @Override
            public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError) {
                if(brainCloudManager.getBrainCloudWrapper().getClient().isAuthenticated()){
                    System.out.println("Logout failed: " + jsonError);
                    Log.e("brainCloud Request Error", "Logout failed: " + jsonError);
                }
                else{
                    ReturnToLogin();
                }
            }
        });
    }

    void ReturnToLogin()
    {
        startActivity(new Intent(getApplication(), Login.class));
        finish();
    }

    //Fisher-Yates shuffle
    void Shuffle(int[] arr)
    {
        Random rand = new Random();
        for(int i = arr.length - 1; i > 0; i--)
        {
            int index = rand.nextInt(i + 1);
            int temp = arr[index];
            arr[index] = arr[i];
            arr[i] = temp;
        }
    }

    void HandleCardChoice(int index)
    {
        if(gameOver)
        {
            NewGame();
            return;
        }
        if(busy || matched[index] || index == firstChoice)
        {
            return;
        }

        //first card of the turn stays blue until the second is revealed
        if(firstChoice < 0)
        {
            firstChoice = index;
            Flip(index, () -> ShowFace(index, R.color.flipBlue), () -> {});
            return;
        }

        int first = firstChoice;
        firstChoice = -1;
        busy = true;

        boolean isMatch = cardValues[first] == cardValues[index];
        int resultColor = isMatch ? R.color.matchGreen : R.color.missRed;
        int pip = turn++;

        Flip(index, () -> {
            ShowFace(first, resultColor);
            ShowFace(index, resultColor);
            FillPip(pip, resultColor);
        }, () -> {
            if(isMatch)
            {
                matched[first] = true;
                matched[index] = true;
                matchedCount += 2;
                EndTurn();
                return;
            }

            //leave both cards visible briefly so the player can remember them
            handler.postDelayed(() -> {
                Flip(first, () -> ShowBack(first), () -> {});
                Flip(index, () -> ShowBack(index), this::EndTurn);
            }, UNFLIP_DELAY_MS);
        });
    }

    void EndTurn()
    {
        if(matchedCount == cards.length)
        {
            EndGame(true);
        }
        else if(turn >= MAX_TURNS)
        {
            EndGame(false);
        }
        else
        {
            busy = false;
            MarkCurrentPip();
        }
    }

    void EndGame(boolean won)
    {
        gameOver = true;
        busy = false;

        statusText.setText(won ? "You win!" : "Out of turns. You lose!");
        statusText.setTextColor(ContextCompat.getColor(this, won ? R.color.matchGreen : R.color.missRed));
        playAgainText.setVisibility(View.VISIBLE);

        try {
            JSONObject obj = new JSONObject();
            obj.put(won ? "Win" : "Lose", 1);
            brainCloudManager.getBrainCloudWrapper().getPlayerStatisticsService().incrementUserStats(obj.toString(), theGameCallback);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }

        //read the new stats off braincloud and display them in the text.
        brainCloudManager.getBrainCloudWrapper().getPlayerStatisticsService().readAllUserStats(theGameCallback);
    }

    void NewGame()
    {
        CheckAchievements();

        handler.removeCallbacksAndMessages(null);
        firstChoice = -1;
        busy = false;
        gameOver = false;
        turn = 0;

        statusText.setText("Find the matching tiles");
        statusText.setTextColor(directionsColor);
        playAgainText.setVisibility(View.INVISIBLE);

        cardValues = new int[cards.length];
        for(int i = 0; i < cards.length; i++)
        {
            cardValues[i] = i / 2;
        }
        Shuffle(cardValues);

        matched = new boolean[cards.length];
        matchedCount = 0;
        for(int i = 0; i < cards.length; i++)
        {
            cards[i].animate().cancel();
            cards[i].setScaleX(1f);
            ShowBack(i);
        }

        for(int i = 0; i < MAX_TURNS; i++)
        {
            StylePip(i, R.color.pipEmpty, false);
        }
        MarkCurrentPip();
    }

    //squash the tile to its edge with a blue border, swap faces, then expand
    void Flip(int index, Runnable atMidpoint, Runnable onDone)
    {
        Button card = cards[index];
        SetBorder(index, GetFill(index), R.color.flipBlue, 5);
        card.animate().scaleX(0f).setDuration(FLIP_HALF_MS).withEndAction(() -> {
            atMidpoint.run();
            card.animate().scaleX(1f).setDuration(FLIP_HALF_MS).withEndAction(onDone).start();
        }).start();
    }

    void ShowBack(int index)
    {
        Button card = cards[index];
        card.setTag(R.color.cardBackFill);
        SetBorder(index, R.color.cardBackFill, R.color.cardOutline, 2);
        card.setCompoundDrawables(null, null, null, null);
        card.setPadding(Dp(4), Dp(4), Dp(4), Dp(4));
        card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        card.setTextColor(ContextCompat.getColor(this, R.color.cardBack));
        card.setText("?");
    }

    void ShowFace(int index, int borderColor)
    {
        Button card = cards[index];
        card.setTag(R.color.cardFace);
        SetBorder(index, R.color.cardFace, borderColor, 5);

        int labelHeight = Dp(16);
        int iconSize = Math.max(Dp(28), Math.min(card.getHeight() - labelHeight - Dp(12), card.getWidth() - Dp(12)));
        Drawable icon = ContextCompat.getDrawable(this, TILE_ICONS[cardValues[index]]).mutate();
        icon.setBounds(0, 0, iconSize, iconSize);
        card.setCompoundDrawables(null, icon, null, null);
        card.setCompoundDrawablePadding(0);
        //centre icon + label vertically; drawableTop otherwise pins to the top edge
        int top = Math.max(Dp(4), (card.getHeight() - iconSize - labelHeight) / 2);
        card.setPadding(Dp(4), top, Dp(4), Dp(4));

        card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        card.setTextColor(ContextCompat.getColor(this, R.color.cardLabel));
        card.setText(TILE_LABELS[cardValues[index]]);
    }

    int GetFill(int index)
    {
        Object tag = cards[index].getTag();
        return tag instanceof Integer ? (Integer) tag : R.color.cardBackFill;
    }

    void SetBorder(int index, int fillColor, int strokeColor, int strokeDp)
    {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(Dp(10));
        bg.setColor(ContextCompat.getColor(this, fillColor));
        bg.setStroke(Dp(strokeDp), ContextCompat.getColor(this, strokeColor));
        cards[index].setBackground(bg);
    }

    void MarkCurrentPip()
    {
        if(turn < MAX_TURNS)
        {
            StylePip(turn, R.color.flipBlue, false);
        }
    }

    void FillPip(int index, int color)
    {
        StylePip(index, color, true);
    }

    void StylePip(int index, int color, boolean filled)
    {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        int c = ContextCompat.getColor(this, color);
        dot.setColor(filled ? c : 0);
        dot.setStroke(Dp(2), c);
        pips[index].setBackground(dot);
    }

    int Dp(int dp)
    {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    String GetAppVersion()
    {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch(PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    //callback functions
    public void serverCallback(ServiceName serviceName, ServiceOperation serviceOperation, JSONObject jsonData)
    {
        if(serviceOperation != ServiceOperation.READ)
        {
            return;
        }

        try {
            JSONObject data = jsonData.getJSONObject("data");

            //player identity for the banner
            if(serviceName == ServiceName.playerState)
            {
                String name = data.optString("playerName");
                if(name.isEmpty()) name = data.optString("emailAddress");
                if(name.isEmpty()) name = data.optString("profileId");
                if(getSupportActionBar() != null)
                {
                    getSupportActionBar().setTitle(name);
                }
            }
            else if(serviceName == ServiceName.playerStatistics)
            {
                //update the ui based on the stats
                JSONObject stats = data.getJSONObject("statistics");
                String numWins = stats.getString("Win");
                String numLose = stats.getString("Lose");
                winsForAchievement = Integer.parseInt(numWins);

                TextView recordText = findViewById(R.id.recordTextView);
                recordText.setText("Wins: " + numWins + "    Losses: " + numLose);
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void serverError(ServiceName serviceName, ServiceOperation serviceOperation, int statusCode, int reasonCode, String jsonError)
    {
        //in the case the callback was bad.
    }

    //just a simple example of awarding an achievement on BrainCloud.
    public void CheckAchievements()
    {
        if(winsForAchievement >= 5)
        {
            //very bare bones, but can pass in much more to affect specific values of the achievement when you award it.
            String[] achievementArr = new String[1];
            achievementArr[0] = "Win5Games";
            brainCloudManager.getBrainCloudWrapper().getGamificationService().awardAchievements(achievementArr, theGameCallback);
        }
    }
}
