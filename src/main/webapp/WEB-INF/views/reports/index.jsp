<ui:layout title="Reports" charts="true">
    <c:set var="query" value="from=${from}&amp;to=${to}&amp;group_by=${groupBy}"/>
    <ui:header title="Reports" eyebrow="Insights" description="Evaluate productivity and profit for any period. Every report can be exported as CSV.">
        <a class="btn btn-primary" href="${ctx}/reports/export?report=${report}&amp;${query}">${h:icon('download')} Export CSV</a>
    </ui:header>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/reports" style="border-radius:var(--radius)">
            <input type="hidden" name="report" value="${report}">
            <div class="field"><label for="from">From</label><input id="from" type="date" name="from" value="${from}"></div>
            <div class="field"><label for="to">To</label><input id="to" type="date" name="to" value="${to}"></div>
            <c:if test="${report == 'harvest'}">
                <div class="field">
                    <label for="group_by">Group by</label>
                    <select id="group_by" name="group_by">
                        <option value="block" ${h:sel('block', groupBy)}>Block</option>
                        <option value="tree_type" ${h:sel('tree_type', groupBy)}>Tree type</option>
                        <option value="month" ${h:sel('month', groupBy)}>Month</option>
                    </select>
                </div>
            </c:if>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Apply</button>
                <a class="btn btn-ghost" href="${ctx}/reports?report=${report}">Reset</a>
            </div>
        </form>
    </section>

    <nav class="tabs" aria-label="Report type">
        <a href="${ctx}/reports?report=harvest&amp;${query}" class="${report == 'harvest' ? 'active' : ''}">Harvest summary</a>
        <a href="${ctx}/reports?report=financial&amp;${query}" class="${report == 'financial' ? 'active' : ''}">Financial summary</a>
        <a href="${ctx}/reports?report=care&amp;${query}" class="${report == 'care' ? 'active' : ''}">Care activity</a>
    </nav>

    <p class="muted">Period: <strong>${h:date(from)}</strong> to <strong>${h:date(to)}</strong></p>

    <c:choose>
        <c:when test="${report == 'financial'}">
            <div class="grid grid-3">
                <div class="card stat"><p class="stat-label">Sales</p><p class="stat-value">${h:money(totals.sales)}</p></div>
                <div class="card stat"><p class="stat-label">Expenses</p><p class="stat-value">${h:money(totals.expenses)}</p></div>
                <div class="card stat ${totals.net.signum() < 0 ? 'negative' : 'positive'}"><p class="stat-label">Net profit</p><p class="stat-value">${h:money(totals.net)}</p></div>
            </div>
            <section class="card">
                <div class="card-head"><div><h2>Sales vs expenses per month</h2></div></div>
                <div class="chart-box"><canvas data-kind="finance" data-chart="${h:e(h:json(financeChart))}" role="img" aria-label="Sales versus expenses per month"></canvas></div>
            </section>
            <div class="grid grid-2" style="align-items:start">
                <section class="card">
                    <div class="card-head"><div><h2>Per month</h2></div></div>
                    <div class="table-wrap">
                        <table class="table">
                            <thead><tr><th>Month</th><th class="num">Sales</th><th class="num">Expenses</th><th class="num">Net</th></tr></thead>
                            <tbody>
                            <c:forEach var="r" items="${financial}">
                                <tr><td>${r.label}</td><td class="num">${h:money(r.sales)}</td><td class="num">${h:money(r.expenses)}</td>
                                    <td class="num cell-main" style="color:${r.net.signum() < 0 ? 'var(--danger)' : 'var(--forest)'}">${h:money(r.net)}</td></tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </section>
                <section class="card">
                    <div class="card-head"><div><h2>Per block</h2><p>Sales follow the block of the harvested tree.</p></div></div>
                    <c:choose>
                        <c:when test="${empty financialBlocks}"><ui:empty icon="map" title="No sales or expenses in this period."/></c:when>
                        <c:otherwise>
                            <div class="table-wrap">
                                <table class="table">
                                    <thead><tr><th>Block</th><th class="num">Sales</th><th class="num">Expenses</th><th class="num">Net</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="r" items="${financialBlocks}">
                                        <tr><td>${h:e(r.label)}</td><td class="num">${h:money(r.sales)}</td><td class="num">${h:money(r.expenses)}</td>
                                            <td class="num cell-main" style="color:${r.net.signum() < 0 ? 'var(--danger)' : 'var(--forest)'}">${h:money(r.net)}</td></tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </c:when>

        <c:when test="${report == 'care'}">
            <c:choose>
                <c:when test="${empty care}">
                    <section class="card"><ui:empty icon="sprout" title="No care activity in this period." text="Try a longer period."/></section>
                </c:when>
                <c:otherwise>
                    <div class="grid grid-2" style="align-items:start">
                        <section class="card">
                            <div class="card-head"><div><h2>Activities by type</h2><p>${h:integer(totals.care)} care logs in total</p></div></div>
                            <div class="chart-box"><canvas data-kind="doughnut" data-chart="${h:e(h:json(careChart))}" role="img" aria-label="Care activities by type"></canvas></div>
                        </section>
                        <section class="card">
                            <div class="card-head"><div><h2>Summary</h2></div></div>
                            <div class="table-wrap">
                                <table class="table">
                                    <thead><tr><th>Activity</th><th class="num">Care logs</th><th>Last performed</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="r" items="${care}">
                                        <tr><td class="cell-main">${h:e(r.label)}</td><td class="num">${r.total}</td><td>${h:date(r.last_performed)}</td></tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:when>

        <c:otherwise>
            <div class="grid grid-3">
                <div class="card stat"><p class="stat-label">Harvests</p><p class="stat-value">${h:integer(totals.harvests)}</p></div>
                <div class="card stat"><p class="stat-label">Weight</p><p class="stat-value">${h:qty(totals.kg)}<small>kg</small></p></div>
                <div class="card stat"><p class="stat-label">Pieces</p><p class="stat-value">${h:qty(totals.pcs)}<small>pcs</small></p></div>
            </div>
            <c:choose>
                <c:when test="${empty harvest}">
                    <section class="card"><ui:empty icon="basket" title="No harvests in this period." text="Try a longer period."/></section>
                </c:when>
                <c:otherwise>
                    <section class="card">
                        <div class="card-head"><div><h2>Harvest by ${groupBy == 'tree_type' ? 'tree type' : groupBy}</h2></div></div>
                        <div class="chart-box"><canvas data-kind="harvest" data-chart="${h:e(h:json(harvestChart))}" role="img" aria-label="Harvest summary chart"></canvas></div>
                        <div class="table-wrap">
                            <table class="table">
                                <thead><tr><th>${groupBy == 'tree_type' ? 'Tree type' : (groupBy == 'month' ? 'Month' : 'Block')}</th><th class="num">Harvests</th><th class="num">Quantity (kg)</th><th class="num">Quantity (pcs)</th></tr></thead>
                                <tbody>
                                <c:forEach var="r" items="${harvest}">
                                    <tr><td class="cell-main">${h:e(r.label)}</td><td class="num">${r.harvests}</td><td class="num">${h:qty(r.kg)}</td><td class="num">${h:qty(r.pcs)}</td></tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </section>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</ui:layout>
