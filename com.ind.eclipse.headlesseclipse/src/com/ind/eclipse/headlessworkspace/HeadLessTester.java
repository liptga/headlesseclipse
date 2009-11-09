package com.ind.eclipse.headlessworkspace;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.ui.launcher.JUnitWorkbenchLaunchShortcut;


/***********************************************************************************
 * The class HeadLessTester …
 * Write down the class documentation here!!!!
 * 
 * @author $Author$
 * @version $Revision$
 *
 * Copyright (c)2009 by Prueftechnik Condition Monitoring GmbH
 * All Rights reserved.
 */
@SuppressWarnings("restriction")
public class HeadLessTester extends TestRunListener
{
    /*##############################################################################
     * PUBLIC DATA MEMBERS
     */


    /*##############################################################################
     * PROTECTED AND PACKAGE-ACCESSABLE DATA MEMBERS
     */


    /*##############################################################################
     * PUBLIC MEMBER METHODS
     */

    /*******************************************************************************
     * @return single instance of this class.
     */
    public static HeadLessTester getInstance()
    {
        if (instance == null)
        {
            instance = new HeadLessTester();
        }
        return instance;
        
    } // END getInstance

    /******************************************************************************
	 * @see org.eclipse.jdt.junit.TestRunListener#sessionFinished(
	 * org.eclipse.jdt.junit.model.ITestRunSession)
	 */
	@Override
	public void sessionFinished(ITestRunSession session) 
	{
        SysOutProgressMonitor.out.println("TEST RUN SESSION FINISHED...");
        
        if (session instanceof TestRunSession)
        {
	        File exportfile = new File("test-export.xml");
	        SysOutProgressMonitor.out.println("EXPORT RUN SESSION to " + 
	        		exportfile.getAbsolutePath());
	        
	        try 
	        {
	        	JUnitModel.exportTestRunSession((TestRunSession) session, exportfile);	
			} 
	        catch (CoreException e) 
			{
	        	SysOutProgressMonitor.out.println("EXPORT FAILED!!!");
	        	e.printStackTrace(SysOutProgressMonitor.out);
			}
        }
		
	} // END sessionFinished

	/******************************************************************************
	 * @see org.eclipse.jdt.junit.TestRunListener#sessionStarted(
	 * org.eclipse.jdt.junit.model.ITestRunSession)
	 */
	@Override
	public void sessionStarted(ITestRunSession session) 
	{
        SysOutProgressMonitor.out.println("TEST RUN SESSION STARTED...");
        
	} // END sessionStarted


	/******************************************************************************
	 * @see org.eclipse.jdt.junit.TestRunListener#testCaseFinished(
	 * org.eclipse.jdt.junit.model.ITestCaseElement)
	 */
	@Override
	public void testCaseFinished(ITestCaseElement testCaseElement) 
	{
        SysOutProgressMonitor.out.print("TEST CASE FINISHED... ");
        SysOutProgressMonitor.out.print(testCaseElement.getTestClassName());
        SysOutProgressMonitor.out.println("."  + testCaseElement.getTestMethodName());
        
        Result res = testCaseElement.getTestResult(false);
        SysOutProgressMonitor.out.print("Run Time: " + testCaseElement.getElapsedTimeInSeconds());
        SysOutProgressMonitor.out.println(" Result: " + res.toString());
        
        if (res != Result.OK)
        {
        	FailureTrace trace = testCaseElement.getFailureTrace();
        	if (trace != null && trace.getTrace() != null)
        	{
        		SysOutProgressMonitor.out.println(trace.getTrace());
        	}
        }
        
	} // END testCaseFinished

	/******************************************************************************
	 * @see org.eclipse.jdt.junit.TestRunListener#testCaseStarted(
	 * org.eclipse.jdt.junit.model.ITestCaseElement)
	 */
	@Override
	public void testCaseStarted(ITestCaseElement testCaseElement) 
	{
        SysOutProgressMonitor.out.print("TEST CASE STARTED... ");
        SysOutProgressMonitor.out.print(testCaseElement.getTestClassName());
        SysOutProgressMonitor.out.println("."  + testCaseElement.getTestMethodName());
        
	} // END testCaseStarted
	
    
    /*##############################################################################
     * PROTECTED AND PACKAGE-ACCESSABLE MEMBER METHODS
     */

	/*******************************************************************************
     * Executes {@link AllTests} suite.
     * 
     * @param monitor the progress monitor
     */
    void runAllTests(IProgressMonitor monitor) throws Exception
    {	
    	SysOutProgressMonitor.out.println("CREATE ALL TESTS PROJECT...");
    	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("com.pruftechnik.rcm.alltests");
    	IJavaProject javaProject = JavaCore.create(project);
        
        ILaunchConfiguration[] lc = new JUnitWorkbenchLaunchShortcut().getLaunchConfigurations(
        		new StructuredSelection(javaProject));
    	    	
    	if (lc != null && lc.length > 0)
    	{    	
    		SysOutProgressMonitor.out.println("Launch configurations found: " + lc.length);
    		
    		for (ILaunchConfiguration launchConfiguration : lc) 
    		{
            	SysOutProgressMonitor.out.println("LAUNCH configuration " + 
            			launchConfiguration.getLocation().toOSString());

    			launchConfiguration.launch(ILaunchManager.RUN_MODE, monitor, true);	
			}
    	}
    	else
    	{
    		SysOutProgressMonitor.out.println("ERROR: Failed to get launch configuration...");
    	}    	

    	SysOutProgressMonitor.out.println("END runAllTests");
        
    } // END runAllTests
    

    /*##############################################################################
     * PRIVATE MEMBER METHODS
     */

	/**
	 * 
	 */
	private HeadLessTester() 
	{
		JUnitCore.addTestRunListener(this);
	}

	
    /*##############################################################################
     * INNER CLASSES
     */
        
	
    /*##############################################################################
     * PRIVATE DATA MEMBERS
     */

    private static HeadLessTester instance;

    
} // END class HeadLessTester


// END OF FILE
