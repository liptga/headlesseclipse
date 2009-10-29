package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

@SuppressWarnings("restriction")
public class HeadLessWarExporter
{
	private static HeadLessWarExporter instance;

	public static HeadLessWarExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessWarExporter();
		}
		return instance;
	}

	private HeadLessWarExporter()
	{
	}

	@SuppressWarnings("unchecked")
	void exportWars(final IProgressMonitor monitor) throws Exception
	{
		final Set projects = ProjectFacetsManager.getFacetedProjects(WebFacetUtils.WEB_FACET);
		final Iterator it = projects.iterator();
		final File root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		while (it.hasNext())
		{
			final IFacetedProject fp = (IFacetedProject) it.next();
			final IProject p = fp.getProject();

			final String war = new File(root, p.getName() + ".war").getAbsolutePath();

			SysOutProgressMonitor.out.println("Exporting project '" + p.getName() + "' to: " + war);

			final IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, p.getName());
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.EXPORT_SOURCE_FILES, false);
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.OVERWRITE_EXISTING, true);
			dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, war);

			dataModel.getDefaultOperation().execute(monitor, null);
		}
	}
}
