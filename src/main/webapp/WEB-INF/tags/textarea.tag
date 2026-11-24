<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Labelled textarea with its validation error." %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="placeholder" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<c:set var="error" value="${requestScope.errors[name]}"/>
<div class="field full ${not empty error ? 'has-error' : ''}">
    <label for="f-${name}">${h:e(label)}</label>
    <textarea id="f-${name}" name="${name}" rows="4" placeholder="${h:e(placeholder)}"
              <c:if test="${not empty error}">aria-invalid="true" aria-describedby="e-${name}"</c:if>>${h:e(value)}</textarea>
    <c:if test="${not empty error}"><p class="error" id="e-${name}">${h:e(error)}</p></c:if>
</div>
