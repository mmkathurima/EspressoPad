package com.github.espressopad;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;

/**
 * @author Alexander Bolte - Bolte Consulting (2010 - 2014).
 * <p>
 * This class shall be a simple implementation of a TreeItem for
 * displaying a file system tree.
 * <p>
 * The idea for this class is taken from the Oracle API docs found
 * <a href="http://docs.oracle.com/javafx/2/api/javafx/scene/control/TreeItem.html">here</a>.
 * <p>
 * Basically the file sytsem will only be inspected once. If it changes
 * during runtime the whole tree would have to be rebuild. Event
 * handling is not provided in this implementation.
 */
public class FilePathTreeItem extends TreeItem<File> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;

    /**
     * Calling the constructor of super class in oder to create a new
     * TreeItem<File>.
     *
     * @param f an object of type File from which a tree should be build or
     *          which children should be gotten.
     */
    public FilePathTreeItem(File f) {
        super(f);
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.TreeItem#getChildren()
     */
    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (this.isFirstTimeChildren) {
            this.isFirstTimeChildren = false;

            /*
             * First getChildren() call, so we actually go off and determine the
             * children of the File contained in this TreeItem.
             */
            super.getChildren().setAll(this.buildChildren(this));
        }
        return super.getChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see javafx.scene.control.TreeItem#isLeaf()
     */
    @Override
    public boolean isLeaf() {
        if (this.isFirstTimeLeaf) {
            this.isFirstTimeLeaf = false;
            File f = getValue();
            this.isLeaf = f.isFile();
        }
        return this.isLeaf;
    }

    /**
     * Returning a collection of type ObservableList containing TreeItems, which
     * represent all children available in handed TreeItem.
     *
     * @param TreeItem the root node from which children a collection of TreeItem
     *                 should be created.
     * @return an ObservableList<TreeItem<File>> containing TreeItems, which
     * represent all children available in handed TreeItem. If the
     * handed TreeItem is a leaf, an empty list is returned.
     */
    private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File f = TreeItem.getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                for (File childFile : files)
                    children.add(new FilePathTreeItem(childFile));
                return children;
            }
        }
        return FXCollections.emptyObservableList();
    }
}