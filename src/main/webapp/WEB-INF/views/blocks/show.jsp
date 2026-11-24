<ui:layout title="${block.name}">
    <ui:header title="${block.name}" eyebrow="Block" back="/blocks" backLabel="Blocks"
               description="${empty block.location_note ? 'No location note.' : block.location_note}">
        <c:if test="${canWrite}">
            <a class="btn btn-secondary" href="${ctx}/trees/create?block_id=${block.id}">${h:icon('plus')} Add tree here</a>
        </c:if>
        <c:if test="${isAdmin}">
            <a class="btn btn-secondary" href="${ctx}/blocks/${block.id}/edit">${h:icon('edit')} Edit</a>
            <ui:delete action="/blocks/${block.id}/delete" what="block" button="true"/>
        </c:if>
    </ui:header>

    <div class="grid grid-4">
        <div class="card stat"><p class="stat-label">Trees</p><p class="stat-value">${h:integer(stats.trees)}</p></div>
        <div class="card stat"><p class="stat-label">Area</p><p class="stat-value">${h:number(block.area_sqm)}<small>m²</small></p></div>
        <div class="card stat"><p class="stat-label">Total harvest (kg)</p><p class="stat-value">${h:qty(stats.kg)}<small>kg</small></p></div>
        <div class="card stat"><p class="stat-label">Total harvest (pcs)</p><p class="stat-value">${h:qty(stats.pcs)}<small>pcs</small></p></div>
    </div>

    <c:if test="${not empty block.description}">
        <div class="card card-body"><p>${h:e(block.description)}</p></div>
    </c:if>

    <section class="card">
        <div class="card-head"><div><h2>Trees in this block</h2><p>Every living record planted in ${h:e(block.name)}</p></div></div>
        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="tree" title="No trees in this block yet.">
                    <c:if test="${canWrite}"><a class="btn btn-primary" href="${ctx}/trees/create?block_id=${block.id}">${h:icon('plus')} Add tree</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Tree</th><th>Status</th><th>Planted</th></tr></thead>
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
                                <td><span class="badge badge-${r.status}">${h:label(r.status)}</span></td>
                                <td class="nowrap">${h:date(r.planted_on)}</td>
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
