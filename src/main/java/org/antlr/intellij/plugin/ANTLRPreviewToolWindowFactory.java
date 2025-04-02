package org.antlr.intellij.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;


public class ANTLRPreviewToolWindowFactory implements ToolWindowFactory {
    public static final Logger LOG = Logger.getInstance(ANTLRPreviewToolWindowFactory.class);
    
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var controller = ANTLRv4PluginController.getInstance(project);
        
        if (controller == null) {
            LOG.warn("Cannot create ANTLR console tool window - controller is null!");
            return;
        }
        
        var previewPanel = controller.getOrCreatePreviewPanel();
        var contentFactory = ContentFactory.getInstance();
        var content = contentFactory.createContent(previewPanel, "", false);
        content.setCloseable(false);
        
        toolWindow.getContentManager().addContent(content);
        toolWindow.setIcon(ANTLRv4Icons.getToolWindowPreview());
    }
}
