package com.ton.movementmeasurer2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Original extends AppCompatActivity {

    LinkedList<Entry> list;
    double x = 0, y = 0, z = 0;
    int index = 0;
    double velocityX = 0;
    double velocityY = 0;
    double velocityZ = 0;
    double positionX = 0;
    double positionY = 0;
    double positionZ = 0;
    final int pointsShowing = 50;
    long lastSampleTime = 0;

    LineChart lineChartView;
    TextView distanceView;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceView = findViewById(R.id.text_view);
        lineChartView = findViewById(R.id.line_chart);
        Description description = new Description();
        description.setText("运动数据");
        lineChartView.setDescription(description);

        XAxis xAxis = lineChartView.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(10, false);

        YAxis yAxis = lineChartView.getAxisLeft();
        yAxis.setLabelCount(5, false);

        list = new LinkedList<>();
        for (int i = 0;i < pointsShowing;i++) {
            list.addLast(new Entry(i, 0));
            index++;
        }
        setDataToChart(list);


        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        lastSampleTime = System.currentTimeMillis();
        sensorManager.registerListener(sensorEventListener, accelerationSensor, SensorManager.SENSOR_DELAY_GAME);

        if (accelerationSensor == null) {
            Toast.makeText(this, "无法获取加速计", Toast.LENGTH_SHORT).show();
        }

        Timer timer = new Timer(true);
        timer.schedule(timerTask, 100, 100);
    }

    private void setDataToChart(List<Entry> data) {
        LineDataSet dataSet = new LineDataSet(data, "速度");
        dataSet.setDrawCircles(false);
        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);
        lineChartView.setData(lineData);
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                x = limitValue(sensorEvent.values[0]);
                y = limitValue(sensorEvent.values[1]);
                z = limitValue(sensorEvent.values[2]);
                long nowTime = System.currentTimeMillis();
                double interval = (nowTime - lastSampleTime) * 0.0001;
                lastSampleTime = nowTime;
                velocityX += x * interval;
                velocityY += y * interval;
                velocityZ += z * interval;
                positionX += velocityX * interval;
                positionY += velocityY * interval;
                positionZ += velocityZ * interval;
            }
        }

        private float limitValue(float num) {
            if (Math.abs(num) < 0.01) {
                return 0;
            }
            return num;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {

        Activity activity;
        public MyHandler(Activity activity) {
            super();
            this.activity = activity;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {

                double movement = Math.sqrt(Math.pow(positionX, 2) + Math.pow(positionY, 2) + Math.pow(positionZ, 2));
                double velocityVector = Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityY, 2) + Math.pow(velocityZ, 2));

                if (list.size() >= pointsShowing) {
                    list.removeFirst();
                }

                list.addLast(new Entry(index, (float)velocityVector));

                distanceView.setText("Total Distance: " + movement + " m");
                index++;
                setDataToChart(list);
                lineChartView.postInvalidate();
            }
        }
    }

    MyHandler handler = new MyHandler(this);

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.sendEmptyMessage(1);
        }
    };
}