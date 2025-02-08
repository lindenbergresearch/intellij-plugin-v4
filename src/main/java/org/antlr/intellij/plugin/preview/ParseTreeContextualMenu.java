package org.antlr.intellij.plugin.preview;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications.Bus;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import org.apache.commons.lang3.StringUtils;
import org.jfree.svg.SVGGraphics2D;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Shows a contextual menu when the user right-clicks on the parse tree preview. The menu contains options
 * to export the parse tree to several image formats.
 */
class ParseTreeContextualMenu {
    
    static void showPopupMenu(UberTreeViewer parseTreeViewer, MouseEvent event) {
        var menu = new JPopupMenu();
        
        menu.add(createExportMenuItem(parseTreeViewer, "Export to image (current background)", false));
        menu.add(createExportMenuItem(parseTreeViewer, "Export to image (transparent background)", true));
        
        menu.show(parseTreeViewer, event.getX(), event.getY());
    }
    
    
    private static JMenuItem createExportMenuItem(UberTreeViewer parseTreeViewer, String label, boolean useTransparentBackground) {
        var item = new JMenuItem(label);
        var isMacNativeSaveDialog = SystemInfo.isMac;
        
        item.addActionListener(event -> {
            var extensions = useTransparentBackground ? new String[]{"png", "svg"} : new String[]{"png", "jpg", "svg"};
            var descriptor = new FileSaverDescriptor("Export Image To", "Choose the destination file", extensions);
            var dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, (Project) null);
            
            var fileName = "parseTree" + (isMacNativeSaveDialog ? ".png" : "");
            var vf = dialog.save((VirtualFile) null, fileName);
            
            if (vf == null) {
                return;
            }
            
            var file = vf.getFile();
            var imageFormat = FileUtilRt.getExtension(file.getName());
            
            if (StringUtils.isBlank(imageFormat)) {
                imageFormat = "png";
            }
            
            if ("svg".equals(imageFormat)) {
                exportToSvg(parseTreeViewer, file, useTransparentBackground);
            } else {
                exportToImage(parseTreeViewer, file, useTransparentBackground, imageFormat);
            }
        });
        
        return item;
    }
    
    
    private static void exportToImage(UberTreeViewer parseTreeViewer, File file, boolean useTransparentBackground, String imageFormat) {
        var imageType = useTransparentBackground ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        var bi = ImageUtil.createImage(parseTreeViewer.getWidth(), parseTreeViewer.getHeight(), imageType);
        var graphics = bi.getGraphics();
        
        if (!useTransparentBackground) {
            graphics.setColor(JBColor.WHITE);
            graphics.fillRect(0, 0, parseTreeViewer.getWidth(), parseTreeViewer.getHeight());
        }
        
        parseTreeViewer.paint(graphics);
        
        try {
            if (!ImageIO.write(bi, imageFormat, file)) {
                var notification = new Notification(
                    "ANTLR 4",
                    "Error while exporting parse tree to file " + file.getAbsolutePath(),
                    "unknown format '" + imageFormat + "'?",
                    NotificationType.WARNING
                );
                Bus.notify(notification);
            }
        } catch (IOException e) {
            Logger.getInstance(ParseTreeContextualMenu.class)
                .error("Error while exporting parse tree to file " + file.getAbsolutePath(), e);
        }
    }
    
    
    private static void exportToSvg(UberTreeViewer parseTreeViewer, File file, boolean useTransparentBackground) {
        var svgGenerator = new SVGGraphics2D(parseTreeViewer.getWidth(), parseTreeViewer.getHeight());
        
        if (!useTransparentBackground) {
            svgGenerator.setColor(JBColor.WHITE);
            svgGenerator.fillRect(0, 0, parseTreeViewer.getWidth(), parseTreeViewer.getHeight());
        }
        parseTreeViewer.paint(svgGenerator);
        
        try (var writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(svgGenerator.getSVGDocument());
        } catch (IOException e) {
            Logger.getInstance(ParseTreeContextualMenu.class)
                .error("Error while exporting parse tree to SVG file " + file.getAbsolutePath(), e);
        }
    }
}
