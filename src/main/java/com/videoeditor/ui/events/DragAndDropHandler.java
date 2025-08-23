package com.videoeditor.ui.events;

import com.videoeditor.model.transfer.FileTransferable; // Use standalone class
import com.videoeditor.ui.panels.TrackPanel;
import com.videoeditor.ui.panels.MainView;
import com.videoeditor.config.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles drag-and-drop functionality for dragging media files from the media pool
 * to the timeline in the video editor UI.
 */
public class DragAndDropHandler {
    private static final Logger logger = Logger.getLogger(DragAndDropHandler.class.getName());
    private final Component dropTarget;
    private final DropCallback dropCallback;

    public interface DropCallback {
        void onDrop(File file, double dropPosition);
    }

    public DragAndDropHandler(Component dropTarget, DropCallback dropCallback) {
        this.dropTarget = dropTarget;
        this.dropCallback = dropCallback;
        setupDropTarget();
    }

    private void setupDropTarget() {
        new DropTarget(dropTarget, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            File file = files.get(0);
                            Point dropPoint = dtde.getLocation();
                            dropCallback.onDrop(file, ((TrackPanel) dropTarget).pixelToTime(dropPoint.x));
                        }
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    logger.warning("Drop failed: " + e.getMessage());
                    dtde.rejectDrop();
                    ((MainView) SwingUtilities.getAncestorOfClass(MainView.class, dropTarget))
                            .showNotification("Error: Drop failed", String.valueOf(UIConstants.ACCENT_ERROR));
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // Use UIUtils for creating border (moved to ui/utils/)
                dropTarget.getParent().repaint();
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                // Use UIUtils for creating border (moved to ui/utils/)
                dropTarget.getParent().repaint();
            }
        });
    }

    public static void enableDrag(Component source, File file) {
        new DragSource().createDefaultDragGestureRecognizer(source, DnDConstants.ACTION_COPY,
                new DragGestureListener() {
                    @Override
                    public void dragGestureRecognized(DragGestureEvent dge) {
                        if (file == null) {
                            logger.warning("No file associated with drag source");
                            return;
                        }
                        // Use standalone FileTransferable class
                        Transferable transferable = new FileTransferable(file);
                        dge.startDrag(DragSource.DefaultCopyDrop, transferable, new DragSourceAdapter() {
                            @Override
                            public void dragDropEnd(DragSourceDropEvent dsde) {
                                if (dsde.getDropSuccess()) {
                                    logger.info("Drag-and-drop successful for file: " + file.getAbsolutePath());
                                } else {
                                    logger.warning("Drag-and-drop failed for file: " + file.getAbsolutePath());
                                }
                            }
                        });
                    }
                });
    }

    // REMOVED: Inner FileTransferable class - use standalone version instead
}
