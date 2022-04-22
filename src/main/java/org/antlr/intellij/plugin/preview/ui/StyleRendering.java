package org.antlr.intellij.plugin.preview.ui;

import java.awt.*;

/**
 * Rendering interface for styled components.
 */
public interface StyleRendering {


    /**
     * Rendering method to be implemented in subclasses.
     *
     * @param graphics2D Graphics context.
     */
    void render(Graphics2D graphics2D);


    /**
     * Called before content rendering.
     */
    void onBeforeRendering();


    /**
     * Called after content rendering done.
     */
    void onAfterRendering();
}
