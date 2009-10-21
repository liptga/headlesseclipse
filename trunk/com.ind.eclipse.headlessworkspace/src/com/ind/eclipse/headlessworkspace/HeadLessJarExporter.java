package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.application.internal.operations.AppClientComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

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
		SysOutProgressMonitor.out.println("Exporting JARS");

		final List projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		SysOutProgressMonitor.out.println(projects);
		final Iterator it = projects.iterator();
		final File root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		while (it.hasNext())
		{
			final IProject p = (IProject) it.next();

			final String jar = new File(root, p.getName() + ".jar").getAbsolutePath();

			SysOutProgressMonitor.out.println("Exporting .... " + p.getName());
			if (p.getName().equals("bfo.infra"))
			{
				SysOutProgressMonitor.out.println("Exporting project '" + p.getName() + "' to: " + jar);

				final IDataModel dataModel = DataModelFactory.createDataModel(new AppClientComponentExportDataModelProvider());

				dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, p.getName());
				dataModel.setProperty(IJ2EEComponentExportDataModelProperties.EXPORT_SOURCE_FILES, false);
				dataModel.setProperty(IJ2EEComponentExportDataModelProperties.OVERWRITE_EXISTING, true);
				dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, jar);

				dataModel.getDefaultOperation().execute(monitor, null);
			}
		}
	}
}
