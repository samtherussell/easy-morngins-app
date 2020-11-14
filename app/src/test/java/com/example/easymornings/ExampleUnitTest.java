package com.example.easymornings;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {

        int hour = 18;
        int minute = 48;

        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) > hour || now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) >= minute) {
            now.add(Calendar.DATE, 1);
        }
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, 0);
        assertEquals(4, 2 + 2);
    }
}