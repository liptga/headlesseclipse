package com.ind.eclipse.headlessworkspace;

import java.io.PrintStream;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class Application implements IApplication
{
	public Object start(IApplicationContext appcontext) throws Exception
	{
		PrintStream err = System.err;
		
		IProgressMonitor monitor = new SysOutProgressMonitor();
        String[] args = (String[]) appcontext.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        SysOutProgressMonitor.out.println("Parameters: " + Arrays.asList(args));
 
		Display d = PlatformUI.createDisplay();
		
		int retcode = PlatformUI.createAndRunWorkbench(d, new HeadLessAdvisor(args, monitor));
		SysOutProgressMonitor.out.println("PlatformUI closed: " + retcode);
		
		err.println(HeadLessBuilder.getInstance().getErrorCount());

		return IApplication.EXIT_OK;
	}

	public void stop()
	{
		SysOutProgressMonitor.out.println("STOPPED");
	}
}
