<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Page title, optional description and back link; the body holds action buttons." %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="description" %>
<%@ attribute name="eyebrow" %>
<%@ attribute name="back" %>
<%@ attribute name="backLabel" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<header class="page-header">
    <div>
        <c:if test="${not empty back}">
            <a class="back" href="${ctx}${back}">${h:icon('back')} ${h:e(backLabel)}</a><br>
        </c:if>
        <c:if test="${not empty eyebrow}"><span class="eyebrow">${h:e(eyebrow)}</span></c:if>
        <h1>${h:e(title)}</h1>
        <c:if test="${not empty description}"><p>${h:e(description)}</p></c:if>
    </div>
    <div class="actions"><jsp:doBody/></div>
</header>
