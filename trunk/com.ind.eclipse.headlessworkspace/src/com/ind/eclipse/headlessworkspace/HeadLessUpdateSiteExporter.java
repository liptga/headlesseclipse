package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;

/**
 * This class searches for update site project in the workspace and exports
 * them.
 * 
 * @author lex.schade@googlemail.com
 */
@SuppressWarnings("restriction")
public class HeadLessUpdateSiteExporter {

    private static final String MSG_NO_UPDATE_SITES_FOUND = "No update sites found in workspace {0}.";
    private static final String SITE_XML_FILENAME = "site.xml";
    private static final String JOB_NAME = "Update site export job";

    /** The shared instance of the exporter. */
    private static HeadLessUpdateSiteExporter instance = new HeadLessUpdateSiteExporter();

    /**
     * Returns the single instance of the update site exporter.
     * 
     * @return the instance
     */
    public static HeadLessUpdateSiteExporter getInstance() {
        return instance;
    }

    private HeadLessUpdateSiteExporter() {
    }

    /**
     * Exports the update site.
     * 
     * @param monitor
     *            the progress monitor to use
     * @throws Exception
     */
    void exportUpdateSites(final IProgressMonitor monitor) throws Exception {

        ISiteModel[] updateSites = getSiteModels();
        if (updateSites.length > 0) {

            // iterate through all available sites
            for (ISiteModel updateSiteModel : getSiteModels()) {

                // set the target, if you need update sites for different target
                // platforms
                // IFile targetFile =
                // updateSiteModel.getUnderlyingResource().getProject().getFile("site.target");
                // TargetPlatformUtils.loadTargetDefinition(targetFile);
                
                // cleanup
                cleanUpdateSiteFolder(updateSiteModel.getInstallLocation());
                
                SysOutProgressMonitor.out.println("Exporting update site: " + updateSiteModel.getInstallLocation());

                // build the update site
                final HeadlessSiteBuildOperation useo = new HeadlessSiteBuildOperation(getFeatures(updateSiteModel), updateSiteModel,
                        JOB_NAME);
               
                SysOutProgressMonitor.out.println("Update site build operation started."); 
                useo.run(monitor);         
                SysOutProgressMonitor.out.println("Update site build operation finished.");
            }
        } else {
            SysOutProgressMonitor.out.println(MessageFormat.format(MSG_NO_UPDATE_SITES_FOUND, ResourcesPlugin
                    .getWorkspace().getRoot().getLocation()));
        }
    }

    /**
     * Returns the features contained in the update site project.
     * 
     * @param updateSiteModel
     *            the update site model
     * @return the features or an empty array if not features can be found
     */
    public IFeatureModel[] getFeatures(ISiteModel updateSiteModel) {
        final List<IFeatureModel> models = new ArrayList<IFeatureModel>();
        final FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
        for (ISiteFeature siteFeature : updateSiteModel.getSite().getFeatures()) {
            final IFeatureModel model = manager.findFeatureModel(siteFeature.getId());
            if (model != null) {
                models.add(model);
            }
        }
        return models.toArray(new IFeatureModel[models.size()]);
    }

    /**
     * Returns all existing update site models from the workspace.
     * 
     * @return the update site models or an empty array if no update site exists
     */
    public ISiteModel[] getSiteModels() {
        final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        final List<ISiteModel> siteModels = new ArrayList<ISiteModel>();

        for (IProject project : projects) {
            IFile siteXml = project.getFile(SITE_XML_FILENAME);
            if (siteXml != null && siteXml.exists()) {
                final ISiteModel model = new WorkspaceSiteModel(siteXml);
                if (model != null) {
                    siteModels.add(model);
                    try {
                        model.load();
                    } catch (CoreException e) {
                        SysOutProgressMonitor.out.println("Problem occurred while loading: " + siteXml.getLocation());
                        e.printStackTrace(SysOutProgressMonitor.out);
                    }
                }
            }
        }
        return siteModels.toArray(new ISiteModel[siteModels.size()]);
    }
    
    /**
     * Deletes existing old files from the update site project folder.
     * @param updateSiteLocation 
     * 
     */
    private void cleanUpdateSiteFolder(String updateSiteLocation) {
        File siteFeatures = new File(updateSiteLocation, "plugins");
        deleteDirectory(siteFeatures);
        
        File sitePlugins = new File(updateSiteLocation, "features");
        deleteDirectory(sitePlugins);
    }
    
    /**
     * Removes the given directory recursive.
     * 
     * @param directory The directory to remove.
     * @return True, if the directory was removed.
     */
    private static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }

}
