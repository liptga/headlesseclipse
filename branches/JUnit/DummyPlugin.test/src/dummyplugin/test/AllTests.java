package dummyplugin.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite(){
        TestSuite suite = new TestSuite("DummyPlugin Test");
        //$JUnit-BEGIN$
        suite.addTestSuite(MessageProviderTest.class);
        //$JUnit-END$
        return suite;

	}
}
