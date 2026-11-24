<ui:layout title="Expenses">
    <ui:header title="Expenses" eyebrow="Finance" description="Money spent on the farm, per block or as general costs.">
        <a class="btn btn-primary" href="${ctx}/expenses/create">${h:icon('plus')} New expense</a>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/expenses">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Description"></div>
            </div>
            <div class="field">
                <label for="block_id">Block</label>
                <select id="block_id" name="block_id">
                    <option value="">All blocks</option>
                    <option value="general" ${h:sel('general', param.block_id)}>General (no block)</option>
                    <c:forEach var="o" items="${blocks}"><option value="${o.id}" ${h:sel(o.id, param.block_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="category">Category</label>
                <select id="category" name="category">
                    <option value="">All categories</option>
                    <c:forEach var="o" items="${expenseCategories}"><option value="${o.value}" ${h:sel(o.value, param.category)}>${o.label}</option></c:forEach>
                </select>
            </div>
            <div class="field"><label for="from">From</label><input id="from" type="date" name="from" value="${h:e(param.from)}"></div>
            <div class="field"><label for="to">To</label><input id="to" type="date" name="to" value="${h:e(param.to)}"></div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/expenses">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="wallet" title="No expenses found." text="Record fertilizer, labor, and other costs to see your real profit.">
                    <a class="btn btn-primary" href="${ctx}/expenses/create">${h:icon('plus')} Add expense</a>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Date</th><th>Category</th><th>Block</th><th>Description</th><th class="num">Amount</th><th>By</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td class="nowrap">${h:date(r.spent_on)}</td>
                                <td><span class="badge badge-neutral plain">${h:label(r.category)}</span></td>
                                <td>${empty r.block_name ? '<span class="muted">General</span>' : h:e(r.block_name)}</td>
                                <td class="muted">${empty r.description ? '—' : h:e(r.description)}</td>
                                <td class="num cell-main">${h:money(r.amount)}</td>
                                <td class="small">${empty r.user_name ? '—' : h:e(r.user_name)}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="Edit" aria-label="Edit expense" href="${ctx}/expenses/${r.id}/edit">${h:icon('edit')}</a>
                                        <ui:delete action="/expenses/${r.id}/delete" what="expense"/>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="table-foot"><span class="total-pill">${h:icon('wallet')} Total for these filters: ${h:money(total)}</span></div>
                <ui:pagination result="${page}" noun="expenses"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
