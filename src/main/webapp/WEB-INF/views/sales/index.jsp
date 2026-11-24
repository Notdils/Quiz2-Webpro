<ui:layout title="Sales">
    <ui:header title="Sales" eyebrow="Finance" description="Harvest sold to buyers. Totals are calculated from quantity and unit price.">
        <a class="btn btn-primary" href="${ctx}/sales/create">${h:icon('plus')} New sale</a>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/sales">
            <div class="field grow">
                <label for="search">Buyer</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Buyer name"></div>
            </div>
            <div class="field">
                <label for="tree_type_id">Tree type</label>
                <select id="tree_type_id" name="tree_type_id">
                    <option value="">All tree types</option>
                    <c:forEach var="o" items="${treeTypes}"><option value="${o.id}" ${h:sel(o.id, param.tree_type_id)}>${h:e(o.name)}</option></c:forEach>
                </select>
            </div>
            <div class="field"><label for="from">From</label><input id="from" type="date" name="from" value="${h:e(param.from)}"></div>
            <div class="field"><label for="to">To</label><input id="to" type="date" name="to" value="${h:e(param.to)}"></div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/sales">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="cash" title="No sales found." text="Record a sale from any harvest that still has stock.">
                    <a class="btn btn-primary" href="${ctx}/sales/create">${h:icon('plus')} Add sale</a>
                </ui:empty>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Date</th><th>Buyer</th><th>Harvest</th><th class="num">Quantity</th><th class="num">Unit price</th><th class="num">Total</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td class="nowrap">${h:date(r.sold_on)}</td>
                                <td class="cell-main">${h:e(r.buyer_name)}</td>
                                <td><a href="${ctx}/trees/${r.tree_id}">${h:e(r.tree_code)}</a><div class="cell-sub">${h:e(r.type_name)} · ${h:date(r.harvested_on)}</div></td>
                                <td class="num">${h:qty(r.quantity)} ${h:label(r.unit)}</td>
                                <td class="num">${h:money(r.unit_price)}</td>
                                <td class="num cell-main">${h:money(r.total_amount)}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="Edit" aria-label="Edit sale" href="${ctx}/sales/${r.id}/edit">${h:icon('edit')}</a>
                                        <ui:delete action="/sales/${r.id}/delete" what="sale"/>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="table-foot"><span class="total-pill">${h:icon('trend')} Revenue for these filters: ${h:money(total)}</span></div>
                <ui:pagination result="${page}" noun="sales"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
