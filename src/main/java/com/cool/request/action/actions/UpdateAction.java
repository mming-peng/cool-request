package com.cool.request.action.actions;

import com.cool.request.icons.MyIcons;
import com.cool.request.view.events.IToolBarViewEvents;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class UpdateAction  extends AnAction {
    private final IToolBarViewEvents iViewEvents;
    public UpdateAction(IToolBarViewEvents iViewEvents) {
        super("Update","Update", MyIcons.UPDATE);
        this.iViewEvents = iViewEvents;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        iViewEvents.pluginHelp();
    }
}
