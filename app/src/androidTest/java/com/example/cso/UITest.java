package com.example.cso;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.example.cso.R;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestRunner.class)
public class UITest {

    @Before
    public void setUp() {
        ActivityScenario.launch(MainActivity.class); // Replace YourActivity with your actual activity class
    }

    @Test
    public void restoreButtonExists(){
//
//        Espresso.onView(ViewMatchers.withId(R.id.restoreButton))
//                .check(matches(withText("Restore")));

        Espresso.onView(ViewMatchers.withId(R.id.restoreButton)).check(matches(withText("Restore")));
    }
}
