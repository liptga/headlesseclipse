package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
		ResourcesPlugin.getWorkspace().getRoot().accept(new IResourceVisitor() {

			public boolean visit(final IResource res) throws CoreException
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
			}
		});

		for (int i = 0; i < products.size(); i++)
		{
			final IFile product = (IFile) products.get(i);
			final WorkspaceProductModel wpm = new WorkspaceProductModel(product, false);
			wpm.load();
			final IProduct prod = wpm.getProduct();

			final File zipFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), product.getName().substring(0, product.getName().length() - 8) + ".zip");

			SysOutProgressMonitor.out.println("Exporting Eclipse Product using '" + product.getLocation() + "' as configuration to: " + zipFile);

			final FeatureExportInfo info = new FeatureExportInfo();
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

			final Class productExportOperationClass = Class.forName("org.eclipse.pde.internal.core.exports.ProductExportOperation");
			final String jobName = "Product export job";
			final List<Object> arguments = new ArrayList();
			Constructor constructor = null;

			//first we look for single argument constructor, which is present in eclipse 3.4
			try
			{
				arguments.add(info);
				constructor = productExportOperationClass.getConstructor(info.getClass(), IProduct.class, String.class);
			}
			catch (final NoSuchMethodException nsme)
			{
				arguments.add(jobName);
				constructor = productExportOperationClass.getConstructor(info.getClass(), String.class, IProduct.class, String.class);
			}
			arguments.add(prod);
			arguments.add(product.getName().substring(0, product.getName().length() - 8));

			final Object peo = constructor.newInstance(arguments.toArray());
			Method runMethod = null;
			Class currentClass = productExportOperationClass;
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
						throw new Exception("run(IProgressMonitor) not found in any of the ancestors of " + productExportOperationClass.getName());
				}
			}
			runMethod.setAccessible(true);
			runMethod.invoke(peo, monitor);
			SysOutProgressMonitor.out.println();
		}
	}

	@SuppressWarnings("unchecked")
	private IFeatureModel[] getFeatures(final IProduct prod)
	{
		final ArrayList list = new ArrayList();
		final FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		final IProductFeature[] features = prod.getFeatures();
		for (int i = 0; i < features.length; i++)
		{
			final IFeatureModel model = manager.findFeatureModel(features[i].getId(), features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	@SuppressWarnings("unchecked")
	private BundleDescription[] getPlugins(final IProduct prod)
	{
		final ArrayList list = new ArrayList();
		final State state = TargetPlatformHelper.getState();
		final IProductPlugin[] plugins = prod.getPlugins();
		for (int i = 0; i < plugins.length; i++)
		{
			final BundleDescription bundle = state.getBundle(plugins[i].getId(), null);
			if (bundle != null)
				list.add(bundle);
		}
		// implicitly add the new launcher plug-in/fragment if we are to use the
		// new launching story and the launcher plug-in/fragment are not already included in the .product file
		final IPluginModelBase launcherPlugin = PluginRegistry.findModel("org.eclipse.equinox.launcher"); //$NON-NLS-1$
		if (launcherPlugin != null)
		{
			final BundleDescription bundle = launcherPlugin.getBundleDescription();
			if (bundle != null && !list.contains(bundle))
			{
				list.add(bundle);
				final BundleDescription[] fragments = bundle.getFragments();
				for (int i = 0; i < fragments.length; i++)
				{
					if (!list.contains(fragments[i]))
					{
						list.add(fragments[i]);
					}
				}
			}
		}
		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
	}

}
