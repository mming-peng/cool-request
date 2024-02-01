package com.cool.request.component.http.net;

import com.cool.request.lib.springmvc.RequestParameterDescription;

import java.util.Objects;

public class FormDataInfo extends RequestParameterDescription implements Cloneable {
    private String value;

    public FormDataInfo(String name, String value, String type) {
        super(name, type, "");
        this.value = value;
    }

    public FormDataInfo() {
        super("", "", "");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FormDataInfo that = (FormDataInfo) object;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public FormDataInfo clone() {
        return new FormDataInfo(getName(), getValue(), getType());
    }
}
