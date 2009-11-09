package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
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
		File rootDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
		File zipFile = new File(rootDir, "exportedfeatures.zip");
		zipFile.delete();
		
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = false;
		info.useJarFormat = true;
		info.exportSource = false;
		info.zipFileName = zipFile.getName();
		info.items = getFeatures();
		info.qualifier = null;
		info.destinationDirectory = rootDir.toString();
		
		FeatureExportOperation peo = new FeatureExportOperation(info);
		peo.run(monitor);
		SysOutProgressMonitor.out.println();
	}

	@SuppressWarnings({"unchecked"})
	public Object[] getFeatures()
	{
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList models = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		for (int i = 0; i < projects.length; i++)
		{
			IFeatureModel model = manager.findFeatureModel(projects[i].getName());
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
			SysOutProgressMonitor.out.print(((IFeatureModel)models.get(i)).getFeature().getId());
		}
		SysOutProgressMonitor.out.println(" to 'exportedfeatures.zip'");
		return (IFeatureModel[]) models.toArray(new IFeatureModel[models.size()]);
	}
}
