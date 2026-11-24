<ui:layout title="${title}">
    <ui:header title="${title}" back="/categories" backLabel="Categories"/>
    <form method="post" action="${ctx}/categories${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:input name="name" label="Name" value="${f.name}" required="true" maxlength="100" cls="full" placeholder="Fruit"/>
            <ui:textarea name="description" label="Description" value="${f.description}" placeholder="What kind of trees belong here?"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/categories">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Create category' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
