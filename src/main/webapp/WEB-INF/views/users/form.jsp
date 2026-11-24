<ui:layout title="${title}">
    <ui:header title="${title}" back="/users" backLabel="Users"/>
    <form method="post" action="${ctx}/users${empty f.id ? '' : '/' += f.id}" class="card form-card" novalidate>
        <ui:csrf/>
        <div class="card-body form-grid">
            <ui:input name="name" label="Name" value="${f.name}" required="true" maxlength="255" autocomplete="off"/>
            <ui:input name="email" label="Email address" type="email" value="${f.email}" required="true" autocomplete="off"/>
            <ui:select name="role" label="Role" required="true" cls="full">
                <c:forEach var="o" items="${userRoles}"><option value="${o.value}" ${h:sel(o.value, f.role)}>${o.label}</option></c:forEach>
            </ui:select>
            <ui:input name="password" label="Password" type="password" required="${empty f.id}" autocomplete="new-password"
                      hint="${empty f.id ? 'At least 8 characters, with a number and an uppercase letter.' : 'Leave blank to keep the current password.'}"/>
            <ui:input name="password_confirmation" label="Confirm password" type="password" required="${empty f.id}" autocomplete="new-password"/>
        </div>
        <div class="form-actions">
            <a class="btn btn-ghost" href="${ctx}/users">Cancel</a>
            <button type="submit" class="btn btn-primary">${empty f.id ? 'Create user' : 'Save changes'}</button>
        </div>
    </form>
</ui:layout>
