<ui:layout title="${title}">
    <ui:header title="${title}" back="${empty back ? '/care-logs' : back}" backLabel="${empty back ? 'Care Logs' : 'Back to tree'}"
               description="Dead trees are not listed because they cannot receive new care logs."/>
    <form method="post" action="${ctx}/care-logs${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <c:if test="${not empty back}"><input type="hidden" name="back" value="${h:e(back)}"></c:if>
        <div class="card-body form-grid">
            <ui:select name="tree_id" label="Tree" required="true" placeholder="Choose a tree">
                <c:forEach var="o" items="${trees}"><option value="${o.id}" ${h:sel(o.id, f.tree_id)}>${h:e(o.code)} · ${h:e(o.type_name)}</option></c:forEach>
            </ui:select>
            <ui:select name="activity_type" label="Activity" required="true" placeholder="Choose an activity">
                <c:forEach var="o" items="${activityTypes}"><option value="${o.value}" ${h:sel(o.value, f.activity_type)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:input name="performed_on" label="Performed on" type="date" value="${f.performed_on}" required="true"/>
            <ui:input name="next_due_on" label="Next due on" type="date" value="${f.next_due_on}" hint="Optional. Shows up in Upcoming Care on the dashboard."/>
            <ui:textarea name="notes" label="Notes" value="${f.notes}" placeholder="What was done and what you noticed."/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}${empty back ? '/care-logs' : back}">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Save care log' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
