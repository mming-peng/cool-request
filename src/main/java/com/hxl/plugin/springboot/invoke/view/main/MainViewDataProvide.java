package com.hxl.plugin.springboot.invoke.view.main;

import com.hxl.plugin.springboot.invoke.bean.RequestEnvironment;
import org.jetbrains.annotations.NotNull;

public interface MainViewDataProvide {
    @NotNull
    public RequestEnvironment getSelectRequestEnvironment();

}