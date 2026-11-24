<ui:layout title="${title}">
    <ui:header title="${title}" back="/sales" backLabel="Sales"
               description="Only harvests with unsold stock are listed. The total is calculated automatically."/>
    <form method="post" action="${ctx}/sales${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:select name="harvest_id" label="Harvest" required="true" placeholder="Choose a harvest" cls="full">
                <c:forEach var="o" items="${harvests}">
                    <option value="${o.id}" ${h:sel(o.id, f.harvest_id)}>${h:e(o.tree_code)} · ${h:e(o.type_name)} · ${h:date(o.harvested_on)} — ${h:qty(o.remaining)} ${h:label(o.unit)} left</option>
                </c:forEach>
            </ui:select>
            <ui:input name="buyer_name" label="Buyer name" value="${f.buyer_name}" required="true" maxlength="150" placeholder="Fresh Harvest Market"/>
            <ui:input name="sold_on" label="Sale date" type="date" value="${f.sold_on}" required="true"/>
            <ui:input name="quantity" label="Quantity" type="number" step="0.01" min="0.01" value="${f.quantity}" required="true"/>
            <ui:input name="unit_price" label="Unit price (IDR)" type="number" step="0.01" min="0.01" value="${f.unit_price}" required="true"/>
            <ui:textarea name="notes" label="Notes" value="${f.notes}"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/sales">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Save sale' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
