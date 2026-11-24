<ui:layout title="Care Logs">
    <ui:header title="Care Logs" eyebrow="Field work" description="Watering, fertilizing, pruning, and every other activity done on your trees.">
        <c:if test="${canWrite}">
            <a class="btn btn-primary" href="${ctx}/care-logs/create">${h:icon('plus')} New care log</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/care-logs">
            <div class="field">
                <label for="tree_id">Tree</label>
                <select id="tree_id" name="tree_id">
                    <option value="">All trees</option>
                    <c:forEach var="o" items="${trees}"><option value="${o.id}" ${h:sel(o.id, param.tree_id)}>${h:e(o.code)} · ${h:e(o.type_name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="block_id">Block</label>
                <select id="block_id" name="block_id">
                    <option value="">All blocks</option>
                    <c:forEach var="o" items="${blocks}"><option value="${o.id}" ${h:sel(o.id, param.block_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="activity_type">Activity</label>
                <select id="activity_type" name="activity_type">
                    <option value="">All activities</option>
                    <c:forEach var="o" items="${activityTypes}"><option value="${o.value}" ${h:sel(o.value, param.activity_type)}>${o.label}</option></c:forEach>
                </select>
            </div>
            <div class="field"><label for="from">From</label><input id="from" type="date" name="from" value="${h:e(param.from)}"></div>
            <div class="field"><label for="to">To</label><input id="to" type="date" name="to" value="${h:e(param.to)}"></div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/care-logs">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="sprout" title="No care logs found." text="Record watering, pruning, or inspections to build each tree's history.">
                    <c:if test="${canWrite}"><a class="btn btn-primary" href="${ctx}/care-logs/create">${h:icon('plus')} Add care log</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Performed</th><th>Tree</th><th>Activity</th><th>Next due</th><th>Notes</th><th>By</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td class="nowrap">${h:date(r.performed_on)}</td>
                                <td><a class="cell-main" href="${ctx}/trees/${r.tree_id}">${h:e(r.tree_code)}</a><div class="cell-sub">${h:e(r.type_name)} · ${h:e(r.block_name)}</div></td>
                                <td>${h:label(r.activity_type)}</td>
                                <td class="nowrap">
                                    <c:choose>
                                        <c:when test="${empty r.next_due_on}"><span class="muted">—</span></c:when>
                                        <c:otherwise>${h:date(r.next_due_on)}<div><span class="badge badge-${h:dueTone(r.next_due_on)}">${h:due(r.next_due_on)}</span></div></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="muted small">${empty r.notes ? '—' : h:e(r.notes)}</td>
                                <td class="small">${empty r.user_name ? '—' : h:e(r.user_name)}</td>
                                <td>
                                    <div class="row-actions">
                                        <c:if test="${isAdmin or (canWrite and r.user_id == user.id)}">
                                            <a class="icon-btn" data-tip="Edit" aria-label="Edit care log" href="${ctx}/care-logs/${r.id}/edit">${h:icon('edit')}</a>
                                        </c:if>
                                        <c:if test="${isAdmin}"><ui:delete action="/care-logs/${r.id}/delete" what="care log"/></c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="care logs"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
