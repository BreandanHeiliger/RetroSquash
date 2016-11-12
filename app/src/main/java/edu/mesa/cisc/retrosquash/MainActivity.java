package edu.mesa.cisc.retrosquash;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends Activity {

    Canvas canvas;
    SquashCourtView squashCourtView;

    //Sound
    //initialize sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //Used for getting display details like the number of pixels
    Display display;
    Point size;
    int screenWidth, screenHeight, hScreenWidth, hScreenHeight;

    //!!Candidates for class!!!
    //Game objects
    int hr1Width, hr1Height, hr2Width, hr2Height;
    Point r1Position,r2Position;

    Point ballPosition;
    int ballWidth, ballSpeed;

    //for ball movement
    boolean ballMLeft, ballMRight, ballMUp, ballMDown;

    //for racket movement
    boolean r1mLeft, r1mRight, r2mLeft,r2mRight;

    //stats
    long lastFrameTime;
    int fps;
    int score;
    int lives;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        squashCourtView = new SquashCourtView(this);
        setContentView(squashCourtView);

        ballSpeed = 10;

        lives = 3;

        //Sound code
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            //Create objects of the 2 required classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create our three fx in memory ready for use
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);


            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);


        } catch (IOException e) {
            //catch exceptions here
        }

        
        //Get the screen size in pixels
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        hScreenWidth = screenWidth/2;
        hScreenHeight = screenHeight/2;


        //The game objects
        r1Position = new Point();
        r1Position.x = screenWidth/2;
        r1Position.y = screenHeight - (2*(screenHeight/100));
        hr1Width = screenWidth/8;
        hr1Height = screenHeight/100;

        r2Position = new Point();
        r2Position.x = screenWidth/2;
        r2Position.y = 2 * (screenHeight/100);
        hr2Width = screenWidth/8;
        hr2Height = screenHeight/100;


        ballWidth = screenWidth/36;
        ballPosition = new Point();
        ballPosition.x = hScreenWidth;
        ballPosition.y = hScreenHeight;



    }

    class SquashCourtView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSquash;
        Paint paint;

        public SquashCourtView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballMDown = true;

            Random randomNumber = new Random();
            int ballDirection = randomNumber.nextInt(3);
            switch (ballDirection) {
                case 0:
                    ballMLeft = true;
                    ballMRight = false;
                    break;

                case 1:
                    ballMRight = true;
                    ballMLeft = false;
                    break;

                case 2:
                    ballMLeft = false;
                    ballMRight = false;
                    break;
            }


        }


        @Override
        public void run() {
            while (playingSquash) {
                updateCourt();
                drawCourt();
                controlFPS();

            }

        }

        public void updateCourt() {

            //Move rackets
            if (r1mRight && r1Position.x + hr1Width < screenWidth)
                r1Position.x = r1Position.x + 20;

            if (r1mLeft && r1Position.x - hr1Width > 0 )
                r1Position.x = r1Position.x - 20;

            if (r2mRight && r2Position.x + hr2Width < screenWidth)
                r2Position.x = r2Position.x + 20;

            if (r2mLeft && r2Position.x - hr2Width > 0)
                r2Position.x = r2Position.x - 20;

            //detect collisions

            //hit right of screen
            if (ballPosition.x + ballWidth >= screenWidth - screenHeight/100) {
                ballMLeft = true;
                ballMRight = false;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //hit left of screen
            if (ballPosition.x - ballWidth <= screenHeight/100) {
                ballMLeft = false;
                ballMRight = true;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //Edge of ball has hit top or bottom of screen
            if (ballPosition.y + ballWidth > screenHeight - screenHeight/100 || ballPosition.y-ballWidth <= screenHeight/100) {

                ballPosition.y = screenHeight/2;

                lives-=1;
                ballSpeed+=3;

                if (lives < 0) {

                    lives = 3;
                    score = 0;
                    soundPool.play(sample4, 1, 1, 0, 0, 1);

                    ballSpeed=10;
                    ballPosition.y = hScreenHeight;//back to the middle of the screen.
                }

                //what horizontal direction should we use
                //for the next ball
                Random randomNumber = new Random();
                int startX = randomNumber.nextInt(screenWidth - (2 * ballWidth) )+ 1;
                ballPosition.x = startX ;

                int ballDirection = randomNumber.nextInt(3);
                switch (ballDirection) {
                    case 0:
                        ballMLeft = true;
                        ballMRight = false;
                        break;

                    case 1:
                        ballMRight = true;
                        ballMLeft = false;
                        break;

                    case 2:
                        ballMLeft = false;
                        ballMRight = false;
                        break;
                }


            }



            //depending upon the two directions we should be
            //moving in adjust our x any positions
            if (ballMDown) {
                ballPosition.y += ballSpeed;
            }

            if (ballMUp) {
                ballPosition.y -= ballSpeed;
            }

            if (ballMLeft) {
                ballPosition.x -= ballSpeed;
            }

            if (ballMRight) {
                ballPosition.x += ballSpeed;
            }

            //Has ball hit racket 1
            if (ballPosition.y + ballWidth >= r1Position.y - hr1Height) {

                if (ballPosition.x - ballWidth > (r1Position.x - hr1Width) && ballPosition.x + ballWidth < (r1Position.x + hr1Width)) {
                    //rebound the ball and play a sound
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballSpeed++;
                    ballMUp = true;
                    ballMDown = false;
                    //now decide how to rebound the ball
                    if (ballPosition.x > r1Position.x) {
                        ballMRight = true;
                        ballMLeft = false;

                    } else {
                        ballMRight = false;
                        ballMLeft = true;
                    }

                }
            }

            //Has ball hit racket 2
            if (ballPosition.y - ballWidth <= r2Position.y + hr2Height) {

                if (ballPosition.x - ballWidth > (r2Position.x - hr2Width) && ballPosition.x + ballWidth < (r2Position.x + hr2Width)) {
                    //rebound the ball and play a sound
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballSpeed++;
                    ballMUp = false;
                    ballMDown = true;
                    //now decide how to rebound the ball
                    if (ballPosition.x > r2Position.x) {
                        ballMRight = true;
                        ballMLeft = false;

                    } else {
                        ballMRight = false;
                        ballMLeft = true;
                    }

                }
            }
        }

        public void drawCourt() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                //Paint paint = new Paint();
                canvas.drawColor(Color.BLACK);//the background
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(45);
                canvas.drawText("Score:" + score + " Lives:" + lives + " fps:" + fps, 50, 200, paint);


                //Draw the squash racket
                canvas.drawRect(r1Position.x-hr1Width, r1Position.y-hr1Height, r1Position.x+hr1Width, r1Position.y+ hr1Height, paint);

                canvas.drawRect(r2Position.x-hr2Width,r2Position.y-hr2Height, r2Position.x+hr2Width, r2Position.y+ hr2Height, paint);

                //Draw the ball
                canvas.drawRect(ballPosition.x-ballWidth, ballPosition.y-ballWidth, ballPosition.x +ballWidth, ballPosition.y + ballWidth, paint);

                //Draw actuall court.
                canvas.drawRect(0,((screenHeight/2) - (screenHeight/180)),screenWidth, ( (screenHeight/2) + (screenHeight/180) ),paint );
                canvas.drawRect(0,0,screenHeight/90, screenHeight, paint);
                canvas.drawRect(screenWidth-(screenHeight/90),0,screenWidth, screenHeight, paint);
                canvas.drawRect(0, 0, screenWidth, screenHeight/90, paint);
                canvas.drawRect(0, screenHeight - (screenHeight/90), screenWidth,screenHeight,paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }

        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 15 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {

                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                }

            }

            lastFrameTime = System.currentTimeMillis();
        }


        public void pause() {
            playingSquash = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
            }

        }

        public void resume() {
            playingSquash = true;
            ourThread = new Thread(this);
            ourThread.start();
        }


        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:

                    if (motionEvent.getY() > hScreenHeight)
                    {
                        if (motionEvent.getX() > hScreenWidth) {
                            r1mRight = true;
                            r1mLeft = false;
                        } else {
                            r1mLeft = true;
                            r1mRight = false;
                        }

                    } else {
                        if (motionEvent.getX() >= hScreenWidth) {
                            r2mRight = true;
                            r2mLeft = false;
                        } else {
                            r2mLeft = true;
                            r2mRight = false;
                        }
                    }
                    break;


                case MotionEvent.ACTION_UP:
                    r1mRight = r1mLeft = r2mLeft = r2mRight = false;
                    break;
            }
            return true;
        }//End onTouchEvent

    }//End SquashCourtView

    @Override
    protected void onStop() {
        super.onStop();

        while (true) {
            squashCourtView.pause();
            break;
        }

        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        squashCourtView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        squashCourtView.pause();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            squashCourtView.pause();
            finish();
            return true;
        }
        return false;
    }


}//End Main Activity
