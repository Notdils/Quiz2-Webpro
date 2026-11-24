<ui:layout title="Dashboard" charts="true">
    <section class="hero">
        <div class="hero-row">
            <div>
                <span class="eyebrow">${h:e(monthLabel)}</span>
                <h1>Good to see you, ${h:e(fn:substringBefore(user.name += ' ', ' '))}.</h1>
                <p>An overview of your farm, harvests, and care schedule.</p>
            </div>
            <div class="actions">
                <c:if test="${canWrite}">
                    <a class="btn btn-secondary" href="${ctx}/care-logs/create">${h:icon('sprout')} Log care</a>
                    <a class="btn btn-secondary" href="${ctx}/harvests/create">${h:icon('basket')} Record harvest</a>
                </c:if>
                <c:if test="${isAdmin}">
                    <a class="btn btn-secondary" href="${ctx}/reports">${h:icon('chart')} View reports</a>
                </c:if>
            </div>
        </div>
    </section>

    <div class="grid grid-4">
        <div class="card stat">
            <div class="stat-top">
                <div>
                    <p class="stat-label">Total trees</p>
                    <p class="stat-value">${h:integer(totalTrees)}</p>
                    <p class="stat-note">${h:integer(empty statuses.productive ? 0 : statuses.productive)} productive · ${h:integer(empty statuses.needs_attention ? 0 : statuses.needs_attention)} need attention</p>
                </div>
                <span class="stat-icon tone-green">${h:icon('tree')}</span>
            </div>
        </div>
        <div class="card stat">
            <div class="stat-top">
                <div>
                    <p class="stat-label">Blocks</p>
                    <p class="stat-value">${h:integer(blocksCount)}</p>
                    <p class="stat-note">Planting areas</p>
                </div>
                <span class="stat-icon tone-blue">${h:icon('map')}</span>
            </div>
        </div>
        <div class="card stat">
            <div class="stat-top">
                <div>
                    <p class="stat-label">Harvest this month</p>
                    <p class="stat-value">${h:qty(monthHarvest.kg)}<small>kg</small></p>
                    <p class="stat-note">${h:qty(monthHarvest.pcs)} pcs in ${h:e(monthLabel)}</p>
                </div>
                <span class="stat-icon tone-amber">${h:icon('basket')}</span>
            </div>
        </div>
        <div class="card stat">
            <div class="stat-top">
                <div>
                    <p class="stat-label">Upcoming care</p>
                    <p class="stat-value">${h:integer(upcomingCount)}</p>
                    <p class="stat-note">Due within 7 days or overdue</p>
                </div>
                <span class="stat-icon tone-soil">${h:icon('clock')}</span>
            </div>
        </div>
    </div>

    <c:if test="${isAdmin}">
        <div class="grid grid-3">
            <div class="card stat">
                <p class="stat-label">Sales this month</p>
                <p class="stat-value">${h:money(finance.sales)}</p>
            </div>
            <div class="card stat">
                <p class="stat-label">Expenses this month</p>
                <p class="stat-value">${h:money(finance.expenses)}</p>
            </div>
            <div class="card stat ${finance.net.signum() < 0 ? 'negative' : 'positive'}">
                <p class="stat-label">Net profit this month</p>
                <p class="stat-value">${h:money(finance.net)}</p>
            </div>
        </div>
    </c:if>

    <div class="grid ${isAdmin ? 'grid-2' : ''}">
        <section class="card">
            <div class="card-head">
                <div><h2>Harvest per month</h2><p>Last 12 months, by unit</p></div>
            </div>
            <div class="chart-box">
                <canvas data-kind="harvest" data-chart="${h:e(h:json(harvestChart))}" aria-label="Harvest per month chart" role="img"></canvas>
            </div>
        </section>
        <c:if test="${isAdmin}">
            <section class="card">
                <div class="card-head">
                    <div><h2>Sales vs expenses</h2><p>Last 12 months, in IDR</p></div>
                </div>
                <div class="chart-box">
                    <canvas data-kind="finance" data-chart="${h:e(h:json(financeChart))}" aria-label="Sales versus expenses chart" role="img"></canvas>
                </div>
            </section>
        </c:if>
    </div>

    <div class="split">
        <section class="card">
            <div class="card-head">
                <div><h2>Recent harvests</h2><p>The five latest records</p></div>
                <a class="btn btn-sm btn-ghost" href="${ctx}/harvests">View all</a>
            </div>
            <c:choose>
                <c:when test="${empty recentHarvests}">
                    <ui:empty icon="basket" title="No harvests yet." text="Recorded harvests will appear here."/>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table class="table">
                            <thead><tr><th>Date</th><th>Tree</th><th>Block</th><th class="num">Quantity</th><th>Grade</th></tr></thead>
                            <tbody>
                            <c:forEach var="r" items="${recentHarvests}">
                                <tr>
                                    <td class="nowrap">${h:date(r.harvested_on)}</td>
                                    <td><a class="cell-main" href="${ctx}/trees/${r.tree_id}">${h:e(r.tree_code)}</a><div class="cell-sub">${h:e(r.type_name)}</div></td>
                                    <td>${h:e(r.block_name)}</td>
                                    <td class="num">${h:qty(r.quantity)} ${h:label(r.unit)}</td>
                                    <td><span class="badge badge-${r.grade}">${h:label(r.grade)}</span></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="card">
            <div class="card-head">
                <div><h2>Upcoming care</h2><p>Due within 7 days or overdue</p></div>
                <a class="btn btn-sm btn-ghost" href="${ctx}/care-logs">View all</a>
            </div>
            <c:choose>
                <c:when test="${empty upcomingCare}">
                    <ui:empty icon="check" title="Nothing due." text="No care activities are due in the next 7 days."/>
                </c:when>
                <c:otherwise>
                    <ul class="list">
                        <c:forEach var="r" items="${upcomingCare}">
                            <li>
                                <div>
                                    <a class="cell-main" href="${ctx}/trees/${r.tree_id}">${h:e(r.tree_code)}</a>
                                    <div class="cell-sub">${h:label(r.activity_type)} · ${h:date(r.next_due_on)}</div>
                                </div>
                                <span class="badge badge-${h:dueTone(r.next_due_on)}">${h:due(r.next_due_on)}</span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</ui:layout>
