<ui:layout title="${tree.code}">
    <ui:header title="${tree.code}" eyebrow="${tree.type_name}" back="/trees" backLabel="Trees">
        <c:if test="${canWrite and tree.status != 'dead'}">
            <a class="btn btn-secondary" href="${ctx}/care-logs/create?tree_id=${tree.id}&amp;back=/trees/${tree.id}">${h:icon('sprout')} Add care log</a>
            <a class="btn btn-secondary" href="${ctx}/harvests/create?tree_id=${tree.id}&amp;back=/trees/${tree.id}">${h:icon('basket')} Add harvest</a>
        </c:if>
        <c:if test="${canWrite}"><a class="btn btn-primary" href="${ctx}/trees/${tree.id}/edit">${h:icon('edit')} Edit</a></c:if>
        <c:if test="${isAdmin}"><ui:delete action="/trees/${tree.id}/delete" what="tree" button="true"/></c:if>
    </ui:header>

    <c:if test="${tree.status == 'dead'}">
        <div class="alert alert-danger">${h:icon('alert')} This tree is dead, so it cannot receive new care logs or harvests.</div>
    </c:if>

    <div class="split">
        <section class="card card-body">
            <dl class="details">
                <div><dt>Status</dt><dd><span class="badge badge-${tree.status}">${h:label(tree.status)}</span></dd></div>
                <div><dt>Tree type</dt><dd>${h:e(tree.type_name)} <span class="muted"><em>${h:e(tree.scientific_name)}</em></span></dd></div>
                <div><dt>Category</dt><dd>${h:e(tree.category_name)}</dd></div>
                <div><dt>Block</dt><dd><a href="${ctx}/blocks/${tree.block_id}">${h:e(tree.block_name)}</a></dd></div>
                <div><dt>Planted on</dt><dd>${h:date(tree.planted_on)}</dd></div>
                <div><dt>Height</dt><dd>${empty tree.height_cm ? '—' : h:qty(tree.height_cm) += ' cm'}</dd></div>
                <div><dt>Total harvest</dt><dd>${h:qty(totals.kg)} kg · ${h:qty(totals.pcs)} pcs</dd></div>
                <div><dt>Added by</dt><dd>${empty tree.creator_name ? '—' : h:e(tree.creator_name)}</dd></div>
                <div class="full"><dt>Notes</dt><dd>${empty tree.notes ? '—' : h:e(tree.notes)}</dd></div>
            </dl>
        </section>
        <div class="photo-frame">
            <c:choose>
                <c:when test="${tree.has_photo}"><img src="${ctx}/trees/${tree.id}/photo" alt="Photo of tree ${h:e(tree.code)}"></c:when>
                <c:otherwise>${h:icon('image')}</c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="grid grid-2" style="align-items:start">
        <section class="card">
            <div class="card-head"><div><h2>Care timeline</h2><p>Most recent activities first</p></div></div>
            <c:choose>
                <c:when test="${empty careLogs}"><ui:empty icon="sprout" title="No care logs yet." text="Watering, pruning, and other activities will appear here."/></c:when>
                <c:otherwise>
                    <ul class="timeline">
                        <c:forEach var="r" items="${careLogs}">
                            <li>
                                <div class="timeline-head">
                                    <strong>${h:label(r.activity_type)}</strong>
                                    <span class="muted small">${h:date(r.performed_on)}</span>
                                    <c:if test="${not empty r.next_due_on}"><span class="badge badge-${h:dueTone(r.next_due_on)}">Next: ${h:date(r.next_due_on)}</span></c:if>
                                </div>
                                <c:if test="${not empty r.notes}"><p class="small" style="margin-top:4px">${h:e(r.notes)}</p></c:if>
                                <p class="cell-sub">by ${empty r.user_name ? 'a former user' : h:e(r.user_name)}</p>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="card">
            <div class="card-head"><div><h2>Harvests</h2><p>${totals.harvests} recorded</p></div></div>
            <c:choose>
                <c:when test="${empty harvests}"><ui:empty icon="basket" title="No harvests yet." text="Harvests of this tree will be listed here."/></c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table class="table">
                            <thead><tr><th>Date</th><th class="num">Quantity</th><th>Grade</th><th>Sold</th></tr></thead>
                            <tbody>
                            <c:forEach var="r" items="${harvests}">
                                <tr>
                                    <td class="nowrap">${h:date(r.harvested_on)}</td>
                                    <td class="num">${h:qty(r.quantity)} ${h:label(r.unit)}</td>
                                    <td><span class="badge badge-${r.grade}">${h:label(r.grade)}</span></td>
                                    <td>
                                        <div class="progress" title="${h:qty(r.sold)} of ${h:qty(r.quantity)} sold"><span style="width:${r.sold * 100 / r.quantity}%"></span></div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</ui:layout>
