<%@ page session="false" import="

com.psddev.cms.db.AbVariation,
com.psddev.cms.db.AbVariationField,
com.psddev.cms.db.AbVariationObject,
com.psddev.cms.db.Content,
com.psddev.cms.db.ContentField,
com.psddev.cms.db.ContentType,
com.psddev.cms.db.GuideType,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Modification,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.RoutingFilter,
com.psddev.dari.util.StringUtils,

java.io.IOException,
java.io.Writer,
java.net.MalformedURLException,
java.net.URL,
java.util.ArrayList,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID,

javax.servlet.ServletException
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);
State state = State.getInstance(request.getAttribute("object"));
State originalState = State.getInstance(request.getAttribute("original"));
ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
ToolUi ui = field.as(ToolUi.class);
ObjectType type = field.getParentType();

boolean isHidden = ui.isHidden();

if (!isHidden &&
        !wp.param(boolean.class, "deprecated") &&
        field.isDeprecated() &&
        ObjectUtils.isBlank(state.get(field.getInternalName()))) {
    isHidden = true;
}

if (!isHidden && type != null) {
    isHidden = !wp.hasPermission("type/" + field.getParentType().getId() + "/read")
            || !wp.hasPermission("type/" + field.getParentType().getId() + "/field/" + fieldName + "/read");
}
if (isHidden) {
    return;
}

boolean isFormPost = (Boolean) request.getAttribute("isFormPost");
boolean isReadOnly = ui.isReadOnly();
if (!isReadOnly && type != null) {
    isReadOnly = !wp.hasPermission("type/" + type.getId() + "/write")
            || !wp.hasPermission("type/" + type.getId() + "/field/" + fieldName + "/write");
}
if (isFormPost && isReadOnly) {
    return;
}

// Wrapped in try/finally because return is used for flow control.
boolean abTesting = wp.param(boolean.class, "ab");
String fieldPrefix = (String) request.getAttribute("fieldPrefix");

if (fieldPrefix == null) {
    fieldPrefix = "";
}

try {
    request.setAttribute("fieldPrefix", fieldPrefix + fieldName + "/");

    // Standard header.
    if (!isFormPost) {
        wp.write("<div class=\"inputContainer");
        if (isReadOnly) {
            wp.write(" inputContainer-readOnly");
        }

        if (ui.isBulkUpload()) {
            wp.write(" inputContainer-bulkUpload");
        }

        if (ui.isExpanded()) {
            wp.write(" inputContainer-expanded");
        }

        String cssClass = ui.getCssClass();

        if (!ObjectUtils.isBlank(cssClass)) {
            wp.write(" ");
            wp.write(cssClass);
        }

        wp.write("\" data-field=\"");
        wp.write(wp.h(fieldPrefix + fieldName));
        wp.write("\" data-name=\"");
        wp.write(wp.h(state.getId()));
        wp.write("/");
        wp.write(wp.h(fieldName));
        wp.write("\" data-standard-image-sizes=\"");
        for (String size : ui.getStandardImageSizes()) {
            wp.write(" ");
            wp.write(wp.h(size));
        }

        String tab = null;
        String label = null;
        ContentType ct = type != null ? Query.from(ContentType.class).where("internalName = ?", type.getInternalName()).first() : null;

        if (ct != null) {
            for (ContentField cf : ct.getFields()) {
                if (fieldName.equals(cf.getInternalName())) {
                    tab = cf.getTab();
                    label = cf.getDisplayName();
                    break;
                }
            }

        } else {
            tab = ui.getTab();
            label = field.getLabel();
        }

        wp.write("\" data-tab=\"");
        wp.writeHtml(ObjectUtils.isBlank(tab) ? "Main" : tab);
        wp.write("\">");

        String heading = ui.getHeading();

        if (!ObjectUtils.isBlank(heading)) {
            wp.write("<h2 style=\"margin-top: 20px;\">");
            wp.writeHtml(heading);
            wp.write("</h2>");
        }

        wp.write("<div class=\"inputLabel\">");
        wp.write("<a class=\"icon icon-object-guide\" tabindex=\"-1\" target=\"guideField\" href=\"", wp.cmsUrl("/guideField", "typeId", state.getType().getId(), "field", field.getInternalName()), "\">Guide</a>");
        wp.write("<label for=\"", wp.createId(), "\">");
        wp.write(wp.h(label));
        wp.write("</label></div>");

        // Field-specific error messages.
        List<String> errors = state.getErrors(field);
        if (originalState != null && ObjectUtils.isBlank(errors)) {
            errors = originalState.getErrors(field);
        }
        if (!ObjectUtils.isBlank(errors)) {
            wp.write("<div class=\"message message-error\">");
            for (String error : errors) {
                wp.write(wp.h(error), " ");
            }
            wp.write("</div>");
        }

        // Write out a helpful note if available.
        String noteHtml = ui.getEffectiveNoteHtml(request.getAttribute("object"));
        if (!ObjectUtils.isBlank(noteHtml)) {
            wp.write("<small class=\"inputNote\">");
            wp.write(noteHtml);
            wp.write("</small>");
        }
    }

    Map<String, AbVariationField> variantFields = state.as(AbVariationObject.class).getFields();
    AbVariationField variantField = variantFields.get(fieldName);

    if (variantField == null) {
        variantField = new AbVariationField();

        variantFields.put(fieldName, variantField);
    }

    List<AbVariation> variants = variantField.getVariations();
    String variantIdParam = state.getId() + "/" + fieldName + ".variantId";
    String actionAddVariationParam = "action-add-variant-" + state.getId() + "-" + fieldName;

    ADD_VARIANT: for (UUID id : wp.params(UUID.class, variantIdParam)) {
        for (AbVariation variant : variantField.getVariations()) {
            if (variant.getId().equals(id)) {
                continue ADD_VARIANT;
            }
        }

        AbVariation variant = new AbVariation();

        variant.getState().setId(id);
        variants.add(variant);
    }

    if (wp.param(String.class, actionAddVariationParam) != null) {
        if (variants.isEmpty()) {
            AbVariation variant = new AbVariation();

            variant.setWeight(50.0);
            variant.setValue(state.get(fieldName));
            variants.add(variant);
        }

        AbVariation variant = new AbVariation();

        variant.setWeight(50.0);
        variant.setValue(state.get(fieldName));
        variants.add(variant);
    }

    if (wp.param(boolean.class, "ab") || !variants.isEmpty()) {
        wp.writeStart("div", "class", "inputVariationControls");
            wp.writeStart("button",
                    "class", "icon icon-action-add link",
                    "name", actionAddVariationParam,
                    "value", true);
                wp.writeHtml("Add ");
                wp.writeHtml(field.getDisplayName());
                wp.writeHtml(" Variation");
            wp.writeEnd();

            wp.writeStart("select");
                wp.writeStart("option");
                    wp.writeHtml("Goal: Clicks To This Content");
                wp.writeEnd();

                wp.writeStart("option");
                    wp.writeHtml("Goal: Clicks From This Content");
                wp.writeEnd();
            wp.writeEnd();
        wp.writeEnd();
    }

    if (variants.isEmpty()) {
        writeInput(wp, application, request, response, out, state, field);

    } else {
        UUID originalId = state.getId();

        for (int i = 0, size = variants.size(); i < size; ++ i) {
            AbVariation variant = variants.get(i);
            UUID variantId = variant.getId();
            String weightName = variantId + "/weight";

            state.setId(variantId);
            state.put(fieldName, variant.getValue());

            wp.writeStart("div", "class", "inputVariation");
                wp.writeStart("div", "class", "inputVariationLabel");
                    wp.writeHtml((char) ('A' + i));

                    wp.writeElement("input",
                            "type", "hidden",
                            "name", variantIdParam,
                            "value", variantId);

                    wp.writeElement("input",
                            "type", "range",
                            "id", wp.createId(),
                            "name", weightName,
                            "value", variant.getWeight(),
                            "min", 0,
                            "max", 100,
                            "step", 1);

                    wp.writeStart("output",
                            "for", wp.getId());
                    wp.writeEnd();
                wp.writeEnd();

                request.setAttribute("inputName", variantId + "/" + fieldName);
                writeInput(wp, application, request, response, out, state, field);

                if (isFormPost) {
                    variant.setWeight(wp.param(double.class, weightName));
                    variant.setValue(state.get(fieldName));
                }

                wp.writeStart("div", "class", "inputVariationConversion");
                    wp.writeHtml(String.format("%.1f%%", Math.random() * 10));
                wp.writeEnd();
            wp.writeEnd();
        }

        state.setId(originalId);
        state.put(fieldName, variants.get(0).getValue());
    }

    if (variants.isEmpty()) {
        variantFields.remove(fieldName);
    }

} finally {
    request.setAttribute("fieldPrefix", fieldPrefix);

    // Standard footer.
    if (!isFormPost) {
        wp.write("</div>");
    }
}
%><%!

