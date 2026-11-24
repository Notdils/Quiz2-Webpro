<ui:layout title="Users">
    <ui:header title="Users" eyebrow="Administration" description="Who can sign in and what they are allowed to do.">
        <a class="btn btn-primary" href="${ctx}/users/create">${h:icon('plus')} New user</a>
    </ui:header>

    <div class="grid grid-3">
        <div class="card card-body"><span class="badge badge-admin">Admin</span><p class="small muted" style="margin-top:8px">Full access, including finance, reports, and users.</p></div>
        <div class="card card-body"><span class="badge badge-staff">Staff</span><p class="small muted" style="margin-top:8px">Adds and edits trees, care logs, and harvests. Cannot delete.</p></div>
        <div class="card card-body"><span class="badge badge-viewer">Viewer</span><p class="small muted" style="margin-top:8px">Read-only access to farm data.</p></div>
    </div>

    <section class="card">
        <form class="filters" method="get" action="${ctx}/users">
            <div class="field grow">
                <label for="search">Search</label>
                <div class="search">${h:icon('search')}<input id="search" name="search" value="${h:e(param.search)}" placeholder="Name or email"></div>
            </div>
            <div class="field">
                <label for="role">Role</label>
                <select id="role" name="role">
                    <option value="">All roles</option>
                    <c:forEach var="o" items="${userRoles}"><option value="${o.value}" ${h:sel(o.value, param.role)}>${o.label}</option></c:forEach>
                </select>
            </div>
            <div class="buttons">
                <button type="submit" class="btn btn-secondary">Filter</button>
                <a class="btn btn-ghost" href="${ctx}/users">Reset</a>
            </div>
        </form>

        <c:choose>
            <c:when test="${page.total == 0}">
                <ui:empty icon="users" title="No users found." text="Try a different search term."/>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>User</th><th>Role</th><th>Joined</th><th><span class="sr-only">Actions</span></th></tr></thead>
                        <tbody>
                        <c:forEach var="r" items="${page.items}">
                            <tr>
                                <td>
                                    <div class="with-thumb">
                                        <span class="avatar">${h:initials(r.name)}</span>
                                        <div><span class="cell-main">${h:e(r.name)}</span><c:if test="${r.id == user.id}"> <span class="muted small">(you)</span></c:if><div class="cell-sub">${h:e(r.email)}</div></div>
                                    </div>
                                </td>
                                <td><span class="badge badge-${r.role}">${h:label(r.role)}</span></td>
                                <td class="nowrap">${h:date(r.created_at)}</td>
                                <td>
                                    <div class="row-actions">
                                        <a class="icon-btn" data-tip="Edit" aria-label="Edit ${h:e(r.name)}" href="${ctx}/users/${r.id}/edit">${h:icon('edit')}</a>
                                        <c:if test="${r.id != user.id and not (r.role == 'admin' and adminCount == 1)}">
                                            <ui:delete action="/users/${r.id}/delete" what="user"/>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <ui:pagination result="${page}" noun="users"/>
            </c:otherwise>
        </c:choose>
    </section>
</ui:layout>
