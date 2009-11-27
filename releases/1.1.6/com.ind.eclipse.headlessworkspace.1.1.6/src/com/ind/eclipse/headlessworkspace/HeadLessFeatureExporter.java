package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

@SuppressWarnings("restriction")
public class HeadLessFeatureExporter
{
	private static HeadLessFeatureExporter instance;

	public static HeadLessFeatureExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessFeatureExporter();
		}
		return instance;
	}

	private HeadLessFeatureExporter()
	{
	}

	void exportFeatures(final IProgressMonitor monitor) throws Exception
	{
		final File rootDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		final File zipFile = new File(rootDir, "exportedfeatures.zip");
		zipFile.delete();

		final FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = false;
		info.useJarFormat = true;
		info.exportSource = false;
		info.zipFileName = zipFile.getName();
		info.items = getFeatures();
		info.qualifier = null;
		info.destinationDirectory = rootDir.toString();

		final Class featureExportOperationClass = Class.forName("org.eclipse.pde.internal.core.exports.FeatureExportOperation");
		final String jobName = "Feature export job";
		final List<Object> arguments = new ArrayList();
		Constructor constructor = null;

		//first we look for single argument constructor, which is present in eclipse 3.4
		try
		{
			arguments.add(info);
			constructor = featureExportOperationClass.getConstructor(info.getClass());
		}
		catch (final NoSuchMethodException nsme)
		{
			arguments.add(jobName);
			constructor = featureExportOperationClass.getConstructor(info.getClass(), String.class);
		}

		final Object feo = constructor.newInstance(arguments.toArray());
		Method runMethod = null;
		Class currentClass = featureExportOperationClass;
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
					throw new Exception("run(IProgressMonitor) not found in any of the ancestors of " + featureExportOperationClass.getName());
			}
		}
		runMethod.setAccessible(true);
		runMethod.invoke(feo, monitor);
		SysOutProgressMonitor.out.println();
	}

	@SuppressWarnings( { "unchecked" })
	public Object[] getFeatures()
	{
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		final ArrayList models = new ArrayList();
		final FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		for (int i = 0; i < projects.length; i++)
		{
			final IFeatureModel model = manager.findFeatureModel(projects[i].getName());
			if (model != null)
			{
				models.add(model);
			}
		}
		SysOutProgressMonitor.out.print("Exporting Features: ");
		for (int i = 0; i < models.size(); i++)
		{
			if (i > 0)
			{
				SysOutProgressMonitor.out.print(", ");
			}
			SysOutProgressMonitor.out.print(((IFeatureModel) models.get(i)).getFeature().getId());
		}
		SysOutProgressMonitor.out.println(" to 'exportedfeatures.zip'");
		return models.toArray(new IFeatureModel[models.size()]);
	}
}
