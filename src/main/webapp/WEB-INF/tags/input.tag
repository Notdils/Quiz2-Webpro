<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Labelled input with its validation error." %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="value" type="java.lang.Object" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="hint" %>
<%@ attribute name="cls" %>
<%@ attribute name="step" %>
<%@ attribute name="min" %>
<%@ attribute name="max" %>
<%@ attribute name="maxlength" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="autocomplete" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<c:set var="error" value="${requestScope.errors[name]}"/>
<div class="field ${cls} ${not empty error ? 'has-error' : ''}">
    <label for="f-${name}">${h:e(label)}<c:if test="${required}"><span class="req" aria-hidden="true">*</span></c:if></label>
    <input id="f-${name}" name="${name}" type="${empty type ? 'text' : type}"
           <c:if test="${type != 'password'}">value="${h:e(value)}"</c:if>
           <c:if test="${required}">required</c:if>
           <c:if test="${not empty step}">step="${step}"</c:if>
           <c:if test="${not empty min}">min="${min}"</c:if>
           <c:if test="${not empty max}">max="${max}"</c:if>
           <c:if test="${not empty maxlength}">maxlength="${maxlength}"</c:if>
           <c:if test="${not empty placeholder}">placeholder="${h:e(placeholder)}"</c:if>
           <c:if test="${not empty autocomplete}">autocomplete="${autocomplete}"</c:if>
           <c:if test="${not empty error}">aria-invalid="true" aria-describedby="e-${name}"</c:if>>
    <c:if test="${not empty hint and empty error}"><p class="hint">${h:e(hint)}</p></c:if>
    <c:if test="${not empty error}"><p class="error" id="e-${name}">${h:e(error)}</p></c:if>
</div>
