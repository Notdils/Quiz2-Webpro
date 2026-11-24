<ui:layout title="${title}">
    <ui:header title="${title}" back="/tree-types" backLabel="Tree Types"/>
    <form method="post" action="${ctx}/tree-types${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:select name="category_id" label="Category" required="true" placeholder="Choose a category">
                <c:forEach var="o" items="${categories}"><option value="${o.id}" ${h:sel(o.id, f.category_id)}>${h:e(o.name)}</option></c:forEach>
            </ui:select>
            <ui:input name="name" label="Name" value="${f.name}" required="true" maxlength="100" placeholder="Durian"/>
            <ui:input name="scientific_name" label="Scientific name" value="${f.scientific_name}" maxlength="150" cls="full" placeholder="Durio zibethinus"/>
            <ui:textarea name="description" label="Description" value="${f.description}"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/tree-types">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Create tree type' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
