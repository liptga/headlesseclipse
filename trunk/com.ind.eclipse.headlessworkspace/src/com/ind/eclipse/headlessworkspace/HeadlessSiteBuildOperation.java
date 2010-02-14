/**
 * 
 */
package com.ind.eclipse.headlessworkspace;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.core.exports.FeatureBasedExportOperation;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.exports.SiteBuildOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteModel;

/**
 * Performs a site build operation that will build any features needed by the
 * site and generate p2 metadata for those features.
 * 
 * This class is only needed for making the run method public. Because calling
 * the schedule method won't work in headless and so we don't need reflection.
 * 
 * @see FeatureBasedExportOperation
 * @see FeatureExportOperation
 * 
 * 
 * @author lex.schade@googlemail.com
 */
@SuppressWarnings("restriction")
public class HeadlessSiteBuildOperation extends SiteBuildOperation {

    public HeadlessSiteBuildOperation(IFeatureModel[] features, ISiteModel site, String jobName) {
        super(features, site, jobName);
    }

    /**
     * We need the run method as public, because calling the schedule method
     * won't work in headless. So we don't need reflection.
     */
    @Override
    public IStatus run(IProgressMonitor monitor) {
        return super.run(monitor);
    }
}
