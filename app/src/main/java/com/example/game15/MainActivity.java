package com.example.game15;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Boolean inGame;
    Integer[][] matrix = new Integer[4][4];
    Integer[][] target = new Integer[4][4];
    int stepNumber;

    Toolbar toolbar;

    AppDatabase db;
    PlayerDao playerDao;
    Cursor cursor;

    MaterialCardView materialCardView;
    FrameLayout winnerFrame;

    TextView statusText;
    TextView stepNumberText;
    Integer[] gridImages;

    GridView gridView;
    GridAdapter gridAdapter;

    View.OnKeyListener gloryListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                v.clearFocus();
                playerDao.insert(new Player(((EditText) v).getText().toString(), stepNumber, System.currentTimeMillis()/1000));
                fillWinnerFrame(R.layout.glory_list_layout);
                fillWinnersList();
                return true;
            }
            return false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_game:
                setBeginState();
                showMatrix();
                return true;
            case R.id.action_clear_winners:
                playerDao.clear();
                fillWinnersList();
                return true;
            case R.id.action_logout:
                finishAndRemoveTask();
/*
                cheat mode on :)
                fillMatrix(matrix);
                showMatrix();
*/
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    GridView.OnItemClickListener gridItemClick = (parent, view, position, id) -> {
        if (inGame) {
        int btX;
        int btY;
        btY = position / 4;
        btX = position % 4;
        playersStep(btY, btX);
        showMatrix();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void fillMatrix(Integer[][] matrix) {
        int n = 1 ;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                matrix[y][x] = n++;
            }
        }
        matrix[3][3] = 0;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("savedMatrix", matrix);
        savedInstanceState.putInt("savedSteps", stepNumber);
        savedInstanceState.putBoolean("savedInGameStatus", inGame);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void getDataFromDB(){
        db =  Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database").allowMainThreadQueries().build();
        playerDao = db.playerDao();
    }

    public void initializeWinnerFrame(){
        winnerFrame = findViewById(R.id.winner_frame);
        materialCardView = findViewById(R.id.material_card);
    }

    public void fillWinnerFrame(@LayoutRes int layout){
        winnerFrame.removeAllViews();
        winnerFrame = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(layout, materialCardView, true);
        if (layout == R.layout.glory_layout) {
            findViewById(R.id.winner_name_edit).setOnKeyListener(gloryListener);
        }
    }

    public void fillWinnersList() {
        cursor = playerDao.getAll();
        SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                R.layout.glory_list_item,
                cursor,
                new String[]{"name", "numberOfSteps", "gameDate"},
                new int[]{R.id.winner_name, R.id.winner_steps, R.id.winner_date},
                0);
        ListView gloryListView = findViewById(R.id.glory_list_name);
        gloryListView.setAdapter(listAdapter);
    }

    public int getAllBarsHeight() { //Тут надо поразбираться
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }

        resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result += getResources().getDimensionPixelSize(resourceId);
        }

        final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        result += mActionBarSize;
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getDataFromDB();

        initializeWinnerFrame();
        fillWinnerFrame(R.layout.glory_list_layout);

        fillWinnersList();
        fillMatrix(target);

        gridImages = new Integer[16];
        for (int i = 0; i < 16; i++){
            String resName = "tree" + (i + 1);
            try {
                gridImages[i] = R.drawable.class.getField(resName).getInt(getResources());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }


        stepNumberText = findViewById(R.id.step_numbers);

        int screenSize = 0;

        switch (Resources.getSystem().getConfiguration().orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                screenSize = Resources.getSystem().getDisplayMetrics().widthPixels-10;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                screenSize = Resources.getSystem().getDisplayMetrics().heightPixels - getAllBarsHeight();
                break;
        }

        gridView = findViewById(R.id.grid);
        gridAdapter = new GridAdapter(this, screenSize/4);
        gridAdapter.setGridItems(gridToLine(matrix));
        gridAdapter.setGridImages(gridImages);
        gridView.setAdapter(gridAdapter);
        gridView.setOnItemClickListener(gridItemClick);

        statusText = findViewById(R.id.tv1);

        if (savedInstanceState != null) {
              matrix = (Integer[][]) savedInstanceState.getSerializable("savedMatrix");
              stepNumber = savedInstanceState.getInt("savedSteps");
              inGame = savedInstanceState.getBoolean("savedInGameStatus");
        } else setBeginState();
        showMatrix();

    }

    public void showMatrix(){
        gridAdapter.setGridItems(gridToLine(matrix));
        gridAdapter.notifyDataSetChanged();
        stepNumberText.setText(new StringBuilder().append(getString(R.string.step_number)).append(" ").append(stepNumber).toString());

        if (Arrays.deepEquals(matrix, target) && inGame) {
            statusText.setText(R.string.congrats);
            inGame = false;
            fillWinnerFrame(R.layout.glory_layout);
        } else statusText.setText(R.string.in_game);
    }


    public void setBeginState() {
        fillMatrix(matrix);
        for (int i = 0; i < 501; i++) {
            randomStep();
        }
        stepNumber = 0;
        inGame = true;
        statusText.setText(R.string.in_game);
    }

    public List<Integer[]> possibleCoordinates(int y, int x) {
        List<Integer[]> possibleCoordinates = new ArrayList<>();
        possibleCoordinates.add(new Integer[]{y, x + 1});
        possibleCoordinates.add(new Integer[]{y, x - 1});
        possibleCoordinates.add(new Integer[]{y + 1, x});
        possibleCoordinates.add(new Integer[]{y - 1, x});
        List<Integer[]> result = new ArrayList<>();
        for (Integer[] coordinates : possibleCoordinates) {
            if (!(coordinates[0] < 0 | coordinates[0] > 3) & !(coordinates[1] < 0 | coordinates[1] > 3)) {
                result.add(coordinates);
            }
        }
        return result;
    }

    public void playersStep(int y, int x) {
        Integer temp;
        List<Integer[]> possibleCoordinates = possibleCoordinates(y, x);
        for (Integer[] coordinates : possibleCoordinates) {
            if (matrix[coordinates[0]][coordinates[1]] == 0) {
                temp = matrix[y][x];
                matrix[y][x] = matrix[coordinates[0]][coordinates[1]];
                matrix[coordinates[0]][coordinates[1]] = temp;
                stepNumber++;
                break;
            }
        }
    }

    public Integer[] gridToLine(Integer[][] matrix) {
        Integer[] result = new Integer[16];
        int counter;
        for (int i = 0; i<4; i++) {
            for (int n = 0; n<4; n++) {
                counter = i+n*4;
                result[counter] = matrix[n][i];
            }
        }
        return result;
    }

    public void randomStep() {
        List<Integer> temp = new ArrayList<>(Arrays.asList(gridToLine(matrix)));
        int randomNum;
        int pos = temp.indexOf(0);
        int y = pos / 4;
        int x = pos % 4;
        List<Integer[]> possibleCoordinates = possibleCoordinates(y, x);
        randomNum = ((int) (Math.random() * possibleCoordinates.size()));
        int targetY = possibleCoordinates.get(randomNum)[0];
        int targetX = possibleCoordinates.get(randomNum)[1];
        matrix[y][x] = matrix[targetY][targetX];
        matrix[targetY][targetX] = 0;
    }
}
