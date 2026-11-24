<ui:layout title="Tree Types">
    <ui:header title="Tree Types" eyebrow="Master data" description="The species you grow, each belonging to a category.">
        <c:if test="${isAdmin}">
            <a class="btn btn-primary" href="${ctx}/tree-types/create${empty param.category_id ? '' : '?category_id=' += h:e(param.category_id)}">${h:icon('plus')} New tree type</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/tree-types">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Name or scientific name"></div>
            </div>
            <div class="field">
                <label for="category_id">Category</label>
                <select id="category_id" name="category_id">
                    <option value="">All categories</option>
                    <c:forEach var="o" items="${categories}"><option value="${o.id}" ${h:sel(o.id, param.category_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/tree-types">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="layers" title="No tree types found." text="Add a tree type such as Durian or Teak, or change the filters.">
                    <c:if test="${isAdmin}"><a class="btn btn-primary" href="${ctx}/tree-types/create">${h:icon('plus')} Add tree type</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Name</th><th>Category</th><th>Description</th><th class="num">Trees</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td>
                                    <a class="cell-main" href="${ctx}/trees?tree_type_id=${r.id}">${h:e(r.name)}</a>
                                    <div class="cell-sub"><em>${h:e(r.scientific_name)}</em></div>
                                </td>
                                <td><span class="badge badge-neutral plain">${h:e(r.category_name)}</span></td>
                                <td class="muted">${empty r.description ? '—' : h:e(r.description)}</td>
                                <td class="num">${r.trees_count}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="View trees" aria-label="View ${h:e(r.name)} trees" href="${ctx}/trees?tree_type_id=${r.id}">${h:icon('eye')}</a>
                                        <c:if test="${isAdmin}">
                                            <a class="icon-btn" data-tip="Edit" aria-label="Edit ${h:e(r.name)}" href="${ctx}/tree-types/${r.id}/edit">${h:icon('edit')}</a>
                                            <ui:delete action="/tree-types/${r.id}/delete" what="tree type"/>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="tree types"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
