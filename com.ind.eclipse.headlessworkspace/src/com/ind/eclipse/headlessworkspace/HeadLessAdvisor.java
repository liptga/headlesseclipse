package com.ind.eclipse.headlessworkspace;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;

public class HeadLessAdvisor extends WorkbenchAdvisor {
    private final String[] args;
    private final IProgressMonitor monitor;

    public HeadLessAdvisor(final String[] args, final IProgressMonitor monitor) {
        this.args = args;
        this.monitor = monitor;
    }

    @Override
    public String getInitialWindowPerspectiveId() {
        return null;
    }

    @Override
    public boolean openWindows() {
        return true;
    }

    @Override
    public void postStartup() {
        super.postStartup();

        try {
            HeadLessServerCreator.getInstance().startServer(monitor);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        SysOutProgressMonitor.out.println("POSTSTARTUP");
        PlatformUI.getWorkbench().close();
        SysOutProgressMonitor.out.println("WORKBENCH CLOSED");
    }

    @Override
    public boolean preShutdown() {
        SysOutProgressMonitor.out.println("PRESHUTDONW");
        return super.preShutdown();
    }

    @Override
    public void postShutdown() {
        SysOutProgressMonitor.out.println("POSTSHUTDOWN");
        super.postShutdown();
    }

    @Override
    @SuppressWarnings( { "unchecked" })
    public void preStartup() {
        try {
            final String debugpluginid = "org.eclipse.debug.core";
            final String debugpluginuiid = "org.eclipse.debug.ui";
            final String statushandlersname = "statusHandlers";
            final IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

            final IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(debugpluginid,
                    statushandlersname);
            final IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
            for (int i = 0; i < infos.length; i++) {
                final IConfigurationElement configurationElement = infos[i];
                final String id = configurationElement.getAttribute("plugin"); //$NON-NLS-1$
                final String code = configurationElement.getAttribute("code"); //$NON-NLS-1$

                if ("200".equals(code) && !id.equals(Activator.PLUGIN_ID)) {
                    final Field mastertokenfield = extensionRegistry.getClass().getDeclaredField("masterToken");
                    mastertokenfield.setAccessible(true);
                    final Object token = mastertokenfield.get(extensionRegistry);
                    final String[] attributeNames = configurationElement.getAttributeNames();
                    for (final String attribute : attributeNames) {
                        System.out.println(attribute + " " + configurationElement.getAttribute(attribute));
                    }
                    extensionRegistry.removeExtension(configurationElement.getDeclaringExtension(), token);
                }
            }

            if (args.length > 2 && args[0].equals("createserver")) {
                HeadLessServerCreator.getInstance().createServer(args[1], args[2], monitor);

            } else if (args.length == 0) {
                printCommandLineParameters();
                return;
            } else if (args.length == 1 && args[0].equals("dumpreferences")) {
                final HeadLessBuilder builder = HeadLessBuilder.getInstance();
                builder.dumpProjectReferences(ResourcesPlugin.getWorkspace().computeProjectOrder(
                        ResourcesPlugin.getWorkspace().getRoot().getProjects()).projects);
            } else {
                final IWorkspaceDescription wsd = ResourcesPlugin.getWorkspace().getDescription();
                wsd.setAutoBuilding(false);
                ResourcesPlugin.getWorkspace().setDescription(wsd);
                ResourcesPlugin.getWorkspace().save(true, null);
                SysOutProgressMonitor.out.println();
                SysOutProgressMonitor.out.println("Auto building is set to false");
                SysOutProgressMonitor.out.println();

                final List list = Arrays.asList(args);

                final File logFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString(),
                        ".build.log");
                logFile.delete();

                final BuildLogReader blr = new BuildLogReader(logFile);
                blr.start();

                final HeadLessBuilder builder = HeadLessBuilder.getInstance();
                Map changes = null;
                if (list.contains("import")) {
                    builder.importProjects(monitor);
                }

                if (list.contains("clean") || list.contains("build")) {
                    changes = builder.modifyExternalToolBuilders(monitor, logFile);
                }

                if (list.contains("clean")) {
                    builder.cleanProjects(monitor);
                }

                if (list.contains("build")) {
                    builder.buildWorkspace(monitor);
                }

                if (changes != null) {
                    builder.revertExternalToolBuilders(changes);
                }

                if (list.contains("exportwars")) {
                    HeadLessWarExporter.getInstance().exportWars(monitor);
                }

                if (list.contains("exportears")) {
                    HeadLessEarExporter.getInstance().exportEars(monitor);
                }

                if (list.contains("exportjars")) {
                    HeadLessJarExporter.getInstance().exportJars(monitor);
                }

                if (list.contains("exportplugins")) {
                    HeadLessPluginExporter.getInstance().exportPlugins(monitor);
                }

                if (list.contains("exportfeatures")) {
                    HeadLessFeatureExporter.getInstance().exportFeatures(monitor);
                }

                if (list.contains("exportproducts")) {
                    HeadLessProductExporter.getInstance().exportProducts(monitor);
                }

                if (list.contains("exportupdatesites")) {
                    HeadLessUpdateSiteExporter.getInstance().exportUpdateSites(monitor);
                }

                if (list.contains("dumpclasspath")) {
                    builder.dumpClassPath();
                }

                // wait ant log to be written out
                Thread.sleep(2000);

                blr.interrupt();
                blr.join();

                logFile.delete();
                logFile.deleteOnExit();
            }

            ResourcesPlugin.getWorkspace().save(true, monitor);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the command line parameters.
     */
    private void printCommandLineParameters() {
        SysOutProgressMonitor.out.println();
        SysOutProgressMonitor.out.println("Possible parameters: import clean build exportwars dumpclasspath");
        SysOutProgressMonitor.out.println("OR: dumpreferences");
        SysOutProgressMonitor.out
                .println("OR to create a server, call this way: createserver <server name> <server properties file path>");
        SysOutProgressMonitor.out.println();
        SysOutProgressMonitor.out
                .println("import: imports all directory containing .project file to the workspace.\r\n\tThe .svn directories will be marked as 'team private'.\r\n\tIt will also set the name of the default JRE to 'JRE'.");
        SysOutProgressMonitor.out.println("clean: calls the clean build on every project.");
        SysOutProgressMonitor.out
                .println("build: builds all projects using incremental building.\r\n\tAfter a clean, or for the first time, a full build will be run.\r\n\tIt will change the external tools builder to log output to a file. At the end, it will restore the original state.");
        SysOutProgressMonitor.out
                .println("exportwars: this will export all dynamic web projects to a war file in the workspace root directory as <project_name>.war");
        SysOutProgressMonitor.out
                .println("exportears: this will export all enterprise application projects to an ear file in the workspace root directory as <project_name>.ear");
        SysOutProgressMonitor.out
                .println("exportjars: this will search all *.jardesc files, and runs them to export jar files");
        SysOutProgressMonitor.out
                .println("exportplugins: exports all plugins to the 'exportedplugins.zip' file in the workspace root directory");
        SysOutProgressMonitor.out
                .println("exportfeatures: exports all features to the 'exportedfeatures.zip' file in the workspace root directory");
        SysOutProgressMonitor.out.println("exportupdatesites: exports all update sites");
        SysOutProgressMonitor.out
                .println("dumpclasspath: the resolved classpath entries for every java project will be dumped to the workspace root directory as <project_name>.classpath");
        SysOutProgressMonitor.out
                .println("dumpreferences: dumps the project build order, listing project static and dynamic references.");
        SysOutProgressMonitor.out
                .println("createserver: creates a server instance of <server name>.\r\n\tThe properties file should contain the server specific parameters. It will print out what is missing...");
        SysOutProgressMonitor.out.println();
    }
}