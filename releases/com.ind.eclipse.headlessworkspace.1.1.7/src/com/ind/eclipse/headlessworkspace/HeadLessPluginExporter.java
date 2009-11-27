package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;

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
		final File rootDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		final File zipFile = new File(rootDir, "exportedplugins.zip");
		zipFile.delete();

		final FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = false;
		info.useJarFormat = true;
		info.exportSource = false;
		info.zipFileName = zipFile.getName();
		info.items = getPlugins();
		info.qualifier = null;
		info.destinationDirectory = rootDir.toString();

		final Class pluginExportOperationClass = Class.forName("org.eclipse.pde.internal.core.exports.PluginExportOperation");
		final String jobName = "Plugin export job";
		final List<Object> arguments = new ArrayList();
		Constructor constructor = null;

		//first we look for single argument constructor, which is present in eclipse 3.4
		try
		{
			arguments.add(info);
			constructor = pluginExportOperationClass.getConstructor(info.getClass());
		}
		catch (final NoSuchMethodException nsme)
		{
			arguments.add(jobName);
			constructor = pluginExportOperationClass.getConstructor(info.getClass(), String.class);
		}

		final Object peo = constructor.newInstance(arguments.toArray());
		Method runMethod = null;
		Class currentClass = pluginExportOperationClass;
		while (runMethod == null)
		{
			try
			{
				runMethod = currentClass.getDeclaredMethod("run", IProgressMonitor.class);
			}
			catch (final NoSuchMethodException nsme)
			{
				SysOutProgressMonitor.out.println("run(IProgressMonitor) not found in " + currentClass.getName() + " trying in parent...");
				currentClass = currentClass.getSuperclass();
				if (currentClass == null)
					throw new Exception("run(IProgressMonitor) not found in any of the ancestors of " + pluginExportOperationClass.getName());
			}
		}
		runMethod.setAccessible(true);
		runMethod.invoke(peo, monitor);
		//		final PluginExportOperation peo = new PluginExportOperation(info);
		SysOutProgressMonitor.out.println();
	}

	@SuppressWarnings( { "unchecked" })
	public Object[] getPlugins()
	{
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		final ArrayList models = new ArrayList();
		for (int i = 0; i < projects.length; i++)
		{
			final IPluginModelBase model = PluginRegistry.findModel(projects[i].getName());
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
			SysOutProgressMonitor.out.print(((IPluginModelBase) models.get(i)).getPluginBase().getId());
		}
		SysOutProgressMonitor.out.println(" to 'exportedplugins.zip'");
		return models.toArray(new IPluginModelBase[models.size()]);
	}
}
