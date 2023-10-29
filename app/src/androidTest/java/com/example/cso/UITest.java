package com.example.cso;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.checkerframework.checker.units.qual.A;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public class UITest {

    @Test
    public void restoreButtonExists(){

        Espresso.onView(ViewMatchers.withId(R.id.restoreButton)).check(matches(withText("Restore")));
    }
}
