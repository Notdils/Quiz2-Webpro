<ui:layout title="Blocks">
    <ui:header title="Blocks" eyebrow="Master data" description="Planting areas of your farm and the trees growing in each.">
        <c:if test="${isAdmin}">
            <a class="btn btn-primary" href="${ctx}/blocks/create">${h:icon('plus')} New block</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/blocks">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Name or location"></div>
            </div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/blocks">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="map" title="No blocks found." text="Add a block to describe where your trees are planted.">
                    <c:if test="${isAdmin}"><a class="btn btn-primary" href="${ctx}/blocks/create">${h:icon('plus')} Add block</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Name</th><th>Location</th><th class="num">Area (m²)</th><th class="num">Trees</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td><a class="cell-main" href="${ctx}/blocks/${r.id}">${h:e(r.name)}</a><div class="cell-sub">${h:e(r.description)}</div></td>
                                <td class="muted">${empty r.location_note ? '—' : h:e(r.location_note)}</td>
                                <td class="num">${h:number(r.area_sqm)}</td>
                                <td class="num">${r.trees_count}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="View" aria-label="View ${h:e(r.name)}" href="${ctx}/blocks/${r.id}">${h:icon('eye')}</a>
                                        <c:if test="${isAdmin}">
                                            <a class="icon-btn" data-tip="Edit" aria-label="Edit ${h:e(r.name)}" href="${ctx}/blocks/${r.id}/edit">${h:icon('edit')}</a>
                                            <ui:delete action="/blocks/${r.id}/delete" what="block"/>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="blocks"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
