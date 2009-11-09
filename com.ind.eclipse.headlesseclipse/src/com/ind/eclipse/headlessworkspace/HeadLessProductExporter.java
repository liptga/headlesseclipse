package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;

@SuppressWarnings("restriction")
public class HeadLessProductExporter
{
	private static HeadLessProductExporter instance;

	public static HeadLessProductExporter getInstance()
	{
		if (instance == null)
		{
			instance = new HeadLessProductExporter();
		}
		return instance;
	}

	private HeadLessProductExporter()
	{
	}
	
	@SuppressWarnings("unchecked")
	void exportProducts(final IProgressMonitor monitor) throws Exception
	{
		final ArrayList products = new ArrayList();
		ResourcesPlugin.getWorkspace().getRoot().accept( new IResourceVisitor(){

			public boolean visit(IResource res) throws CoreException
			{
				if (res.getName().endsWith(".product"))
				{
					products.add(res);
				}
				if (res.getProjectRelativePath().segmentCount() > 0)
				{
					return false;
				}
				return true;
			}});
		
		for (int i = 0; i < products.size(); i++)
		{
			IFile product = (IFile) products.get(i);
			WorkspaceProductModel wpm = new WorkspaceProductModel(product, false);
			wpm.load();
			IProduct prod = wpm.getProduct();
			
			File zipFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), product.getName().substring(0, product.getName().length() - 8) + ".zip");

			SysOutProgressMonitor.out.println("Exporting Eclipse Product using '" + product.getLocation() + "' as configuration to: " + zipFile);
			
			FeatureExportInfo info = new FeatureExportInfo();
			info.useJarFormat = true;
			info.zipFileName = zipFile.getName();
			if (prod.useFeatures())
			{
				info.items = getFeatures(prod);
			}
			else
			{
				info.items = getPlugins(prod);
			}
			info.destinationDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
			
			ProductExportOperation peo = new ProductExportOperation(info, prod, product.getName().substring(0, product.getName().length() - 8));
			peo.run(monitor);
			SysOutProgressMonitor.out.println();
		}
	}
	
	@SuppressWarnings("unchecked")
	private IFeatureModel[] getFeatures(IProduct prod) {
		ArrayList list = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IProductFeature[] features = prod.getFeatures();
		for (int i = 0; i < features.length; i++) {
			IFeatureModel model = manager.findFeatureModel(features[i].getId(), features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	@SuppressWarnings("unchecked")
	private BundleDescription[] getPlugins(IProduct prod) {
		ArrayList list = new ArrayList();
		State state = TargetPlatformHelper.getState();
		IProductPlugin[] plugins = prod.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription bundle = state.getBundle(plugins[i].getId(), null);
			if (bundle != null)
				list.add(bundle);
		}
		// implicitly add the new launcher plug-in/fragment if we are to use the
		// new launching story and the launcher plug-in/fragment are not already included in the .product file
		IPluginModelBase launcherPlugin = PluginRegistry.findModel("org.eclipse.equinox.launcher"); //$NON-NLS-1$
		if (launcherPlugin != null) {
			BundleDescription bundle = launcherPlugin.getBundleDescription();
			if (bundle != null && !list.contains(bundle)) {
				list.add(bundle);
				BundleDescription[] fragments = bundle.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					if (!list.contains(fragments[i])) {
						list.add(fragments[i]);
					}
				}
			}
		}
		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
	}

}
