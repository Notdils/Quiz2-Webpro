<%@ page isErrorPage="true" %>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="message" value="${status == 403 ? 'You do not have permission to open this page.'
        : status == 404 ? 'The page you are looking for does not exist or was removed.'
        : status == 419 ? 'Your session expired. Please go back, refresh the page, and try again.'
        : 'Something went wrong on our side. Please try again in a moment.'}"/>
<c:choose>
    <c:when test="${not empty user}">
        <ui:layout title="Error ${status}">
            <div class="error-page">
                <div class="grid" style="justify-items:center">
                    <p class="error-code">${status}</p>
                    <p class="muted">${message}</p>
                    <a class="btn btn-primary" href="${ctx}/dashboard">Back to the dashboard</a>
                </div>
            </div>
        </ui:layout>
    </c:when>
    <c:otherwise>
        <ui:auth title="Error ${status}">
            <p class="error-code">${status}</p>
            <p class="muted">${message}</p>
            <a class="btn btn-primary" href="${ctx}/login">Go to the login page</a>
        </ui:auth>
    </c:otherwise>
</c:choose>
