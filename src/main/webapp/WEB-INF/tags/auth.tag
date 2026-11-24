<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Guest page shell: photo panel and a centered form." %>
<%@ attribute name="title" required="true" %>
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
<div class="auth">
    <section class="auth-art">
        <a class="brand" href="${ctx}/login">
            <span class="brand-mark">${h:icon('leaf')}</span>
            <span class="brand-name">${h:e(appName)}</span>
        </a>
        <div>
            <p class="auth-quote">Every tree, every harvest, every rupiah, in one place.</p>
            <p>Record care and harvests from the field, then see what your orchard really earns.</p>
        </div>
    </section>
    <main class="auth-panel">
        <div class="auth-card">
            <jsp:doBody/>
        </div>
    </main>
</div>
<script src="${ctx}/assets/app.js"></script>
</body>
</html>
