<ui:layout title="${title}">
    <ui:header title="${title}" back="${empty f.id ? '/blocks' : '/blocks/' += f.id}" backLabel="${empty f.id ? 'Blocks' : 'Back to block'}"/>
    <form method="post" action="${ctx}/blocks${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:input name="name" label="Name" value="${f.name}" required="true" maxlength="100" placeholder="North Block"/>
            <ui:input name="area_sqm" label="Area (m²)" type="number" step="0.01" min="0" value="${f.area_sqm}"/>
            <ui:input name="location_note" label="Location note" value="${f.location_note}" maxlength="255" cls="full" placeholder="Next to the main gate."/>
            <ui:textarea name="description" label="Description" value="${f.description}"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/blocks">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Create block' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
