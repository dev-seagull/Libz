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

    @Test
    public void restoreButtonExists(){
        Espresso.onView(ViewMatchers.withId(R.id.restoreButton))
                .check(matches(withText("Restore")));
    }

    @Test
    public void headerExists(){
        Espresso.onView(ViewMatchers.withId(R.id.headerTextView)).
                check(ViewAssertions.matches(ViewMatchers.withText("CSO")));

        Espresso.onView(ViewMatchers.withId(R.id.infoButton))
                .check(matches(withText("i")));
    }

    @Test
    public void drawerLayoutExists(){
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
                .check(ViewAssertions.doesNotExist());

        Espresso.onView(ViewMatchers.withId(R.id.navigationView))
                .check(ViewAssertions.doesNotExist());
    }
}
