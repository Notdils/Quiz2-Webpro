<ui:layout title="Categories">
    <ui:header title="Categories" eyebrow="Master data" description="Group tree types into broad families such as fruit or timber.">
        <c:if test="${isAdmin}">
            <a class="btn btn-primary" href="${ctx}/categories/create">${h:icon('plus')} New category</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/categories">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Name or description"></div>
            </div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/categories">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="tag" title="No categories found."
                          text="${empty param.search ? 'Add your first category to start organizing tree types.' : 'Try a different search term.'}">
                    <c:if test="${isAdmin and empty param.search}">
                        <a class="btn btn-primary" href="${ctx}/categories/create">${h:icon('plus')} Add category</a>
                    </c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Name</th><th>Description</th><th class="num">Tree types</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td><a class="cell-main" href="${ctx}/tree-types?category_id=${r.id}">${h:e(r.name)}</a></td>
                                <td class="muted">${empty r.description ? '—' : h:e(r.description)}</td>
                                <td class="num">${r.tree_types_count}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="View tree types" aria-label="View tree types of ${h:e(r.name)}" href="${ctx}/tree-types?category_id=${r.id}">${h:icon('eye')}</a>
                                        <c:if test="${isAdmin}">
                                            <a class="icon-btn" data-tip="Edit" aria-label="Edit ${h:e(r.name)}" href="${ctx}/categories/${r.id}/edit">${h:icon('edit')}</a>
                                            <ui:delete action="/categories/${r.id}/delete" what="category"/>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="categories"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
