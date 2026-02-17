package com.github.espressopad.views.components;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTree extends JTree {
    private final DefaultTreeModel defaultTreeModel;
    private final Path dir;

    public FileTree(Path dir) {
        this.dir = dir;
        this.defaultTreeModel = new DefaultTreeModel(this.addNodes(null, dir));
        this.setLayout(new BorderLayout());

        // Make a tree list with all the nodes, and make it a JTree
        this.setModel(this.defaultTreeModel);

        // Lastly, put the JTree into a JScrollPane.
        JScrollPane scrollpane = new JScrollPane();
        scrollpane.getViewport().add(this);
        this.setCellRenderer(new FileTreeCellRenderer());
    }

    /**
     * Add nodes from under "dir" into curTop. Highly recursive.
     */
    private DefaultMutableTreeNode addNodes(DefaultMutableTreeNode curTop, Path dir) {
        try (Stream<Path> dirList = Files.list(dir)) {
            Path curPath = dir.getFileName();
            DefaultMutableTreeNode curDir = new DefaultMutableTreeNode(curPath);
            // should only be null at root
            if (curTop != null)
                curTop.add(curDir);
            List<Path> ol = dirList.sorted(Path::compareTo)
                    .collect(Collectors.toList());
            List<Path> files = new ArrayList<>();
            // Make two passes, one for Dirs and one for Files. This is #1.
            for (Path thisObject : ol) {
                Path newPath;
                if (curPath.equals(Path.of(".")))
                    newPath = thisObject;
                else
                    newPath = curPath.resolve(thisObject);
                if (Files.isDirectory(newPath))
                    this.addNodes(curDir, newPath);
                else
                    files.add(thisObject);
            }
            // Pass two: for files.
            for (Path file : files)
                curDir.add(new DefaultMutableTreeNode(file));
            return curDir;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DefaultTreeModel getModel() {
        return this.defaultTreeModel;
    }

    public void refreshTree() {
        this.setModel(null);
        this.setModel(new DefaultTreeModel(this.addNodes(null, this.dir)));
        this.setCellRenderer(new FileTreeCellRenderer());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, 400);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 400);
    }

    private class FileTreeCellRenderer extends DefaultTreeCellRenderer {

        private final FileSystemView fileSystemView;

        private final JLabel label;

        FileTreeCellRenderer() {
            this.label = new JLabel();
            this.label.setOpaque(true);
            this.fileSystemView = FileSystemView.getFileSystemView();
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            Path userObject = (Path) ((DefaultMutableTreeNode) value).getUserObject();
            this.setText(String.valueOf(userObject.getFileName()));
            this.setToolTipText(userObject.toString());
            return this;
        }
    }
}
