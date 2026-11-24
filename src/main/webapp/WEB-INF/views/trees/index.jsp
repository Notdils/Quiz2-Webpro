<ui:layout title="Trees">
    <ui:header title="Trees" eyebrow="Orchard" description="Every tree on the farm, with its type, block, and current condition.">
        <c:if test="${canWrite}">
            <a class="btn btn-primary" href="${ctx}/trees/create">${h:icon('plus')} New tree</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/trees">
            <input type="hidden" name="sort" value="${h:e(param.sort)}">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Code or notes"></div>
            </div>
            <div class="field">
                <label for="block_id">Block</label>
                <select id="block_id" name="block_id">
                    <option value="">All blocks</option>
                    <c:forEach var="o" items="${blocks}"><option value="${o.id}" ${h:sel(o.id, param.block_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="category_id">Category</label>
                <select id="category_id" name="category_id">
                    <option value="">All categories</option>
                    <c:forEach var="o" items="${categories}"><option value="${o.id}" ${h:sel(o.id, param.category_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="tree_type_id">Tree type</label>
                <select id="tree_type_id" name="tree_type_id">
                    <option value="">All tree types</option>
                    <c:forEach var="o" items="${treeTypes}"><option value="${o.id}" ${h:sel(o.id, param.tree_type_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="status">Status</label>
                <select id="status" name="status">
                    <option value="">All statuses</option>
                    <c:forEach var="o" items="${treeStatuses}"><option value="${o.value}" ${h:sel(o.value, param.status)}>${o.label}</option></c:forEach>
                </select>
            </div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/trees">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="tree" title="No trees found." text="No trees yet, or none match these filters. Add your first tree to get started.">
                    <c:if test="${canWrite}"><a class="btn btn-primary" href="${ctx}/trees/create">${h:icon('plus')} Add tree</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                        <tr>
                            <th><a href="${h:sortUrl(pageContext.request, param.sort == 'code' ? '-code' : 'code')}">Tree ${h:icon('sort')}</a></th>
                            <th>Category</th>
                            <th>Block</th>
                            <th>Status</th>
                            <th><a href="${h:sortUrl(pageContext.request, param.sort == 'planted_on' ? '-planted_on' : 'planted_on')}">Planted ${h:icon('sort')}</a></th>
                            <th class="num">Height</th>
                            <th><span class="sr-only">Actions</span></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td>
                                    <div class="with-thumb">
                                        <c:choose>
                                            <c:when test="${r.has_photo}"><img class="thumb" src="${ctx}/trees/${r.id}/photo" alt="Photo of tree ${h:e(r.code)}" loading="lazy"></c:when>
                                            <c:otherwise><span class="thumb thumb-empty">${h:icon('tree')}</span></c:otherwise>
                                        </c:choose>
                                        <div><a class="cell-main" href="${ctx}/trees/${r.id}">${h:e(r.code)}</a><div class="cell-sub">${h:e(r.type_name)}</div></div>
                                    </div>
                                </td>
                                <td>${h:e(r.category_name)}</td>
                                <td><a href="${ctx}/blocks/${r.block_id}">${h:e(r.block_name)}</a></td>
                                <td><span class="badge badge-${r.status}">${h:label(r.status)}</span></td>
                                <td class="nowrap">${h:date(r.planted_on)}</td>
                                <td class="num">${empty r.height_cm ? '—' : h:qty(r.height_cm) += ' cm'}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="View" aria-label="View ${h:e(r.code)}" href="${ctx}/trees/${r.id}">${h:icon('eye')}</a>
                                        <c:if test="${canWrite}"><a class="icon-btn" data-tip="Edit" aria-label="Edit ${h:e(r.code)}" href="${ctx}/trees/${r.id}/edit">${h:icon('edit')}</a></c:if>
                                        <c:if test="${isAdmin}"><ui:delete action="/trees/${r.id}/delete" what="tree"/></c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="trees"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
