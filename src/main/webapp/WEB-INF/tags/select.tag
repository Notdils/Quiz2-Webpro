<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Labelled select; the body holds the options." %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="hint" %>
<%@ attribute name="cls" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<c:set var="error" value="${requestScope.errors[name]}"/>
<div class="field ${cls} ${not empty error ? 'has-error' : ''}">
    <label for="f-${name}">${h:e(label)}<c:if test="${required}"><span class="req" aria-hidden="true">*</span></c:if></label>
    <select id="f-${name}" name="${name}" <c:if test="${required}">required</c:if>
            <c:if test="${not empty error}">aria-invalid="true" aria-describedby="e-${name}"</c:if>>
        <c:if test="${not empty placeholder}"><option value="">${h:e(placeholder)}</option></c:if>
        <jsp:doBody/>
    </select>
    <c:if test="${not empty hint and empty error}"><p class="hint">${h:e(hint)}</p></c:if>
    <c:if test="${not empty error}"><p class="error" id="e-${name}">${h:e(error)}</p></c:if>
</div>
