package com.ind.eclipse.headlessworkspace;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;

public class StatusHandler implements IStatusHandler
{

	public Object handleStatus(final IStatus status, final Object source) throws CoreException
	{
		SysOutProgressMonitor.out.println("Status came from source object:" + source + ".");
		SysOutProgressMonitor.out.println("\tPlugin: " + status.getPlugin());
		SysOutProgressMonitor.out.println("\tMessage: " + status.getMessage());
		SysOutProgressMonitor.out.println("\tSeverity: " + status.getSeverity());
		if (status.getException() != null)
		{
			SysOutProgressMonitor.out.println("\tException: ");
			status.getException().printStackTrace(SysOutProgressMonitor.out);
		}
		SysOutProgressMonitor.out.println("\tCode" + status.getCode());
		return null;
	}

}
