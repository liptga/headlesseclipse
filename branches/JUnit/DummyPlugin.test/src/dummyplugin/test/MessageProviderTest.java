package dummyplugin.test;

import junit.framework.TestCase;
import dummyplugin.MessageProvider;

public class MessageProviderTest extends TestCase {
 
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("SETUP");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		System.out.println("TEARDOWN");
	}

	public void testDummy() throws Exception {
		assertTrue(true);
	}
	
	public void testMakeGreeting() throws Exception {
		String result = (new MessageProvider()).makeGreeting();
		String expected = "Good morning";
		assertEquals(expected, result);
	}
	
	public void testFailer() throws Exception {
		fail();
	}
}
