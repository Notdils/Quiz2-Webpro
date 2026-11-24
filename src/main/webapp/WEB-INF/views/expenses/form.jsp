<ui:layout title="${title}">
    <ui:header title="${title}" back="/expenses" backLabel="Expenses"/>
    <form method="post" action="${ctx}/expenses${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:select name="category" label="Category" required="true" placeholder="Choose a category">
                <c:forEach var="o" items="${expenseCategories}"><option value="${o.value}" ${h:sel(o.value, f.category)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:select name="block_id" label="Block" placeholder="General (no block)" hint="Leave empty for costs that belong to the whole farm.">
                <c:forEach var="o" items="${blocks}"><option value="${o.id}" ${h:sel(o.id, f.block_id)}>${h:e(o.name)}</option></c:forEach>
            </ui:select>
            <ui:input name="amount" label="Amount (IDR)" type="number" step="0.01" min="0.01" value="${f.amount}" required="true"/>
            <ui:input name="spent_on" label="Date" type="date" value="${f.spent_on}" required="true"/>
            <ui:input name="description" label="Description" value="${f.description}" maxlength="255" cls="full" placeholder="NPK and organic compost"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/expenses">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Save expense' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
