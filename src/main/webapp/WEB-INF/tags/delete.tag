<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Delete button (POST + CSRF) with a confirmation dialog." %>
<%@ attribute name="action" required="true" %>
<%@ attribute name="what" required="true" %>
<%@ attribute name="button" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<form method="post" action="${ctx}${action}" data-confirm="Are you sure you want to delete this ${h:e(what)}? This cannot be undone.">
    <input type="hidden" name="_csrf" value="${csrf}">
    <c:choose>
        <c:when test="${button}"><button type="submit" class="btn btn-danger">${h:icon('trash')} Delete</button></c:when>
        <c:otherwise><button type="submit" class="icon-btn danger" data-tip="Delete" aria-label="Delete ${h:e(what)}">${h:icon('trash')}</button></c:otherwise>
    </c:choose>
</form>
