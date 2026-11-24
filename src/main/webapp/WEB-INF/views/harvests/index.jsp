<ui:layout title="Harvests">
    <ui:header title="Harvests" eyebrow="Production" description="What each tree produced, by date, quantity, and grade.">
        <c:if test="${canWrite}">
            <a class="btn btn-primary" href="${ctx}/harvests/create">${h:icon('plus')} New harvest</a>
        </c:if>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/harvests">
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
                <label for="tree_type_id">Tree type</label>
                <select id="tree_type_id" name="tree_type_id">
                    <option value="">All tree types</option>
                    <c:forEach var="o" items="${treeTypes}"><option value="${o.id}" ${h:sel(o.id, param.tree_type_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="grade">Grade</label>
                <select id="grade" name="grade">
                    <option value="">All grades</option>
                    <c:forEach var="o" items="${harvestGrades}"><option value="${o.value}" ${h:sel(o.value, param.grade)}>${o.label}</option></c:forEach>
                </select>
            </div>
            <div class="field"><label for="from">From</label><input id="from" type="date" name="from" value="${h:e(param.from)}"></div>
            <div class="field"><label for="to">To</label><input id="to" type="date" name="to" value="${h:e(param.to)}"></div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/harvests">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="basket" title="No harvests found." text="Record a harvest to track production and stock for sales.">
                    <c:if test="${canWrite}"><a class="btn btn-primary" href="${ctx}/harvests/create">${h:icon('plus')} Add harvest</a></c:if>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Date</th><th>Tree</th><th>Block</th><th class="num">Quantity</th><th>Grade</th><th class="num">Sold</th><th>By</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td class="nowrap">${h:date(r.harvested_on)}</td>
                                <td><a class="cell-main" href="${ctx}/trees/${r.tree_id}">${h:e(r.tree_code)}</a><div class="cell-sub">${h:e(r.type_name)}</div></td>
                                <td>${h:e(r.block_name)}</td>
                                <td class="num">${h:qty(r.quantity)} ${h:label(r.unit)}</td>
                                <td><span class="badge badge-${r.grade}">${h:label(r.grade)}</span></td>
                                <td class="num">${h:qty(r.sold)} ${h:label(r.unit)}</td>
                                <td class="small">${empty r.user_name ? '—' : h:e(r.user_name)}</td>
                                <td>
                                    <div class="row-actions">
                                        <c:if test="${isAdmin and r.sold < r.quantity}">
                                            <a class="icon-btn" data-tip="Sell" aria-label="Record a sale" href="${ctx}/sales/create?harvest_id=${r.id}">${h:icon('cash')}</a>
                                        </c:if>
                                        <c:if test="${isAdmin or (canWrite and r.user_id == user.id)}">
                                            <a class="icon-btn" data-tip="Edit" aria-label="Edit harvest" href="${ctx}/harvests/${r.id}/edit">${h:icon('edit')}</a>
                                        </c:if>
                                        <c:if test="${isAdmin}"><ui:delete action="/harvests/${r.id}/delete" what="harvest"/></c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="table-foot">
                    <span class="total-pill">${h:icon('basket')} Total for these filters: ${h:qty(totals.kg)} kg · ${h:qty(totals.pcs)} pcs</span>
                </div>
                <ui:pagination result="${page}" noun="harvests"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
