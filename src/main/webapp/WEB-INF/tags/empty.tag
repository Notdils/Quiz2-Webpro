<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Empty state: icon, message, and an optional action in the body." %>
<%@ attribute name="icon" required="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="text" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<div class="empty">
    <span class="empty-icon">${h:icon(icon)}</span>
    <h3>${h:e(title)}</h3>
    <c:if test="${not empty text}"><p>${h:e(text)}</p></c:if>
    <jsp:doBody/>
</div>
