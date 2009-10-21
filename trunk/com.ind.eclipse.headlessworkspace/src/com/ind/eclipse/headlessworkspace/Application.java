package com.ind.eclipse.headlessworkspace;

import java.io.PrintStream;
import java.util.Arrays;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class Application implements IApplication
{
	public Object start(final IApplicationContext appcontext) throws Exception
	{
		final IProgressMonitor monitor = new SysOutProgressMonitor();
		final PrintStream err = System.err;

		final String[] args = (String[]) appcontext.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		SysOutProgressMonitor.out.println("Parameters: " + Arrays.asList(args));

		final Display d = PlatformUI.createDisplay();

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.getPathVariableManager().setValue("workspace", workspace.getRoot().getLocation());
		SysOutProgressMonitor.out.println("\"workspace\" path variable has been set.");
		final int retcode = PlatformUI.createAndRunWorkbench(d, new HeadLessAdvisor(args, monitor));
		SysOutProgressMonitor.out.println("PlatformUI closed: " + retcode);

		err.println(HeadLessBuilder.getInstance().getErrorCount());

		return IApplication.EXIT_OK;
	}

	public void stop()
	{
		SysOutProgressMonitor.out.println("STOPPED");
	}
}
