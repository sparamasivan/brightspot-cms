package com.psddev.cms.db;

import com.psddev.dari.util.PageContextFilter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

@Rule.DisplayName("Request Param")
public class RequestParamRule extends Rule {
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    
    @Override
    public boolean evaluate(Variation variation, Profile profile, Object object) {
        
        HttpServletRequest request = PageContextFilter.Static.getRequest();
        if (request == null) {
            return false;
        }
        
        return StringUtils.equalsIgnoreCase(request.getParameter(getName()), getValue());
    }

}
