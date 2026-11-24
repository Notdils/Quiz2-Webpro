<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Signed-in page shell: sidebar, mobile top bar, flash toasts." %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="charts" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${h:e(title)} · ${h:e(appName)}</title>
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600&family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="${ctx}/assets/app.css">
</head>
<body>
<div class="app">
    <aside class="sidebar" aria-label="Main navigation">
        <a class="brand" href="${ctx}/dashboard">
            <span class="brand-mark">${h:icon('leaf')}</span>
            <span class="brand-name">${h:e(appName)}</span>
        </a>
        <nav class="nav">
            <a href="${ctx}/dashboard" class="${path == '/dashboard' ? 'active' : ''}">${h:icon('dashboard')} Dashboard</a>
            <a href="${ctx}/trees" class="${fn:startsWith(path, '/trees') ? 'active' : ''}">${h:icon('tree')} Trees</a>
            <a href="${ctx}/care-logs" class="${fn:startsWith(path, '/care-logs') ? 'active' : ''}">${h:icon('sprout')} Care Logs</a>
            <a href="${ctx}/harvests" class="${fn:startsWith(path, '/harvests') ? 'active' : ''}">${h:icon('basket')} Harvests</a>

            <p class="nav-label">Master Data</p>
            <a href="${ctx}/categories" class="${fn:startsWith(path, '/categories') ? 'active' : ''}">${h:icon('tag')} Categories</a>
            <a href="${ctx}/tree-types" class="${fn:startsWith(path, '/tree-types') ? 'active' : ''}">${h:icon('layers')} Tree Types</a>
            <a href="${ctx}/blocks" class="${fn:startsWith(path, '/blocks') ? 'active' : ''}">${h:icon('map')} Blocks</a>

            <c:if test="${isAdmin}">
                <p class="nav-label">Finance</p>
                <a href="${ctx}/expenses" class="${fn:startsWith(path, '/expenses') ? 'active' : ''}">${h:icon('wallet')} Expenses</a>
                <a href="${ctx}/sales" class="${fn:startsWith(path, '/sales') ? 'active' : ''}">${h:icon('cash')} Sales</a>

                <p class="nav-label">Insights</p>
                <a href="${ctx}/reports" class="${fn:startsWith(path, '/reports') ? 'active' : ''}">${h:icon('chart')} Reports</a>
                <a href="${ctx}/users" class="${fn:startsWith(path, '/users') ? 'active' : ''}">${h:icon('users')} Users</a>
            </c:if>
        </nav>

        <div class="sidebar-footer">
            <a class="me" href="${ctx}/profile" title="Profile">
                <span class="avatar">${h:initials(user.name)}</span>
                <span>
                    <span class="me-name">${h:e(user.name)}</span><br>
                    <span class="me-role">${h:label(user.role)}</span>
                </span>
            </a>
            <form method="post" action="${ctx}/logout">
                <input type="hidden" name="_csrf" value="${csrf}">
                <button type="submit" class="btn btn-sm logout">${h:icon('logout')} Log out</button>
            </form>
        </div>
    </aside>
    <div class="scrim"></div>

    <div>
        <header class="topbar">
            <a class="brand" href="${ctx}/dashboard"><span class="brand-mark">${h:icon('leaf')}</span><span class="brand-name">${h:e(appName)}</span></a>
            <button type="button" class="icon-btn" data-nav-toggle aria-label="Open menu">${h:icon('menu')}</button>
        </header>

        <div class="toasts" role="status" aria-live="polite">
            <c:if test="${not empty flash}">
                <div class="toast toast-success">${h:icon('check')}<span>${h:e(flash)}</span><button type="button" data-dismiss aria-label="Dismiss">&times;</button></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="toast toast-error">${h:icon('alert')}<span>${h:e(flashError)}</span><button type="button" data-dismiss aria-label="Dismiss">&times;</button></div>
            </c:if>
        </div>

        <main class="main">
            <div class="content">
                <jsp:doBody/>
            </div>
        </main>
    </div>
</div>
<c:if test="${charts}">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js"></script>
</c:if>
<script src="${ctx}/assets/app.js"></script>
</body>
</html>
