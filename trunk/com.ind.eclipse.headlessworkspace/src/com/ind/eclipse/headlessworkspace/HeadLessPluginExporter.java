package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;

@SuppressWarnings("restriction")
public class HeadLessPluginExporter
{
	private static HeadLessPluginExporter instance;

	public static HeadLessPluginExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessPluginExporter();
		}
		return instance;
	}

	private HeadLessPluginExporter()
	{
	}
	
	void exportPlugins(final IProgressMonitor monitor) throws Exception
	{
		File rootDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		File zipFile = new File(rootDir, "exportedplugins.zip");
		zipFile.delete();
		
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = false;
		info.useJarFormat = true;
		info.exportSource = false;
		info.zipFileName = zipFile.getName();
		info.items = getPlugins();
		info.qualifier = null;
		info.destinationDirectory = rootDir.toString();
		
		PluginExportOperation peo = new PluginExportOperation(info);
		peo.run(monitor);
	}

	@SuppressWarnings({"unchecked"})
	public Object[] getPlugins() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList models = new ArrayList();
		for (int i = 0; i < projects.length; i++)
		{
			IPluginModelBase model = PluginRegistry.findModel(projects[i].getName());
			if (model != null)
				models.add(model);
		}
		SysOutProgressMonitor.out.print("Exporting Plugins: ");
		for (int i = 0; i < models.size(); i++)
		{
			if (i > 0)
			{
				SysOutProgressMonitor.out.print(", ");
			}
			SysOutProgressMonitor.out.print(((IPluginModelBase)models.get(i)).getPluginBase().getId());
		}
		SysOutProgressMonitor.out.println(" to 'exportedplugins.zip'");
		return (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
	}
}
