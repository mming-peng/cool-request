package com.cool.request.component.http.script;

import com.cool.request.common.constant.CoolRequestIdeaTopic;
import com.cool.request.script.ILog;
import com.intellij.openapi.project.Project;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ScriptSimpleLogImpl extends PrintStream implements ILog {
    private final Project project;
    private final String id;

    public ScriptSimpleLogImpl(Project project, String id) {
        super(new ByteArrayOutputStream());
        this.project = project;
        this.id = id;
    }


    @Override
    public void clearLog() {
        project.getMessageBus().syncPublisher(CoolRequestIdeaTopic.SCRIPT_LOG).clear(id);
    }

    @Override
    public void println(String value) {
        project.getMessageBus().syncPublisher(CoolRequestIdeaTopic.SCRIPT_LOG).log(id, value + "\n");
    }

    @Override
    public void print(String value) {
        project.getMessageBus().syncPublisher(CoolRequestIdeaTopic.SCRIPT_LOG).log(id, value);
    }
}
