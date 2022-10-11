package com.example.game15;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;

import android.content.DialogInterface;
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

    Boolean inGame;   // Флаг начатой игры
    Integer[][] matrix = new Integer[4][4]; // Массив текущего положения костяшек
    Integer[][] target = new Integer[4][4]; // Массив целевого расположения костяшек
    int stepNumber; // Количество ходов текущей игры

    // Обработчик положительного результата диалога перезапуска игры
    DialogInterface.OnClickListener gameRenewListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            setBeginState();
            showMatrix();
            dialog.cancel();
        }
    };

    // Обработчик положительного результата диалога очистки доски почета
    DialogInterface.OnClickListener clearGloryListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            playerDao.clear();
            fillWinnersList();
            dialog.cancel();
        }
    };

    // Обработчик положительного результата диалога выхода из игры
    DialogInterface.OnClickListener quitGameListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            finishAndRemoveTask();
            dialog.cancel();
        }
    };

    // Создаем диалоговые окна для подтверждения действий кнопок тулбара
    MyDialogFragment gameRenewDialogFragment = new MyDialogFragment("Начать игру заново?", gameRenewListener);
    MyDialogFragment clearGloryDialogFragment = new MyDialogFragment("Очистить доску почета?", clearGloryListener);
    MyDialogFragment quitGameDialogFragment = new MyDialogFragment("Покинуть игру?", quitGameListener);
    FragmentManager manager = getSupportFragmentManager();

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

    // Обработчик нажатия клавиш при вводе имени победителя
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

    // Обработчик событий нажатия иконок в тулбаре
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_game: // Начать игру заново
                gameRenewDialogFragment.show(manager, "gameRenewDialogFragment"); // Вызов диалога перезапуска игры
                return true;
            case R.id.action_clear_winners: // Очистить доску почета
                clearGloryDialogFragment.show(manager, "clearGloryDialogFragment"); // Вызов диалога очистки доски почета
                return true;
            case R.id.action_logout: // Выйти из игры
                quitGameDialogFragment.show(manager, "quitGameDialogFragment"); // Вызов диалога выхода из игры
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Обработчик событий нажатия на костяшки
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

    // Отрисовка кастомного макета тулбара
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Заполнение массива положения костяшек по порядку
    public void fillMatrix(Integer[][] matrix) {
        int n = 1 ;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                matrix[y][x] = n++;
            }
        }
        matrix[3][3] = 0;
    }

    // Сохранение текущей игры для использования при повороте экрана
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable("savedMatrix", matrix);
        savedInstanceState.putInt("savedSteps", stepNumber);
        savedInstanceState.putBoolean("savedInGameStatus", inGame);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Чтение данных из БД для заполнения доски почета
    public void getDataFromDB(){
        db =  Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database").allowMainThreadQueries().build();
        playerDao = db.playerDao();
    }

    // Инициализация объектов доски почета
    public void initializeWinnerFrame(){
        winnerFrame = findViewById(R.id.winner_frame);
        materialCardView = findViewById(R.id.material_card);
    }

    // Отрисовка макета доски почета (либо список победителей, либо ввод имени нового победителя)
    public void fillWinnerFrame(@LayoutRes int layout){
        winnerFrame.removeAllViews();
        winnerFrame = (FrameLayout) LayoutInflater.from(getApplicationContext()).inflate(layout, materialCardView, true);
        if (layout == R.layout.glory_layout) {
            findViewById(R.id.winner_name_edit).setOnKeyListener(gloryListener);
        }
    }

    // Заполнение списка победителей
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

    // Вычисление высоты служебных панелей (для вычисления размера костяшки)
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

    // Запуск приложения
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Запрещаем закрытие диалогов по нажатию в произвольном месте экрана (почему-то setCancelable в классе диалога не срабатывает)
        gameRenewDialogFragment.setCancelable(false);
        clearGloryDialogFragment.setCancelable(false);
        quitGameDialogFragment.setCancelable(false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getDataFromDB();

        initializeWinnerFrame();
        fillWinnerFrame(R.layout.glory_list_layout);

        fillWinnersList();
        fillMatrix(target);

        // Заполнение массива изображений костяшек
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

        // Вычисление размера экрана (для вычисления размера костяшки)
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

        // Считывание текущей игры в случае если событие вызывается после поворота экрана
        if (savedInstanceState != null) {
              matrix = (Integer[][]) savedInstanceState.getSerializable("savedMatrix");
              stepNumber = savedInstanceState.getInt("savedSteps");
              inGame = savedInstanceState.getBoolean("savedInGameStatus");
        } else setBeginState();
        showMatrix();
    }

    //Отображение текущей игровой ситуации после хода игрока и проверка на окончание игры
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

    // Первоначальное заполнение массива костяшек и 500 случайных ходов для запутывания противника :)
    public void setBeginState() {
        fillMatrix(matrix);
        for (int i = 0; i < 501; i++) {
            randomStep();
        }
        stepNumber = 0;
        inGame = true;
        statusText.setText(R.string.in_game);
    }

    // Вычисление списка возможных перемещений костяшки при нажатии (чтоб не выползти за пределы игрового поля)
    public List<Integer[]> possibleCoordinates(int y, int x) {
        List<Integer[]> possibleCoordinates = new ArrayList<>(); // Создали пустой список
        possibleCoordinates.add(new Integer[]{y, x + 1}); //
        possibleCoordinates.add(new Integer[]{y, x - 1}); // Заполнили список всеми
        possibleCoordinates.add(new Integer[]{y + 1, x}); // возможными вариантами хода
        possibleCoordinates.add(new Integer[]{y - 1, x}); //
        List<Integer[]> result = new ArrayList<>();
        for (Integer[] coordinates : possibleCoordinates) { // Вычленили те ходы, которые не выводят за пределы поля
            if (!(coordinates[0] < 0 | coordinates[0] > 3) & !(coordinates[1] < 0 | coordinates[1] > 3)) {
                result.add(coordinates);
            }
        }
        return result;
    }

    // Ход игрока
    public void playersStep(int y, int x) {
        Integer temp;
        List<Integer[]> possibleCoordinates = possibleCoordinates(y, x); // Получили возможные ходы выбранной игроком костяшки
        for (Integer[] coordinates : possibleCoordinates) {
            if (matrix[coordinates[0]][coordinates[1]] == 0) { // Нашли среди них пустой слот
                temp = matrix[y][x];
                matrix[y][x] = matrix[coordinates[0]][coordinates[1]];
                matrix[coordinates[0]][coordinates[1]] = temp; // Переместили выбранную костяшку в пустой слот
                stepNumber++;
                break;
            }
        }
    }

    // Перевод двумерного массива костяшек в одномерный для удобства сравнения массивов. Вообще, конечно, стоит перевести целевой массив в константы, чтоб не считатть его лишний раз
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

    // Процедура выполнения слуайного хода для первоначальной расстановки
    public void randomStep() {
        List<Integer> temp = new ArrayList<>(Arrays.asList(gridToLine(matrix)));
        int randomNum;
        int pos = temp.indexOf(0); // Получили позицию пустого поля
        int y = pos / 4; // Получили координаты
        int x = pos % 4; // пустого поля
        List<Integer[]> possibleCoordinates = possibleCoordinates(y, x); // Получили список возможных вариантов хода
        randomNum = ((int) (Math.random() * possibleCoordinates.size())); // Выбрали случайный ход
        int targetY = possibleCoordinates.get(randomNum)[0];
        int targetX = possibleCoordinates.get(randomNum)[1];
        matrix[y][x] = matrix[targetY][targetX];
        matrix[targetY][targetX] = 0; // Передвинули костяшку
    }
}
