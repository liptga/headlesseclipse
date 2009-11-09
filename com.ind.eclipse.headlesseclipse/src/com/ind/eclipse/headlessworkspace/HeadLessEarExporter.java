package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.application.internal.operations.EARComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.EARFacetUtils;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

@SuppressWarnings("restriction")
public class HeadLessEarExporter
{
	private static HeadLessEarExporter instance;

	public static HeadLessEarExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessEarExporter();
		}
		return instance;
	}

	private HeadLessEarExporter()
	{
	}

	@SuppressWarnings("unchecked")
	void exportEars(final IProgressMonitor monitor) throws Exception
	{
		final Set projects = ProjectFacetsManager.getFacetedProjects(EARFacetUtils.EAR_FACET);
		final Iterator it = projects.iterator();
		final File root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		while (it.hasNext())
		{
			final IFacetedProject fp = (IFacetedProject) it.next();
			final IProject p = fp.getProject();

			final String ear = new File(root, p.getName() + ".ear").getAbsolutePath();

			SysOutProgressMonitor.out.println("Exporting project '" + p.getName() + "' to: " + ear);

			final IDataModel dataModel = DataModelFactory.createDataModel(new EARComponentExportDataModelProvider());
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, p.getName());
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.EXPORT_SOURCE_FILES, false);
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.OVERWRITE_EXISTING, true);
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, ear);

			dataModel.getDefaultOperation().execute(monitor, null);
			SysOutProgressMonitor.out.println();
		}
	}
}