public static URL getResource(ServletContext context, HttpServletRequest request, String path) throws MalformedURLException {
    if (Boolean.TRUE.equals(request.getAttribute("resourceChecked." + path))) {
        return (URL) request.getAttribute("resource." + path);
    }

    URL resource = context.getResource(path);

    request.setAttribute("resourceChecked." + path, Boolean.TRUE);
    request.setAttribute("resource." + path, resource);
    return resource;
}

public static void writeInput(
        ToolPageContext wp,
        ServletContext application,
        HttpServletRequest request,
        HttpServletResponse response,
        Writer out,
        State state,
        ObjectField field)
        throws IOException, ServletException {

    String fieldName = field.getInternalName();
    ToolUi ui = field.as(ToolUi.class);
    String processorPath = ui.getInputProcessorPath();
    if (processorPath != null) {
        JspUtils.include(request, response, out,
                RoutingFilter.Static.getApplicationPath(ui.getInputProcessorApplication()) +
                StringUtils.ensureStart(processorPath, "/"));
        return;
    }

    // Look for class/field-specific handler.
    // TODO - There should be some type of a hook for external plugins.
    String prefix = wp.cmsUrl("/WEB-INF/field/");
    String path = prefix + field.getJavaDeclaringClassName() + "." + fieldName + ".jsp";
    if (getResource(application, request, path) != null) {
        JspUtils.include(request, response, out, path);
        return;
    }

    // Look for most specific field type handler first.
    // For example, given list/map/any, following JSPs are examined:
    // - list/map/any.jsp
    // - list/map.jsp
    // - list.jsp
    // - default.jsp
    String displayType = ToolUi.getFieldDisplayType(field);
    if (ObjectUtils.isBlank(displayType)) {
        displayType = field.getInternalType();
    }
    while (true) {

        path = prefix + displayType + ".jsp";
        if (getResource(application, request, path) != null) {
            JspUtils.include(request, response, out, path);
            return;
        }

        int slashAt = displayType.lastIndexOf("/");
        if (slashAt < 0) {
            break;
        } else {
            displayType = displayType.substring(0, slashAt);
        }
    }

    JspUtils.include(request, response, out, prefix + "default.jsp");
}
%>
