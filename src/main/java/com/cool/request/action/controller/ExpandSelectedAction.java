package com.cool.request.action.controller;

import com.cool.request.common.icons.CoolRequestIcons;
import com.cool.request.utils.ResourceBundleUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.List;

public class ExpandSelectedAction extends AnAction {
    private JTree tree;

    public ExpandSelectedAction(JTree tree) {
        getTemplatePresentation().setText(ResourceBundleUtils.getString("expand"));
        getTemplatePresentation().setIcon(CoolRequestIcons.EXPANDALL);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TreePath> treePaths = TreeUtil.collectSelectedPaths(tree);
        for (TreePath treePath : treePaths) {
            expandPath(treePath);

        }
    }

    private void expandPath(TreePath treePath) {
        TreeNode node = (TreeNode) treePath.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<? extends TreeNode> children = node.children(); children.hasMoreElements(); ) {
                TreeNode n = (TreeNode) children.nextElement();
                TreePath path = treePath.pathByAddingChild(n);
                expandPath(path);
            }
        }
        tree.expandPath(treePath);
    }

}
