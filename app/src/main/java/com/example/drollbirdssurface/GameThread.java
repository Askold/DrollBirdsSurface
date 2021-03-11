package com.example.drollbirdssurface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class GameThread extends Thread {
    private SurfaceHolder surfaceHolder;

    private int points = 0;
    private int level = 0;

    // определяем все спрайты
    private Sprite playerBird; //объект птички
    private Sprite enemyBird; // объект вражеской птички
    private Sprite bonus; // объект бонуса
    private Sprite karateGuy; // объект второго врага

    private final int timerInterval = 30; //таймер
    private int viewWidth;
    private int viewHeight;
    Context context;

    private volatile boolean running = true;//флаг для остановки потока
    private boolean touch = true;

    private int towardPointX;
    private int towardPointY;

    public GameThread(Context context, SurfaceHolder surfaceHolder) {
        //------------------птичка-герой------------------------------------------------------------
        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.player);

        int w = b.getWidth()/5;
        int h = b.getHeight()/3;

        Rect firstFrame = new Rect(0, 0, w, h);

        playerBird = new Sprite(10, 0, 0, 100, firstFrame, b);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (i == 2 && j == 3) {
                    continue;
                }
                playerBird.addFrame(new Rect(j * w, i * h,
                        j * w + w, i * w + w));
            }
        }
        //------------------птичка-враг-------------------------------------------------------------
        b = BitmapFactory.decodeResource(context.getResources(), R.drawable.enemy);

        w = b.getWidth()/5;
        h = b.getHeight()/3;

        firstFrame = new Rect(4*w, 0, 5*w, h);

        enemyBird = new Sprite(2000, 250, -300, 0, firstFrame, b);
        for (int i = 0; i < 3; i++) {
            for (int j = 4; j >= 0; j--) {
                if (i ==0 && j == 4) {
                    continue;
                }
                if (i ==2 && j == 0) {
                    continue;
                }
                enemyBird.addFrame(new Rect(j*w, i*h, j*w+w, i*w+w));
            }
        }
        //-------------------бонус------------------------------------------------------------------
        b = BitmapFactory.decodeResource(context.getResources(), R.drawable.bonus);

        w = b.getWidth();
        h = b.getHeight();

        firstFrame = new Rect(0, 0, w, h);

        bonus = new Sprite(2000, 250, -300, 0, firstFrame, b);
        //------------------кликабельный противник--------------------------------------------------
        b = BitmapFactory.decodeResource(context.getResources(), R.drawable.karateguy);

        w = b.getWidth()/5;
        h = b.getHeight();

        firstFrame = new Rect(0, 0, w, h);
        karateGuy = new Sprite(2000, 250, -300, 0, firstFrame, b);
        for (int i = 0; i < 5; i++){
            karateGuy.addFrame(new Rect(i*w, 0, i*w+w, h));
        }


        this.surfaceHolder = surfaceHolder;
    }

    public void requestStop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = surfaceHolder.lockCanvas();
            viewHeight = canvas.getHeight();
            viewWidth = canvas.getWidth();
            if (canvas != null) {
                try {
                    // прорисовываем всех персонажей и фон
                    canvas.drawARGB(250, 127, 199, 255); // заливаем цветом
                    playerBird.draw(canvas);
                    enemyBird.draw(canvas);
                    bonus.draw(canvas);
                    karateGuy.draw(canvas);

                    Paint p = new Paint();
                    p.setAntiAlias(true);
                    p.setTextSize(55.0f);
                    p.setColor(Color.WHITE);
                    canvas.drawText("points: "+points+""+"\nlevel: "+level, 100, 70, p);

                    playerBird.update(timerInterval);
                    enemyBird.update(timerInterval);
                    bonus.update(timerInterval);
                    karateGuy.update((int) (timerInterval*1.5));

                    if (playerBird.getY() + playerBird.getFrameHeight() > viewHeight) {
                        playerBird.setY(viewHeight - playerBird.getFrameHeight());
                        playerBird.setVy(-playerBird.getVy());
                        points--;
                    }
                    else if (playerBird.getY() < 0) {
                        playerBird.setY(0);
                        playerBird.setVy(-playerBird.getVy());
                        points--;
                    }

                    if (enemyBird.getX() < - enemyBird.getFrameWidth()) {
                        teleportEnemy();
                        points +=10;
                    }

                    if (enemyBird.intersect(playerBird)) {
                        teleportEnemy();
                        points -= 10;
                    }

                    if (bonus.getX() < -bonus.getFrameWidth()) {
                        teleportBonus();
                    }

                    if (karateGuy.getX() < - karateGuy.getFrameWidth()) {
                        teleportkarateGuy();
                    }
                    if(karateGuy.isCollition(towardPointX, towardPointY)){ // если кликаем в пределах его фрейма
                        points += 20; // то прибавляем очки
                        teleportkarateGuy(); // и телепортируем его
                    }

                    if (bonus.intersect(playerBird)) {
                        teleportBonus();
                        points += 40;
                    }

                    if (points >= 150){
                        level++; points = 0;
                        enemyBird.setVx(enemyBird.getVx()*1.5);
                    }

                    if((playerBird.getY() > towardPointY) && touch){
                        playerBird.setVy(-Math.abs(playerBird.getVy()));
                        touch = false;
                        points--;
                    } else if ((playerBird.getY() < towardPointY) && touch){
                        playerBird.setVy(Math.abs(playerBird.getVy()));
                        touch = false;
                        points--;
                    }

                    if (points <= -100){
                        Looper.prepare();
                        Toast toast = Toast.makeText(context, "Вы проиграли", Toast.LENGTH_LONG);
                        toast.show();
                    }
                    // рисование на canvas
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    public void setTowardPoint(int x, int y) {
        towardPointX = x;
        towardPointY = y;
        touch = true;
    }

    private void teleportEnemy () {
        enemyBird.setX(viewWidth + Math.random() * 500);
        enemyBird.setY(Math.random() * (viewHeight - enemyBird.getFrameHeight()));
    }

    private void teleportBonus () {
        bonus.setX(viewWidth + Math.random() * 500);
        bonus.setY(Math.random() * (viewHeight - bonus.getFrameHeight()));
    }
    private void teleportkarateGuy() {
        karateGuy.setX(viewWidth + Math.random() * 500);
        karateGuy.setY(Math.random() * (viewHeight - karateGuy.getFrameHeight()));
    }
}
