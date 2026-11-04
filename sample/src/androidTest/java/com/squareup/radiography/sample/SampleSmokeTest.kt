package com.squareup.radiography.sample

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class SampleSmokeTest {

  @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Test
  fun displaysInitialScreen() {
    onView(withText("Remember me")).check(matches(isDisplayed()))
  }
}
