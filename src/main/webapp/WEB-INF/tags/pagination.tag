<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Result count and page links for a Query.Page." %>
<%@ attribute name="result" required="true" type="java.lang.Object" %>
<%@ attribute name="noun" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://harvesta.app/functions" %>
<div class="table-foot">
    <span>Showing ${result.from}–${result.to} of ${h:integer(result.total)} ${noun}</span>
    <c:if test="${result.pages > 1}">
        <nav class="pagination" aria-label="Pagination">
            <c:choose>
                <c:when test="${result.page > 1}"><a href="${h:pageUrl(pageContext.request, result.page - 1)}" aria-label="Previous page">&lsaquo;</a></c:when>
                <c:otherwise><span class="disabled">&lsaquo;</span></c:otherwise>
            </c:choose>
            <c:forEach var="n" begin="${result.page - 2 < 1 ? 1 : result.page - 2}" end="${result.page + 2 > result.pages ? result.pages : result.page + 2}">
                <c:choose>
                    <c:when test="${n == result.page}"><span class="current" aria-current="page">${n}</span></c:when>
                    <c:otherwise><a href="${h:pageUrl(pageContext.request, n)}">${n}</a></c:otherwise>
                </c:choose>
            </c:forEach>
            <c:choose>
                <c:when test="${result.page < result.pages}"><a href="${h:pageUrl(pageContext.request, result.page + 1)}" aria-label="Next page">&rsaquo;</a></c:when>
                <c:otherwise><span class="disabled">&rsaquo;</span></c:otherwise>
            </c:choose>
        </nav>
    </c:if>
</div>
