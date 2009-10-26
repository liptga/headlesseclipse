package com.ind.eclipse.headlessworkspace;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.jarpackager.IJarDescriptionReader;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

public class HeadLessJarExporter
{
	private static HeadLessJarExporter instance;

	public static HeadLessJarExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessJarExporter();
		}
		return instance;
	}

	private HeadLessJarExporter()
	{
	}
	
	@SuppressWarnings("unchecked")
	void exportJars(final IProgressMonitor monitor) throws Exception
	{
		final ArrayList jardescs = new ArrayList();
		ResourcesPlugin.getWorkspace().getRoot().accept( new IResourceVisitor(){

			public boolean visit(IResource res) throws CoreException
			{
				if (res.getName().endsWith(".jardesc"))
				{
					jardescs.add(res);
				}
				if (res.getProjectRelativePath().segmentCount() > 0)
				{
					return false;
				}
				return true;
			}});
		
		for (int i = 0; i < jardescs.size(); i++)
		{
			IFile jardesc = (IFile) jardescs.get(i);
			JarPackageData jarPackage = new JarPackageData();
			IJarDescriptionReader reader = jarPackage.createJarDescriptionReader(jardesc.getContents());
			reader.read(jarPackage);
			jarPackage.setSaveManifest(false);
			jarPackage.setSaveDescription(false);
			jarPackage.setOverwrite(true);
			jarPackage.setBuildIfNeeded(false);
			IPath path = jarPackage.getJarLocation();
			if (path.segmentCount() > 1)
			{
				jarPackage.setJarLocation(path.removeFirstSegments(path.segmentCount() - 1));
			}
			SysOutProgressMonitor.out.println("Exporting jar file using jar descriptor: " + jardesc.getFullPath() + " to: " + jarPackage.getAbsoluteJarLocation().toOSString());
			IJarExportRunnable export = jarPackage.createJarExportRunnable(null);
			export.run(monitor);
		}
	}
}
