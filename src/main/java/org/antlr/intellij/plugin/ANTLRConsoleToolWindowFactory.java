package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.jetbrains.annotations.NotNull;

public class ANTLRConsoleToolWindowFactory implements ToolWindowFactory {
    public static final Logger LOG = Logger.getInstance(ANTLRConsoleToolWindowFactory.class);
    
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var controller = ANTLRv4PluginController.getInstance(project);
        
        if (controller == null) {
            LOG.warn("Cannot create ANTLR console tool window - controller is null!");
            return;
        }
        
        var factory = TextConsoleBuilderFactory.getInstance();
        var consoleBuilder = factory.createBuilder(project);
        var console = consoleBuilder.getConsole();
        controller.setConsole(console);
        
        var component = console.getComponent();
        var contentFactory = ContentFactory.getInstance();
        var content = contentFactory.createContent(component, "ANTLR Console", false);
        content.setCloseable(false);
        
        toolWindow.getContentManager().addContent(content);
        toolWindow.setIcon(ANTLRv4Icons.getToolWindowConsole());
        
        // log version info and startup
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(ANTLRv4PluginController.PLUGIN_ID));
        var version = plugin != null
            ? plugin.getName() + " v" + plugin.getVersion() + ", ANTLR Runtime: v" + RuntimeMetaData.VERSION
            : "no plugin-descriptor found";
        
        controller.printToConsole(version);
        controller.printToConsole("Project: " + project.getName());
        controller.printToConsole("-----------------------------------------------------");
    }
}
