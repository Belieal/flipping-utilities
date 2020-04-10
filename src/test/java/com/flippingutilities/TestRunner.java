package com.flippingutilities;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This class is responsible for running each test class so you don't have to run each file manually,
 * just add your test class below, inside @Suite.SuiteClasses({}).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	HistoryManagerTest.class
})
public class TestRunner {

}