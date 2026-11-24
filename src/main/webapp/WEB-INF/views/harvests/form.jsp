<ui:layout title="${title}">
    <ui:header title="${title}" back="${empty back ? '/harvests' : back}" backLabel="${empty back ? 'Harvests' : 'Back to tree'}"
               description="Dead trees are not listed because they cannot receive new harvests."/>
    <form method="post" action="${ctx}/harvests${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <c:if test="${not empty back}"><input type="hidden" name="back" value="${h:e(back)}"></c:if>
        <div class="card-body form-grid">
            <ui:select name="tree_id" label="Tree" required="true" placeholder="Choose a tree" cls="full">
                <c:forEach var="o" items="${trees}"><option value="${o.id}" ${h:sel(o.id, f.tree_id)}>${h:e(o.code)} · ${h:e(o.type_name)}</option></c:forEach>
            </ui:select>
            <ui:input name="harvested_on" label="Harvested on" type="date" value="${f.harvested_on}" required="true"/>
            <ui:select name="grade" label="Grade" required="true">
                <c:forEach var="o" items="${harvestGrades}"><option value="${o.value}" ${h:sel(o.value, f.grade)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:input name="quantity" label="Quantity" type="number" step="0.01" min="0.01" value="${f.quantity}" required="true"/>
            <ui:select name="unit" label="Unit" required="true">
                <c:forEach var="o" items="${harvestUnits}"><option value="${o.value}" ${h:sel(o.value, f.unit)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:textarea name="notes" label="Notes" value="${f.notes}"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}${empty back ? '/harvests' : back}">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Save harvest' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
