package com.example.cso;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public class UITest {

    @Before
    public void setUp() {
        ActivityScenario.launch(MainActivity.class);
    }

}
