<%@ page import="

com.psddev.cms.db.ReferentialTextMarker,
com.psddev.cms.db.StandardImageSize,
com.psddev.cms.tool.Area,
com.psddev.cms.tool.Tool,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.Widget,

com.psddev.dari.db.Application,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.db.WebResourceOverride,

java.util.Arrays,
java.util.List
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminSettings")) {
    return;
}

Object selected = Query.findById(Object.class, wp.uuidParam("id"));
if (selected == null) {
    if (wp.uuidParam("typeId") != null) {
        selected = wp.findOrReserve(StandardImageSize.class);
    } else {
        selected = wp.getCmsTool();
    }
}

if (selected != null && wp.tryStandardUpdate(selected)) {
    return;
}

State selectedState = State.getInstance(selected);
List<StandardImageSize> standardImageSizes = Query.from(StandardImageSize.class).sortAscending("displayName").select();

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">

        <div class="widget">
            <h1 class="icon icon-cogs">Settings</h1>

            <h2>Applications</h2>
            <ul class="links">
                <% for (Object app : Query
                        .from(Application.class)
                        .sortAscending("name")
                        .select()) { %>
                    <li<%= app.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, app) %>"><%= wp.objectLabel(app) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>Standard Image Sizes</h2>
            <ul class="links">
                <li class="new<%= selected instanceof StandardImageSize && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, StandardImageSize.class) %>">New Standard Image Size</a>
                </li>
                <% for (StandardImageSize size : standardImageSizes) { %>
                    <li<%= size.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, size) %>"><%= wp.objectLabel(size) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>Referential Text Markers</h2>
            <ul class="links">
                <li class="new<%= selected instanceof ReferentialTextMarker && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ReferentialTextMarker.class) %>">New Referential Text Marker</a>
                </li>
                <% for (ReferentialTextMarker marker : Query.
                        from(ReferentialTextMarker.class).
                        sortAscending("displayName").
                        select()) { %>
                    <li<%= marker.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, marker) %>"><%= wp.objectLabel(marker) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>Web Resource Overrides</h2>
            <ul class="links">
                <li class="new<%= selected instanceof WebResourceOverride && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, WebResourceOverride.class) %>">New Web Resource Override</a>
                </li>
                <% for (WebResourceOverride override : Query.
                        from(WebResourceOverride.class).
                        sortAscending("path").
                        selectAll()) { %>
                    <li<%= override.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, override) %>"><%= wp.objectLabel(override) %></a>
                    </li>
                <% } %>
            </ul>
        </div>

    </div>
    <div class="main">

        <% if (selected != null) { %>
            <div class="widget">
                <% wp.writeStandardForm(selected); %>
            </div>
        <% } %>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
