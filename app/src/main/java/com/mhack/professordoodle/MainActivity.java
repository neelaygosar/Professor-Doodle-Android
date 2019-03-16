package com.mhack.professordoodle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    DrawingView dv;
    private Paint mPaint;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dv = new DrawingView(this);
        setContentView(R.layout.activity_main);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);
    }

    public class DrawingView extends View {

        public int width;
        public int height;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint mBitmapPaint;
        Context context;
        private Paint circlePaint;
        private Path circlePath;
        boolean first = true;
        private String objectid;

        public DrawingView(Context c) {
            super(c);
            context = c;
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            circlePaint = new Paint();
            circlePath = new Path();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeJoin(Paint.Join.MITER);
            circlePaint.setStrokeWidth(4f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawARGB(255, 255, 255, 255);
            uploadCanvas();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
            canvas.drawPath(circlePath, circlePaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;

                circlePath.reset();
                circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
            }
            uploadCanvas();
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            circlePath.reset();
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            HashMap<String, Float> map = new HashMap<>();
            map.put("x", x);
            map.put("y", y);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    map.put("break", 1.0f);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    map.put("break", 0f);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    map.put("break", 0f);
                    break;
            }
//            reference.child("drawing0000").child(String.valueOf(i++)).setValue(map);
            return true;
        }

        private void uploadCanvas() {
//            convertCanvasToImage();
            new Thread(new Runnable() {
                private HttpURLConnection httpURLConnection = null;

                @Override
                public void run() {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] bytes = byteArrayOutputStream.toByteArray();
                    String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                    try {
                        URL url = new URL("http://192.168.0.11:1234/drawing.php");
                        httpURLConnection = (HttpURLConnection) url.openConnection();
//                        httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        httpURLConnection.setReadTimeout(10000);
                        httpURLConnection.setConnectTimeout(15000);
                        httpURLConnection.setRequestMethod("POST");
//                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(true);

/*
                        Uri.Builder uriBuilder = new Uri.Builder()
                                .appendQueryParameter("image", base64)
                                .appendQueryParameter("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
*/
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("image", base64);
                        jsonObject.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

                        if (first) {
                            jsonObject.put("first", "1");
                        } else {
                            jsonObject.put("first", "0");
                            jsonObject.put("id", objectid);
                        }
//                        String query = uriBuilder.build().getEncodedQuery();

                        System.out.println(jsonObject.toString());
                        DataOutputStream os = new DataOutputStream(httpURLConnection.getOutputStream());
                        os.writeBytes(jsonObject.toString());
                        os.flush();
                        os.close();

                        httpURLConnection.connect();

//
//                           BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                        InputStream inputStream = httpURLConnection.getInputStream();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder builder = new StringBuilder();

                        String line;

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        if (first) {
                            objectid = builder.toString();
                            System.out.println(objectid);
                        }
                        first = false;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                    }

                }
            }).start();
        }

        private void convertCanvasToImage() {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(getExternalCacheDir() + "/temp.jpeg");
            } catch (Exception e) {
            }
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
        }

    }


}